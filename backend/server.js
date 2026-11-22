const crypto = require('crypto');
const express = require('express');
const cors = require('cors');
const { initializeApp, cert, applicationDefault, getApps } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

const PORT = Number(process.env.PORT || 8080);
const FAMGATEWAY_BASE_URL = 'https://famgateway.in';
const DEFAULT_DB_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  'ai-studio-android-kelywing-6aebd6f3-784d-4585-8d00-7a7b234bfbcf';

function initFirebase() {
  if (getApps().length > 0) {
    return getApps()[0];
  }
  const rawJson = (process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '').trim();
  if (rawJson) {
    let parsed;
    try {
      const jsonText = rawJson.startsWith('{')
        ? rawJson
        : Buffer.from(rawJson, 'base64').toString('utf8');
      parsed = JSON.parse(jsonText);
    } catch (err) {
      throw new Error('Invalid FIREBASE_SERVICE_ACCOUNT_JSON format: ' + err.message);
    }
    return initializeApp({ credential: cert(parsed) });
  }
  return initializeApp({ credential: applicationDefault() });
}

const firebaseApp = initFirebase();
const adminAuth = getAuth(firebaseApp);
const db = getFirestore(firebaseApp, DEFAULT_DB_ID);

const app = express();
app.set('trust proxy', true);

const allowedOrigins = (process.env.ALLOWED_ORIGINS || '')
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean);

app.use(
  cors({
    origin(origin, callback) {
      // Native Android apps / server-to-server webhooks do not send a browser Origin header.
      if (!origin) return callback(null, true);
      if (allowedOrigins.includes(origin)) return callback(null, true);
      return callback(new Error('Origin not allowed by CORS'));
    },
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-FamGateway-Signature'],
  })
);

// Capture raw bytes so HMAC-SHA256 webhook verification matches the exact payload signed by FamGateway.
app.use(
  express.json({
    limit: '1mb',
    verify(req, _res, buf) {
      req.rawBody = buf;
    },
  })
);

let cachedSecretState = {
  apiKey: '',
  backendUrl: '',
  enabled: true,
  fetchedAt: 0,
};
const SECRET_CACHE_TTL_MS = 10_000;
const orderVerifyCooldown = new Map();

async function getFamGatewayCredentials(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && now - cachedSecretState.fetchedAt < SECRET_CACHE_TTL_MS) {
    return cachedSecretState;
  }

  let docData = {};
  try {
    const snap = await db.collection('admin_secrets').doc('famgateway').get();
    if (snap.exists) {
      docData = snap.data() || {};
    }
  } catch (err) {
    console.warn('Could not read /admin_secrets/famgateway from Firestore:', err.message);
  }

  const envApiKey = (process.env.FAMGATEWAY_API_KEY || '').trim();
  const firestoreApiKey = typeof docData.apiKey === 'string' ? docData.apiKey.trim() : '';
  const firestoreUrl = typeof docData.backendUrl === 'string' ? docData.backendUrl.trim() : '';
  const enabled = typeof docData.enabled === 'boolean' ? docData.enabled : true;

  cachedSecretState = {
    apiKey: firestoreApiKey || envApiKey,
    backendUrl: firestoreUrl || (process.env.PUBLIC_BASE_URL || '').trim(),
    enabled,
    fetchedAt: now,
  };
  return cachedSecretState;
}

function normalizePublicHttpsUrl(candidateUrl, req) {
  const raw = (candidateUrl || '').trim().replace(/\/+$/, '');
  const resolved =
    raw ||
    (req && req.get('host')
      ? `${req.protocol === 'https' || req.get('x-forwarded-proto') === 'https' ? 'https' : req.protocol}://${req.get('host')}`
      : '');

  if (!resolved) return '';
  try {
    const parsed = new URL(resolved);
    if (parsed.protocol !== 'https:' && parsed.protocol !== 'http:') return '';
    const host = parsed.hostname.toLowerCase();
    if (
      host === 'localhost' ||
      host === '127.0.0.1' ||
      host === '0.0.0.0' ||
      host.startsWith('10.') ||
      host.startsWith('192.168.') ||
      /^172\.(1[6-9]|2\d|3[0-1])\./.test(host)
    ) {
      return '';
    }
    return parsed.origin;
  } catch {
    return '';
  }
}

async function verifyFirebaseBearer(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    const match = /^Bearer\s+(.+)$/i.exec(header);
    if (!match) {
      return res.status(401).json({ status: 'error', message: 'Missing Firebase authorization token.' });
    }
    const decoded = await adminAuth.verifyIdToken(match[1].trim());
    req.authUser = decoded;
    return next();
  } catch (err) {
    return res.status(401).json({ status: 'error', message: 'Invalid or expired Firebase session.' });
  }
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 12_000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function settleSuccessfulDeposit({ orderId, utr, senderName, verifiedAmount, sourceLabel }) {
  const cleanOrderId = String(orderId || '').trim();
  if (!cleanOrderId) return { outcome: 'INVALID_ORDER' };

  const depositRef = db.collection('deposits').doc(cleanOrderId);
  const configRef = db.collection('app_config').doc('global');

  return db.runTransaction(async (txn) => {
    const [depSnap, cfgSnap] = await Promise.all([txn.get(depositRef), txn.get(configRef)]);
    if (!depSnap.exists) {
      return { outcome: 'NOT_FOUND' };
    }

    const dep = depSnap.data() || {};
    if (dep.status === 'APPROVED') {
      return {
        outcome: 'ALREADY_APPROVED',
        orderId: cleanOrderId,
        amount: Number(dep.amount || 0),
        utr: dep.utrNumber || utr || '',
        bonusApplied: Number(dep.bonusApplied || 0),
      };
    }

    if (dep.status === 'REJECTED') {
      return {
        outcome: 'ALREADY_REJECTED',
        orderId: cleanOrderId,
        amount: Number(dep.amount || 0),
      };
    }

    const expectedAmount = Number(dep.amount || 0);
    if (Number.isFinite(verifiedAmount) && verifiedAmount > 0 && Math.abs(verifiedAmount - expectedAmount) > 1.0) {
      throw new Error(`Amount mismatch for order ${cleanOrderId}: expected ${expectedAmount}, got ${verifiedAmount}`);
    }

    const userRef = db.collection('users').doc(dep.userId);
    const depTxRef = userRef.collection('transactions').doc(cleanOrderId);
    const bonusTxRef = userRef.collection('transactions').doc(`BONUS_${cleanOrderId}`);

    const [userSnap, depTxSnap, bonusTxSnap] = await Promise.all([
      txn.get(userRef),
      txn.get(depTxRef),
      txn.get(bonusTxRef),
    ]);

    if (!userSnap.exists) {
      throw new Error(`User profile ${dep.userId} missing for deposit ${cleanOrderId}`);
    }

    const user = userSnap.data() || {};
    const cfg = cfgSnap.exists ? cfgSnap.data() || {} : {};
    const firstBonusSetting = Number(cfg.firstDepositBonusAmount ?? 12);
    const alreadyGotBonus = Boolean(user.hasReceivedFirstDepositBonus);
    const appliedBonus = !alreadyGotBonus && firstBonusSetting > 0 && !bonusTxSnap.exists ? firstBonusSetting : 0;

    const currentBalance = Number(user.balance || 0);
    const newBalance = currentBalance + expectedAmount + appliedBonus;
    const cleanUtr = String(utr || '').trim() || dep.utrNumber || `FG_${cleanOrderId}`;
    const payerSuffix = senderName ? ` · ${String(senderName).trim()}` : '';
    const nowMillis = Date.now();

    txn.update(userRef, {
      balance: newBalance,
      hasReceivedFirstDepositBonus: alreadyGotBonus || appliedBonus > 0,
      updatedAt: FieldValue.serverTimestamp(),
    });

    txn.update(depositRef, {
      status: 'APPROVED',
      utrNumber: cleanUtr,
      bonusApplied: appliedBonus,
      adminNote: `Auto-verified via ${sourceLabel}${payerSuffix}`,
      updatedAt: FieldValue.serverTimestamp(),
    });

    const txSubtitle = `FamGateway Auto · UTR ${cleanUtr}${payerSuffix}`;
    if (depTxSnap.exists) {
      txn.update(depTxRef, {
        status: 'APPROVED',
        subtitle: txSubtitle,
      });
    } else {
      txn.set(depTxRef, {
        userId: dep.userId,
        category: 'DEPOSIT',
        title: `FamPay Gateway Deposit · ₹${Math.round(expectedAmount)}`,
        subtitle: txSubtitle,
        amount: expectedAmount,
        status: 'APPROVED',
        createdAtMillis: nowMillis,
        createdAt: FieldValue.serverTimestamp(),
      });
    }

    if (appliedBonus > 0) {
      txn.set(bonusTxRef, {
        userId: dep.userId,
        category: 'BONUS',
        title: `First Deposit Welcome Bonus (+₹${Math.round(appliedBonus)})`,
        subtitle: 'Auto-credited on first FamGateway UPI deposit',
        amount: appliedBonus,
        status: 'COMPLETED',
        createdAtMillis: nowMillis + 1,
        createdAt: FieldValue.serverTimestamp(),
      });
    }

    return {
      outcome: 'APPROVED_NOW',
      orderId: cleanOrderId,
      amount: expectedAmount,
      utr: cleanUtr,
      senderName: senderName || '',
      bonusApplied: appliedBonus,
      newBalance,
    };
  });
}

async function rejectFailedDeposit(orderId, reason) {
  const cleanOrderId = String(orderId || '').trim();
  if (!cleanOrderId) return { outcome: 'INVALID_ORDER' };

  const depositRef = db.collection('deposits').doc(cleanOrderId);
  return db.runTransaction(async (txn) => {
    const depSnap = await txn.get(depositRef);
    if (!depSnap.exists) return { outcome: 'NOT_FOUND' };
    const dep = depSnap.data() || {};
    if (dep.status !== 'PENDING') {
      return { outcome: `ALREADY_${dep.status}`, orderId: cleanOrderId };
    }

    const depTxRef = db
      .collection('users')
      .doc(dep.userId)
      .collection('transactions')
      .doc(cleanOrderId);
    const depTxSnap = await txn.get(depTxRef);

    const safeReason = String(reason || 'Payment failed or expired').slice(0, 220);
    txn.update(depositRef, {
      status: 'REJECTED',
      bonusApplied: 0,
      adminNote: safeReason,
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (depTxSnap.exists) {
      txn.update(depTxRef, {
        status: 'REJECTED',
        subtitle: `Failed / Expired · ${safeReason}`,
      });
    }
    return { outcome: 'REJECTED_NOW', orderId: cleanOrderId };
  });
}

app.get('/health', async (_req, res) => {
  const creds = await getFamGatewayCredentials();
  res.json({
    ok: true,
    service: 'kelywin-famgateway-backend',
    enabled: creds.enabled,
    apiKeyConfigured: Boolean(creds.apiKey),
  });
});

async function handleCreateOrder(req, res) {
  try {
    const uid = req.authUser.uid;
    const rawAmount = Number(req.body?.amount);
    if (!Number.isFinite(rawAmount) || rawAmount <= 0) {
      return res.status(400).json({ status: 'error', message: 'Enter a valid positive deposit amount.' });
    }
    const cleanAmount = Math.round(rawAmount * 100) / 100;

    const creds = await getFamGatewayCredentials();
    if (!creds.enabled) {
      return res.status(503).json({ status: 'error', message: 'FamGateway deposits are currently paused by Admin.' });
    }
    if (!creds.apiKey) {
      return res.status(503).json({
        status: 'error',
        message: 'FamGateway Secret API Key is not configured yet in Admin App -> UPI QR.',
      });
    }

    const [userSnap, cfgSnap] = await Promise.all([
      db.collection('users').doc(uid).get(),
      db.collection('app_config').doc('global').get(),
    ]);

    if (!userSnap.exists) {
      return res.status(404).json({ status: 'error', message: 'User wallet profile not found.' });
    }
    const user = userSnap.data() || {};
    if (user.blocked === true) {
      return res.status(403).json({ status: 'error', message: 'Your account is blocked. Contact Kelywin Admin.' });
    }

    const cfg = cfgSnap.exists ? cfgSnap.data() || {} : {};
    if (cfg.enableDeposits === false) {
      return res.status(403).json({ status: 'error', message: 'Deposits are temporarily paused by Admin.' });
    }

    const minDeposit = Number(cfg.minDeposit ?? 100);
    const maxDeposit = Number(cfg.maxDeposit ?? 50000);
    if (cleanAmount < minDeposit || cleanAmount > maxDeposit) {
      return res.status(400).json({
        status: 'error',
        message: `Deposit amount must be between ₹${minDeposit} and ₹${maxDeposit}.`,
      });
    }

    const publicBaseUrl = normalizePublicHttpsUrl(creds.backendUrl, req);
    const webhookUrl = publicBaseUrl ? `${publicBaseUrl}/famgateway-webhook` : undefined;
    const customerName = String(req.body?.customerName || user.kwId || 'Kelywin User').slice(0, 60);
    const customerPhone = String(req.body?.customerPhone || user.phoneNumber || '').replace(/\D/g, '').slice(-10);

    const payload = {
      amount: cleanAmount,
      customer_name: customerName,
      ...(customerPhone.length === 10 ? { customer_phone: customerPhone } : {}),
      ...(webhookUrl ? { webhook_url: webhookUrl } : {}),
    };

    const fgResp = await fetchWithTimeout(`${FAMGATEWAY_BASE_URL}/api/create-order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Api-Key': creds.apiKey,
      },
      body: JSON.stringify(payload),
    });

    const fgJson = await fgResp.json().catch(() => ({}));
    if (!fgResp.ok || fgJson.status !== 'success' || !fgJson.data?.order_id) {
      const msg = fgJson.message || `FamGateway returned HTTP ${fgResp.status}`;
      return res.status(502).json({ status: 'error', message: msg });
    }

    const data = fgJson.data;
    const orderId = String(data.order_id);
    const payableAmount = Number(data.payable_amount || data.amount || cleanAmount);
    const nowMillis = Date.now();
    const expiresAtMillis = nowMillis + 5 * 60 * 1000;
    const upiId = String(data.upi_id || '');
    const upiIntent = String(data.upi_intent || '');
    const qrUrl = String(data.qr_url || '');
    const checkoutUrl = String(data.checkout_url || '');
    const userPhoneDisplay = `${user.countryCode || '+91'} ${user.phoneNumber || ''}`.trim() || user.email || uid;

    const batch = db.batch();
    batch.set(db.collection('deposits').doc(orderId), {
      userId: uid,
      orderId,
      userPhone: userPhoneDisplay,
      amount: payableAmount,
      utrNumber: '',
      upiIdUsed: upiId,
      gatewayName: 'FamGateway Auto UPI',
      status: 'PENDING',
      bonusApplied: 0,
      adminNote: 'Awaiting automatic FamPay confirmation (No UTR)',
      createdAtMillis: nowMillis,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    batch.set(
      db.collection('users').doc(uid).collection('transactions').doc(orderId),
      {
        userId: uid,
        category: 'DEPOSIT',
        title: `FamPay Auto Deposit · ₹${Math.round(payableAmount)}`,
        subtitle: `Order ${orderId} · Waiting for automatic UPI confirmation`,
        amount: payableAmount,
        status: 'PENDING_GATEWAY_INTEGRATION',
        createdAtMillis: nowMillis,
        createdAt: FieldValue.serverTimestamp(),
      }
    );
    await batch.commit();

    return res.json({
      status: 'success',
      orderId,
      amount: payableAmount,
      payableAmount,
      upiId,
      upiIntent,
      qrUrl,
      checkoutUrl,
      expiresAtMillis,
      isFamGateway: true,
    });
  } catch (err) {
    console.error('Create order error:', err.message);
    return res.status(500).json({
      status: 'error',
      message: err.message || 'Unable to create FamGateway order.',
    });
  }
}

app.post('/api/famgateway/create-order', verifyFirebaseBearer, handleCreateOrder);
app.post('/create-qr', verifyFirebaseBearer, handleCreateOrder);

app.get('/api/famgateway/status', verifyFirebaseBearer, async (req, res) => {
  try {
    const uid = req.authUser.uid;
    const orderId = String(req.query.orderId || '').trim();
    if (!orderId || !/^[a-zA-Z0-9_\-]+$/.test(orderId)) {
      return res.status(400).json({ status: 'error', message: 'Valid orderId is required.' });
    }

    const depSnap = await db.collection('deposits').doc(orderId).get();
    if (!depSnap.exists) {
      return res.status(404).json({ status: 'error', message: 'Deposit order not found.' });
    }
    const dep = depSnap.data() || {};
    if (dep.userId !== uid) {
      return res.status(403).json({ status: 'error', message: 'You do not have access to this order.' });
    }

    if (dep.status === 'APPROVED') {
      return res.json({
        status: 'SUCCESS',
        orderId,
        amount: Number(dep.amount || 0),
        utr: dep.utrNumber || '',
        bonusApplied: Number(dep.bonusApplied || 0),
        message: 'Deposit already verified and credited.',
      });
    }

    if (dep.status === 'REJECTED') {
      return res.json({
        status: 'FAILED',
        orderId,
        amount: Number(dep.amount || 0),
        message: dep.adminNote || 'Payment failed or expired. Zero balance credited.',
      });
    }

    const now = Date.now();
    const lastPollAt = orderVerifyCooldown.get(orderId) || 0;
    if (now - lastPollAt < 3000) {
      return res.json({
        status: 'PENDING',
        orderId,
        amount: Number(dep.amount || 0),
        message: 'Checking FamPay credit status...',
      });
    }
    orderVerifyCooldown.set(orderId, now);

    const creds = await getFamGatewayCredentials();
    if (!creds.apiKey) {
      return res.json({
        status: 'PENDING',
        orderId,
        amount: Number(dep.amount || 0),
        message: 'Waiting for Admin Secret API key configuration.',
      });
    }

    const verifyResp = await fetchWithTimeout(
      `${FAMGATEWAY_BASE_URL}/api/verify-order.php?order_id=${encodeURIComponent(orderId)}`,
      {
        method: 'GET',
        headers: {
          'X-Api-Key': creds.apiKey,
        },
      }
    );

    const verifyJson = await verifyResp.json().catch(() => ({}));
    const remoteStatus = String(verifyJson.status || '').toLowerCase();

    if (remoteStatus === 'success') {
      const settled = await settleSuccessfulDeposit({
        orderId,
        utr: verifyJson.utr,
        senderName: verifyJson.sender_name,
        verifiedAmount: Number(verifyJson.amount),
        sourceLabel: 'FamGateway Verify API',
      });
      return res.json({
        status: 'SUCCESS',
        orderId,
        amount: settled.amount || Number(dep.amount || 0),
        utr: settled.utr || '',
        senderName: settled.senderName || '',
        bonusApplied: settled.bonusApplied || 0,
        message: 'Payment confirmed automatically! Balance credited.',
      });
    }

    const ageMs = now - Number(dep.createdAtMillis || now);
    if (remoteStatus === 'expired' || remoteStatus === 'failed' || verifyResp.status === 408 || ageMs > 340_000) {
      await rejectFailedDeposit(orderId, 'Payment window expired without FamPay bank credit');
      return res.json({
        status: 'FAILED',
        orderId,
        amount: Number(dep.amount || 0),
        message: 'Payment failed or expired. Zero balance added.',
      });
    }

    return res.json({
      status: 'PENDING',
      orderId,
      amount: Number(dep.amount || 0),
      message: 'Waiting for UPI transfer confirmation...',
    });
  } catch (err) {
    console.error('Order status error:', err.message);
    return res.status(500).json({ status: 'error', message: err.message || 'Status verification error.' });
  }
});

function verifyHmacSignature(rawBuffer, receivedSig, secretKey) {
  if (!receivedSig || !secretKey || !rawBuffer) return false;
  const expectedHex = crypto.createHmac('sha256', secretKey).update(rawBuffer).digest('hex');
  const receivedClean = String(receivedSig).trim().toLowerCase();
  if (receivedClean.length !== expectedHex.length) return false;
  return crypto.timingSafeEqual(Buffer.from(receivedClean, 'utf8'), Buffer.from(expectedHex, 'utf8'));
}

app.post('/famgateway-webhook', async (req, res) => {
  try {
    const signature = req.headers['x-famgateway-signature'];
    const rawBuffer = req.rawBody || Buffer.from(JSON.stringify(req.body || {}), 'utf8');

    let creds = await getFamGatewayCredentials(false);
    let signatureValid = verifyHmacSignature(rawBuffer, signature, creds.apiKey);
    if (!signatureValid) {
      // Refresh secret once in case Admin rotated the key inside Admin APK seconds ago.
      creds = await getFamGatewayCredentials(true);
      signatureValid = verifyHmacSignature(rawBuffer, signature, creds.apiKey);
    }

    if (!signatureValid) {
      return res.status(401).json({ status: 'error', message: 'Invalid webhook signature.' });
    }

    const body = req.body || {};
    const status = String(body.status || '').toLowerCase();
    const orderId = String(body.order_id || '').trim();

    if (!orderId) {
      return res.status(400).json({ status: 'error', message: 'Missing order_id.' });
    }

    if (status === 'success') {
      const settled = await settleSuccessfulDeposit({
        orderId,
        utr: body.utr || body.transaction_id,
        senderName: body.sender_name,
        verifiedAmount: Number(body.amount),
        sourceLabel: 'FamGateway Webhook',
      });
      return res.status(200).json({ status: 'received', outcome: settled.outcome });
    }

    if (status === 'expired' || status === 'failed') {
      const rejected = await rejectFailedDeposit(orderId, `FamGateway webhook: payment ${status}`);
      return res.status(200).json({ status: 'received', outcome: rejected.outcome });
    }

    return res.status(200).json({ status: 'ignored', receivedStatus: status });
  } catch (err) {
    console.error('Webhook processing error:', err.message);
    return res.status(500).json({ status: 'error', message: err.message });
  }
});

app.use((err, _req, res, _next) => {
  console.error('Unhandled server error:', err.message);
  res.status(500).json({ status: 'error', message: err.message || 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`Kelywin FamGateway backend listening on port ${PORT}`);
});

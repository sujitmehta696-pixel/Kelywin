const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "admin_uid_001";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinus(ms = 1000) {
  return new Date(Date.now() - ms);
}

function makeUserDoc(uid) {
  const ts = nowMinus(5000);
  return {
    userId: uid,
    kwId: "KW123456",
    countryCode: "+91",
    phoneNumber: "9876543210",
    email: "",
    passwordHash: "a".repeat(64),
    encryptedPassword: "enc_blob_value",
    secretCode: "KW-8F4K-29X7",
    secretCodeHash: "b".repeat(64),
    inviteCode: "KW999999",
    balance: 0,
    hasReceivedFirstDepositBonus: false,
    blocked: false,
    lastLoginAtMillis: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any Kelywin collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("app_config").doc("global").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("bets").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(makeUserDoc(ALICE_UID))
  );
});

test("Authenticated user creates ₹0 profile, cannot self-credit bonus or balance, and cannot read Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeUserDoc(ALICE_UID))
  );

  // Reject creating profile with pre-credited balance
  const tamperedBob = makeUserDoc(BOB_UID);
  tamperedBob.balance = 5000;
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(tamperedBob));

  // Alice cannot update hasReceivedFirstDepositBonus or blocked or read Bob
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hasReceivedFirstDepositBonus: true,
      updatedAt: nowMinus(1000),
    })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("User bets support ₹1+ stakes, filtered userId queries succeed, unfiltered fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowMinus(2000);
  await assertSucceeds(
    aliceDb.collection("bets").doc("BET-001").set({
      userId: ALICE_UID,
      periodId: "20261121100000001",
      modeId: "30sec",
      selectionType: "NUMBER",
      selectionValue: "7",
      baseStake: 1,
      multiplier: 1,
      totalStake: 1,
      status: "PENDING",
      payout: 0,
      createdAtMillis: Date.now(),
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query matches rule
  await assertSucceeds(
    aliceDb.collection("bets").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered query rejected
  await assertFails(aliceDb.collection("bets").get());
});

test("Only verified Admin can toggle app_config, room_controls (XIN_LOGIC), approve deposits, and list users", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "sujitmehta696@gmail.com",
      role: "ADMIN",
      createdAt: nowMinus(10000),
    });
    await ctx.firestore().collection("users").doc(ALICE_UID).set(makeUserDoc(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "sujitmehta696@gmail.com",
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "sujitmehta696@gmail.com",
    email_verified: false,
  }).firestore();

  // Regular user and unverified email spoof cannot write room_controls or app_config
  await assertFails(
    aliceDb.collection("room_controls").doc("30sec").set({
      modeId: "30sec",
      controlMode: "XIN_LOGIC",
      fallbackMode: "RANDOM",
      xinPattern: "TWO_HIGH_FOUR_LOW",
      xinAnchorPeriodIndex: 1,
      manualNumber: 7,
      updatedAt: nowMinus(1000),
    })
  );
  await assertFails(
    spoofedAdminDb.collection("room_controls").doc("30sec").set({
      modeId: "30sec",
      controlMode: "XIN_LOGIC",
      fallbackMode: "RANDOM",
      xinPattern: "TWO_HIGH_FOUR_LOW",
      xinAnchorPeriodIndex: 1,
      manualNumber: 7,
      updatedAt: nowMinus(1000),
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    adminDb.collection("room_controls").doc("30sec").set({
      modeId: "30sec",
      controlMode: "XIN_LOGIC",
      fallbackMode: "RANDOM",
      xinPattern: "TWO_HIGH_FOUR_LOW",
      xinAnchorPeriodIndex: 1,
      manualNumber: 7,
      updatedAt: nowMinus(1000),
    })
  );

  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      announcementText: "Live",
      maintenanceMode: true,
      maintenanceMessage: "Scheduled upgrade in progress.",
      enableRegistration: true,
      enableDeposits: true,
      enableWithdrawals: true,
      enableNumberBetting: true,
      firstDepositBonusAmount: 12,
      minBetAmount: 1,
      greenRedMultiplier: 2,
      violetMultiplier: 4.5,
      numberMultiplier: 9,
      dragonTigerMultiplier: 2,
      platformFeeRate: 0.02,
      gatewayName: "Kelywin Dynamic UPI Gateway",
      gatewayApiUrl: "",
      merchantKeyId: "",
      merchantUpiPool: "kelywin.gw1@okaxis",
      merchantName: "Kelywin Payments",
      qrLabel: "Dynamic QR",
      minDeposit: 100,
      maxDeposit: 50000,
      updatedAt: nowMinus(1000),
    })
  );

  // Admin can list users
  await assertSucceeds(adminDb.collection("users").get());
});

test("Only verified Admin can read or write /admin_secrets/famgateway; regular and unauth users are denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "sujitmehta696@gmail.com",
      role: "ADMIN",
      createdAt: nowMinus(10000),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "sujitmehta696@gmail.com",
    email_verified: false,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "sujitmehta696@gmail.com",
    email_verified: true,
  }).firestore();

  const secretPayload = {
    provider: "FAMGATEWAY",
    apiKey: "fg_live_test_secret_123456",
    backendUrl: "https://kelywin-backend.onrender.com",
    enabled: true,
    updatedByUid: ADMIN_UID,
    updatedAtMillis: Date.now(),
    updatedAt: nowMinus(1000),
  };

  // Unauthenticated, regular user, and unverified spoofed email cannot write or read admin_secrets
  await assertFails(unauthDb.collection("admin_secrets").doc("famgateway").set(secretPayload));
  await assertFails(aliceDb.collection("admin_secrets").doc("famgateway").set(secretPayload));
  await assertFails(spoofedAdminDb.collection("admin_secrets").doc("famgateway").set(secretPayload));

  // Verified admin writes successfully
  await assertSucceeds(adminDb.collection("admin_secrets").doc("famgateway").set(secretPayload));
  await assertSucceeds(adminDb.collection("admin_secrets").doc("famgateway").get());

  // Regular user & unauthenticated user still cannot read written secret
  await assertFails(unauthDb.collection("admin_secrets").doc("famgateway").get());
  await assertFails(aliceDb.collection("admin_secrets").doc("famgateway").get());
  await assertFails(aliceDb.collection("admin_secrets").get());
});


# Kelywin FamGateway v2.0 Backend Server

This Node.js server securely bridges **Kelywin User App**, **Kelywin Admin App**, **Cloud Firestore**, and **FamGateway (`https://famgateway.in`)** for **100% automatic, without-UTR** UPI deposits.

## How Admin Controls Everything
- You can enter or update your **FamGateway Secret API Key** and **Backend Server URL** directly inside **Kelywin Admin App -> UPI QR tab -> FamGateway Auto-UPI**.
- The Secret API Key is saved strictly in `/admin_secrets/famgateway` in Firestore (readable ONLY by Admin and this server, never shipped inside the User APK).

## Quick Deploy (Render / Railway / VPS)
1. Deploy `/backend` as a Node.js web service (`npm install` -> `npm start`).
2. Add environment variable `FIREBASE_SERVICE_ACCOUNT_JSON` (paste your Firebase Project Service Account JSON from Firebase Console -> Project Settings -> Service Accounts -> Generate New Private Key).
3. Add `PUBLIC_BASE_URL` = your deployed `https://...` URL (for webhook callbacks).
4. Optional fallback: `FAMGATEWAY_API_KEY` (if set in Admin App, Admin App's live key in `/admin_secrets/famgateway` is used automatically!).
5. Copy your deployed HTTPS URL into **Kelywin Admin App -> UPI QR -> Backend Server URL**, paste your FamGateway API Key, and tap **Save FamGateway Settings**.
6. In `https://famgateway.in/webhooks.php`, add `<YOUR_BACKEND_URL>/famgateway-webhook` (optional, since each order also sends `webhook_url` dynamically AND polls `/api/verify-order.php`).

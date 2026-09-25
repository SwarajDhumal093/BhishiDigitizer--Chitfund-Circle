# Bhishi Digitizer trusted backend

This Node/Express service performs operations that must not be trusted to the Android APK: secure lucky-draw/sealed-auction payout finalisation and, when enabled, Razorpay order/signature/webhook handling.

## Endpoints

- `GET /health` — service status (`payouts` remains available when Razorpay is disabled).
- `POST /payouts/finalize` — authenticated, server-authoritative lucky draw / sealed auction finalisation.
- `POST /payments/create-order` — optional Razorpay order creation.
- `POST /payments/verify` — optional Razorpay verification.
- `POST /payments/webhook` — optional Razorpay capture reconciliation.

## Local setup

```powershell
npm install
Copy-Item .env.example .env
npm start
```

Set `FIREBASE_DATABASE_URL` to the Realtime Database **root** URL only. For Firebase Admin authentication use either:

1. `GOOGLE_APPLICATION_CREDENTIALS` pointing to a local service-account JSON file, or
2. `FIREBASE_SERVICE_ACCOUNT_JSON`, or
3. all of `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, and `FIREBASE_PRIVATE_KEY`.

Never commit `.env`, a service-account JSON/private key, Razorpay secrets, or keystore files.

## Render deployment

The repository includes `render.yaml`. You can create a Render Blueprint from the repository, or create a Web Service manually with:

- Root directory: `payment-backend-reference`
- Build command: `npm ci`
- Start command: `npm start`
- Health check: `/health`

Set these Render environment variables:

- `FIREBASE_DATABASE_URL`
- `FIREBASE_SERVICE_ACCOUNT_JSON` (recommended; paste the entire service-account JSON as a secret value)

Razorpay variables are optional until online payments are enabled:

- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`

After deployment, verify:

```powershell
Invoke-RestMethod https://YOUR-SERVICE.onrender.com/health
```

Then place only the HTTPS base URL (no `/health`) in Android `payment_backend_base_url`.

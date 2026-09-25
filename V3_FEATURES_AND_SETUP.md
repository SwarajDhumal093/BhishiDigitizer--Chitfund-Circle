# Bhishi Digitizer v3.0 — Feature & Setup Checklist

## Included in the code

### Professional dashboard
- Logo-based orange/teal/navy/cream visual system
- Light + dark themes
- Admin metrics: active Bhishis, due payments, pool value, members
- Admin quick actions: Create, QR Invite, Lucky Draw, Sealed Auction, Receipts, Reports
- Member metrics: joined groups, due contributions, pending verification
- Member quick actions: QR Join, Pay, Receipts

### Advanced payout engine
- Dual-verification eligibility check
- Prior-winner exclusion
- Lucky-draw display shuffle + server result
- Server cryptographic nonce + audit hash
- 30-minute sealed-auction session
- Private member bids
- Server-side auction close before bid snapshot
- Lowest valid bid + deterministic tie-break
- Dividend calculation
- Locked result, audit dialog, share action and payout-certificate PDF

### QR joining
- QR generation with ZXing
- QR scanning using Google Code Scanner
- Shareable QR image + group code
- Group preview before joining
- Manual code fallback

### Multilingual
- English / Marathi / Hindi selector
- Saved preference
- Android locale resources and Android 13+ locale config

### Digital receipts
- Receipt list generated from contribution history
- Verified / pending state
- Gateway/offline method support
- Branded PDF generation
- Secure PDF sharing with FileProvider

## Required once
- Firebase project
- `google-services.json`
- Email/Password Firebase Authentication enabled
- Realtime Database created
- `firebase_database_rules.json` published

## Required only for advanced payout finalisation
- Deploy `payment-backend-reference/` with Firebase Admin credentials
- Put its HTTPS URL in `payment_backend_base_url`

## Required only for online Razorpay
- Razorpay test/live key ID + secret
- webhook secret
- webhook configured to the backend

If Razorpay is not configured, offline contribution entry remains usable and the payout backend still runs.

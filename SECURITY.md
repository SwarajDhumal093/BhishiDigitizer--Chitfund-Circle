# Security notes

- Never commit Firebase Admin service-account credentials, `.env`, Razorpay secrets or Android signing keys.
- Keep payout finalisation on the trusted Node backend; do not move winner selection into the Android client.
- Publish and review `firebase_database_rules.json` in Firebase Console before demonstrations or production use.
- Restrict the Firebase/Google API key in Google Cloud Console to the intended Android app/package/signing certificate where applicable.
- The current project is suitable for academic/demo deployment, but a real-money production chit-fund application requires additional legal/compliance review, server-side audit logging, abuse controls and security testing.

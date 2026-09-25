# Private Configuration Checklist

Keep these **outside GitHub**:

- `payment-backend-reference/.env`
- Firebase Admin service-account JSON/private key
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_PRIVATE_KEY`
- Razorpay secret/webhook secret (when enabled)
- Any production signing keystore and its passwords

Safe/public project configuration:

- Android Firebase client `app/google-services.json` (contains client project identifiers, not the Admin private key)
- Render public HTTPS base URL
- Firebase Realtime Database root URL (not a credential by itself; access is enforced by rules/auth)

Before every push, run:

```powershell
git status
```

Confirm `.env`, `node_modules`, `local.properties`, build outputs, service-account JSON and keystores are not staged.

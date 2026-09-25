# Bhishi Digitizer v4.1.0 — Final Polish & Private Setup

## Final polishing included

This package keeps the existing Java/XML Android design and secure Render/Firebase payout architecture, and adds the final reliability/presentation pass requested for the project.

### UI/UX polish
- Contribution states remain explicit: Not paid, Submitted/Awaiting admin, and Paid & Locked.
- Notifications now have a proper empty state instead of a blank screen.
- Notification rows are marked seen when the user opens the notification center.
- Trust Score is no longer based mainly on group count. It now uses actual contribution records, dual-verification/locked history, on-time history, group participation, and open disputes.
- Group Closure now performs a preflight review. The Archive button is enabled only when planned payout cycles are locked and there are no unresolved disputes.
- Auction/draw result continues to show the locked result, winner, pool/audit information, sharing and certificate actions.
- Existing app-wide offline indication remains enabled.

## New requested feature: draw/auction notifications

The app now sends notifications when:
1. A sealed auction starts.
2. A sealed auction ends/finalizes.
3. A lucky draw starts.
4. A lucky draw ends/finalizes.

Delivery has two layers:
- **System push notification:** Firebase Cloud Messaging (FCM), including when the app is in the background (after the user has opened the app and allowed notifications).
- **In-app notification inbox:** the same event is stored under `/notifications/{uid}` and is visible from the app's Notifications screen.

The backend deduplicates start/end events under `/notificationEvents/...`, so repeated taps do not intentionally create duplicate round-event announcements.

## Private / deployment-only changes YOU must make

### 1. Android backend URL
In `app/src/main/res/values/strings.xml`, set:

```xml
<string name="payment_backend_base_url">https://YOUR-REAL-RENDER-SERVICE.onrender.com</string>
```

This URL is public configuration, not a secret. Commit it after the Render service is final.

### 2. Render environment variables — NEVER commit these
Keep these only in Render > Environment:

- `FIREBASE_DATABASE_URL`
- `FIREBASE_SERVICE_ACCOUNT_JSON`

or instead of the JSON variable:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

Future payment gateway secrets must also stay only on Render:
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`

Never put Firebase Admin private keys or Razorpay secrets in Android code, GitHub, `strings.xml`, or `google-services.json`.

### 3. Firebase rules
Publish the included `firebase_database_rules.json` after this update. It tightens notification writes so one authenticated user cannot write directly into another user's notification inbox. The trusted Admin SDK backend can still fan out notifications because Admin SDK bypasses client rules.

### 4. Firebase Cloud Messaging
The Android app already includes Firebase Messaging and the new `BhishiMessagingService`. No legacy FCM server key is stored in the Android app. The Render backend sends messages with Firebase Admin credentials.

On Android 13+, the app asks for notification permission once. If the user declines, system notifications will not appear, but in-app notifications will still be stored and shown in the Notifications screen.

### 5. Render redeploy required
Because `payment-backend-reference/server.js` changed, push this version to GitHub and let Render redeploy the latest commit. Then verify:

```powershell
Invoke-RestMethod https://YOUR-RENDER-SERVICE.onrender.com/health
```

Expected while Razorpay is intentionally disabled:

```text
ok=True, payouts=True, payments=False
```

## Final test checklist

- Login as admin and member on separate devices/emulators/accounts.
- Both users must open the app once and allow notifications.
- Verify both are members of the same group.
- For auction: admin opens auction -> member receives "Sealed auction started"; finalize -> members receive "Sealed auction completed".
- For lucky draw: admin starts draw -> members receive "Lucky draw started"; final result -> members receive "Lucky draw completed".
- Open Notifications screen and verify the same event appears in-app.
- Disable internet and verify the offline banner appears.
- Test Reports, Trust Score, Group Closure and History empty states.
- Verify a group with unresolved disputes/incomplete payout cycles cannot be archived.

## What remains intentionally out of scope

The online payment gateway is still intentionally not enabled. Offline/manual contribution recording + admin verification continue to work. When Razorpay is added later, secrets and payment verification must remain server-side.

# Bhishi Digitizer v4.1.0 — GitHub/Render Ready Java Edition

Bhishi Digitizer is a native **Android Java + XML** application for digitising neighbourhood Bhishi / chit-fund savings circles. The current v4 release uses the supplied Bhishi Digitizer logo as its visual identity and expands the app around a professional dashboard, auditable lucky draw / sealed auction, QR joining, English–Marathi–Hindi UI support, and branded digital payment receipts.

> Android client: Java + XML. No Flutter or Dart is used.
>
> The included Node/Express service is only the trusted server layer for operations that must not be decided by an APK: secure payout finalisation and, when enabled, Razorpay order/signature/webhook handling.

## Core highlights

### 1. Professional branded UI
- Supplied Bhishi Digitizer logo replaces the old app mark on the launcher, splash, authentication and major dashboard surfaces.
- Gradient Contrast visual system based on the logo: deep navy, warm orange/gold, teal/cyan and cream.
- Separate light/dark palettes through Android `values/` and `values-night/`.
- Saved theme switch under **Profile → Dark theme**.
- Splash intro, activity transitions and restrained press/entrance micro-animations.
- Redesigned admin/member dashboards with metrics, banner, quick actions and fixed bottom navigation.
- UI design reference is included at `docs/UI_PREVIEW_V3.png`.

### 2. Advanced lucky draw / sealed auction
- Only members with a dual-verified current contribution are eligible.
- Previous locked payout winners are excluded from later rounds.
- Lucky draw uses a short display shuffle, then asks the trusted backend for the actual result.
- Backend creates a cryptographic nonce + SHA-256 audit material and writes the result once.
- Sealed auction has an admin-opened 30-minute bidding window.
- Firebase rules only allow an eligible member to write their own bid while the window is open.
- When finalisation begins, the backend closes the auction before taking the bid snapshot.
- Lowest valid bid wins; exact lowest-bid ties use the locked cryptographic audit material.
- `(pool - winning bid)` is calculated as a dividend for the other members.
- Locked results expose audit details, shareable result text and a branded **winner-certificate PDF**.

### 3. QR group joining
- Each group has a generated QR payload containing its group ID + 6-digit invite code.
- Admin can show or share a QR invite image from the dashboard/group screen.
- Members can scan with Google Code Scanner without managing camera permission directly.
- App verifies the QR against Firebase and shows a group preview before joining.
- Manual 6-digit code remains available as fallback.

### 4. Multilingual UI
Built-in app language selector:
- English
- मराठी
- हिंदी

The choice is saved independently of login/theme state. Core v3 navigation, dashboards, QR, receipt, payout and settings labels use resource translations. Android 13+ system app-language integration is declared through `res/xml/locales_config.xml`.

### 5. Digital payment receipts
- Receipt history is generated from contribution records across the member’s joined groups.
- Shows group, member, cycle, amount, payment method/reference and dual-verification state.
- Creates a branded PDF using Android `PdfDocument`.
- PDF can be shared securely through `FileProvider`.
- Works for offline/manual contribution records too; Razorpay is not required simply to use the receipt UI.



## Firebase data model

```text
groups/{groupId}
contributions/{groupId}/{yyyy-MM}/{uid}
auctionBids/{groupId}/{yyyy-MM}/{uid}
roundConfigs/{groupId}/{yyyy-MM}
roundSeeds/{groupId}/{yyyy-MM}
payouts/{groupId}/{yyyy-MM}
payments/{groupId}/{yyyy-MM}/{uid}
```

`roundConfigs` controls the sealed-auction window. `payouts` and `roundSeeds` are server-written only. Bid rules require the user to be eligible, dual verified, not a prior payout winner, inside the open window, and before a result is locked.


- Polished notification empty states and read state.
- Tightened notification database rules so users cannot write to another user's inbox.

See `FINAL_POLISHING_AND_PRIVATE_SETUP.md` before final deployment.

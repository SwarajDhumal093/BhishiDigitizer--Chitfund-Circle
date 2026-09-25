# Bhishi Digitizer v4.0.3 — GitHub/Render Ready Java Edition

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

## Open and run

1. Extract the project and open the **BhishiDigitizer_Professional** directory in Android Studio.
2. Let Gradle sync. The wrapper is set to Gradle 8.2 for Android Gradle Plugin 8.2.0.
3. Replace `app/google-services.json` with the file from your own Firebase Android app using package:
   `com.bhishi.digitizer`
4. In Firebase Authentication enable **Email/Password** (the current phone-number UI maps the phone to an internal synthetic email account).
5. Create Firebase Realtime Database.
6. Open Firebase Console → Realtime Database → Rules, paste `firebase_database_rules.json`, and **Publish**.
7. Run the Android app.

### Important Firebase note
Editing `firebase_database_rules.json` locally does not change Firebase. The rules must be published/deployed to your Firebase project.

## Running without Razorpay

Yes. Online gateway payment is optional.

You can use:
- authentication
- admin/member dashboards
- group creation
- QR invite/join
- contribution tracking
- manual/offline payment records
- passbook
- receipts/PDFs
- disputes
- language/theme switching

without Razorpay keys.

The trusted backend can also run **payout finalisation without Razorpay**. If Razorpay keys are absent or left as placeholders, `/health` reports payments disabled while `/payouts/finalize` remains available.

## Secure backend for advanced draw / auction

Folder: `payment-backend-reference/`

The backend provides:
- `GET /health` — shows whether payout service is available and whether online payments are configured.
- `POST /payouts/finalize` — server-authoritative lucky draw / sealed-auction result.
- `POST /payments/create-order` — Razorpay order creation (optional).
- `POST /payments/verify` — Razorpay signature/payment verification (optional).
- `POST /payments/webhook` — capture reconciliation (optional).

### Backend quick start without payment gateway

```text
cd payment-backend-reference
npm install
copy .env.example .env
```

Set Firebase Admin credentials and:

```text
FIREBASE_DATABASE_URL=https://YOUR_PROJECT_ID-default-rtdb.firebaseio.com
```

The Razorpay entries may stay as placeholders if online payment is not being used.

Then:

```text
npm start
```

Deploy behind HTTPS and set:

```xml
<string name="payment_backend_base_url">https://YOUR_BACKEND_DOMAIN.example</string>
```

in `app/src/main/res/values/strings.xml`.

The **advanced lucky draw and sealed-auction final result intentionally require this trusted backend**. That prevents a modified Android admin app from choosing or directly writing a winner.

## Main application files

```text
app/src/main/java/com/bhishi/digitizer/
├── AdminDashboardActivity.java
├── MemberDashboardActivity.java
├── QrInviteActivity.java
├── JoinGroupActivity.java
├── LanguageActivity.java
├── ReceiptsActivity.java
├── ReceiptDetailActivity.java
├── fragments/DrawAuctionFragment.java
├── models/RoundConfig.java
└── utils/
    ├── LocaleManager.java
    ├── QrUtils.java
    ├── ReceiptPdfGenerator.java
    ├── PayoutCertificatePdfGenerator.java
    ├── ThemeManager.java
    └── UiMotion.java

app/src/main/res/
├── drawable-nodpi/
│   ├── app_icon.png
│   ├── bhishi_logo_full.png
│   └── bhishi_logo_mark.png
├── values/          # English + light theme
├── values-hi/       # Hindi
├── values-mr/       # Marathi
├── values-night/    # dark palette
└── xml/
    ├── file_paths.xml
    └── locales_config.xml

firebase_database_rules.json
payment-backend-reference/server.js
docs/UI_PREVIEW_V3.png
docs/BRAND_LOGO_REFERENCE.png
```

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

## Build/validation note

The project resources and source references are statically validated in this package. The user has also previously built the v4 project successfully in Android Studio. After cloning, run **Sync → Clean Project → Rebuild Project** on your machine.


## v4.0.1 runtime fixes

- Fixed the Analytics & Reports screen crash caused by missing mandatory layout parameters on metric labels.
- Audited every layout for missing width/height parameters when no style supplies them.
- Fixed the same latent inflation issue in Explainable Trust Score.
- Payment gateway remains intentionally disabled for this milestone; offline/member-admin verification remains available.


## v4.0.2 connectivity update

- App-wide validated internet monitoring.
- Persistent multilingual offline indicator on every Activity.
- Automatic “Back online” status when connectivity returns.
- Bottom-navigation-aware status placement on dashboard screens.

See `OFFLINE_CONNECTIVITY.md`.


## v4.0.3 GitHub / Render readiness update

- Removed generated Android build output, IDE-local state, Node `node_modules`, local SDK paths and the local backend `.env` from the repository package.
- Fixed `.gitignore`; the previous blanket `*.json` rule would have hidden `package.json`, `firebase_database_rules.json` and `google-services.json` in a new repository.
- Removed an accidental duplicate `payment_backend_base_url` resource that included `http://localhost:8080`. The committed Android value is now the HTTPS deployment placeholder only.
- Added `.env.example` with no real credentials.
- Backend now supports Render-safe Firebase Admin credentials through `FIREBASE_SERVICE_ACCOUNT_JSON` or separate Firebase service-account environment variables, while keeping Application Default Credentials as a fallback.
- Backend binds on `0.0.0.0` and uses Render's `PORT` automatically.
- Added `render.yaml` and `GITHUB_RENDER_DEPLOYMENT.md`.
- Updated Android version to **4.0.3** (`versionCode 6`).

**Never commit a Firebase Admin service-account key, `.env`, Razorpay secret, Android signing keystore or other private credential.**

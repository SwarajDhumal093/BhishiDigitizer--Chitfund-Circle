# GitHub + Render deployment checklist

## Before `git push`

The GitHub-ready package intentionally excludes:

- `.env`
- `local.properties`
- `.idea/`
- `.gradle/`
- `app/build/`
- `payment-backend-reference/node_modules/`
- service-account/private-key files

`package.json`, `package-lock.json`, `firebase_database_rules.json`, and `app/google-services.json` are intentionally **not** hidden by a blanket `*.json` rule.

> `google-services.json` identifies the Firebase Android project and is needed for this app to build against that Firebase project. Do not put Firebase Admin service-account JSON in `app/` or anywhere in the repository.

## GitHub upload

From the project root:

```powershell
git init
git add .
git status
git commit -m "Bhishi Digitizer v4.0.3"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

Before committing, confirm `git status` does **not** show `.env`, `local.properties`, `node_modules`, build directories, or a service-account key.

## Render

Use the included `render.yaml` Blueprint, or create a Web Service manually:

- Root directory: `payment-backend-reference`
- Build: `npm ci`
- Start: `npm start`
- Health check: `/health`

Required secrets/config on Render:

- `FIREBASE_DATABASE_URL`
- `FIREBASE_SERVICE_ACCOUNT_JSON` (recommended)

Razorpay variables can remain unset while online payment is postponed.

## Android after Render deployment

Change `app/src/main/res/values/strings.xml`:

```xml
<string name="payment_backend_base_url">https://YOUR-SERVICE.onrender.com</string>
```

Then Sync, Clean, Rebuild and run the app.

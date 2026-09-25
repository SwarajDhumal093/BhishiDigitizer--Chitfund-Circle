# Offline / Internet Connectivity Indicator — v4.0.2

Bhishi Digitizer is internet-oriented, so every Activity now inherits a shared connectivity monitor from `BaseActivity`.

## Behaviour

- If Android loses a validated internet connection, a persistent branded message appears:
  - **No internet connection • Live data and online actions are temporarily unavailable.**
  - **Settings** opens the device wireless settings.
- On dashboard screens the message is anchored above the bottom navigation so it does not cover navigation controls.
- When internet access returns, the offline message is dismissed automatically and a short **Back online** confirmation appears.
- The indicator follows the selected English / Marathi / Hindi app language.
- A Wi-Fi connection without usable internet is treated as offline by checking Android's `NET_CAPABILITY_VALIDATED`, not only whether Wi-Fi/mobile data is enabled.

## Implementation

- `utils/NetworkMonitor.java` — reusable `ConnectivityManager.NetworkCallback` monitor.
- `BaseActivity.java` — starts/stops monitoring for every app screen and presents the Material status message.
- `AndroidManifest.xml` — adds `ACCESS_NETWORK_STATE`.

This version only indicates connectivity state. Existing Firebase operations are unchanged; future offline-first caching/sync can be added separately if required.

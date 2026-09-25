# Professional redesign summary

## UI / UX

The application now uses a consistent Material-style visual system with deep green as the trust/finance primary, gold for payout/auction emphasis, off-white surfaces, rounded cards, clearer hierarchy, larger controls and better spacing. Login, signup, admin dashboard, member dashboard, group creation/joining, group details, contribution tracking, payout, payment, passbook, trust score, notifications, profile and dispute screens were refined to feel like one product instead of unrelated practical screens.

Layouts use scrollable or weighted containers where appropriate so content does not depend on one fixed phone size. Reusable drawables/styles are used for cards, primary/secondary buttons, icons, chips, hero sections and navigation.

## Lucky draw

The original client-side random selection has been replaced by server-authoritative finalisation. The backend verifies the admin, reconstructs the membership and dual-verified contribution state, excludes prior payout winners, generates a cryptographic nonce, hashes deterministic round material, selects the winner from a sorted eligible list and writes the result using a Firebase transaction. The locked record contains an audit hash.

## Auction

Bidding is private/sealed. Members write only to their own bid path and cannot bid until dual-verified for the month or after receiving their cycle payout. The backend never trusts a client-provided bidder UID: bidder identity comes from the database path. The lowest valid bid wins; exact ties use the audit nonce/hash. The pool discount is split equally among the other group members.

## Payments

`PaymentActivity` uses Razorpay Standard Checkout but does not hold any gateway secret. The server creates the order from the authoritative group amount, verifies Firebase identity, validates the checkout signature, fetches the payment, requires capture before crediting the Bhishi ledger and reconciles capture through a signed webhook. Offline payments stay available as `pending_admin` records and require the admin side of dual verification.

## Security

Firebase rules no longer grant blanket authenticated-user root access. Client writes to `paymentVerified`, `paymentId`, server payout results and audit seeds are denied. Payout finalisation and verified payment writes are Admin-SDK operations. Locked contribution/dispute concepts remain protected at the database layer rather than only in UI state.

## Before production

Use the Firebase Emulator Suite to test rules, use Razorpay test mode for the full payment lifecycle, configure an HTTPS backend, add provider-side SMS/WhatsApp reminder delivery if required, and complete merchant/compliance onboarding before enabling live financial transactions.

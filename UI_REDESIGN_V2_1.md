# Bhishi Digitizer UI Redesign v2.1

This refresh keeps the project native Android **Java + XML** and focuses on a modern financial-app experience.

## Visual direction

- Gradient contrast palette: deep emerald → brand green → teal, with warm gold accents.
- Layered light surfaces in light mode and deep charcoal/emerald surfaces in dark mode.
- Larger corner radii, clearer card borders, cleaner typography hierarchy and consistent spacing.
- Proper vector icons for navigation, profile settings, payments, security and appearance.

## Motion

- Branded splash intro with logo scale/settle, staggered headline/body/feature entry and loading state.
- Activity-level fade/slide transitions from `BaseActivity`.
- Subtle press-scale feedback on clickable controls through `UiMotion`.
- Existing payout-result animation remains in the draw/auction experience.

## Theme switching

Theme switching is in **Profile & preferences → Appearance**. `ThemeManager` stores the selection in a UI-specific SharedPreferences file, so logging out does not erase the visual preference. Android's `AppCompatDelegate` applies light or dark mode across every activity.

## Splash content

The splash now communicates the value of the app before login:

- contributions
- fair payouts
- secure records
- transparent Bhishi management

## Notes

The redesign does not change Firebase, bidding, lucky draw or Razorpay business logic. It changes presentation and interaction behavior only, so the existing backend/security model remains intact.

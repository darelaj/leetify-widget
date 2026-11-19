# Stat Peek

Stat Peek is an unofficial Counter-Strike home-screen widget for Android 7.0 (API 24) and newer. It shows Aim, Utility, Leetify Rating, current Premier rating, and up to ten recent match results. Compact widgets show up to five. Stat Peek is independent of Valve and Leetify.

1. Open **Stat Peek** and enter the 17-digit SteamID64 for your registered, accessible Leetify profile.
2. Optionally enter your own [Leetify API key](https://leetify.com/app/developer). Keyless requests have stricter limits. Tap **Verify and save**.
3. Open your home screen's widget picker, find **Stat Peek**, and add **CS Stats**. The widget displays the last successful fetch time. Tap **Refresh** to request an update, or **View on Leetify** to open the source profile.

Widgets share one refresh about every hour while installed. Android can delay work when the device is offline or saving power. Manual refreshes share a 60-second cooldown, and Leetify may ask the app to wait longer. The separate Premier history widget is [deferred](docs/API_RESEARCH.md#premier-chart-feasibility).

The widget picker lists **CS Stats** (4 columns × 3 rows) and **CS Stats 4×2** (4 columns × 2 rows). Add the 4×2 entry directly. It keeps all four ratings, up to five recent results, the fetch time, and both actions. Exact dimensions vary by launcher.

## Privacy

The app sends your SteamID64 and optional API key directly to Leetify over HTTPS for verification and refreshes. It stores your configuration and refresh timing on the device and protects the key with Android Keystore encryption. Those settings are excluded from backup and device transfer.

The app uses fetched stats to render the widgets without saving response bodies, stats, or history to disk. Android and the launcher can retain the last rendered view until it is replaced. Use **Reset account** to clear local account settings and replace the displayed stats, or remove the widget from the home screen. Removing the last widget stops scheduled refreshes.

The widget includes Leetify's original attribution badge. See the [attribution details](docs/ATTRIBUTION.md) and [Leetify developer guidelines](https://leetify.com/blog/leetify-api-developer-guidelines/).

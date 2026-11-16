# StreamGo Lite

An **independent**, lightweight Android companion for YouTube's official web pages. Not affiliated with or endorsed by Google or YouTube.

## Version 2.0 Beta — signed-in browsing

- **Signed-in Chrome mode (on by default)**: opens YouTube in a Chrome Custom Tab. Sign into YouTube using Google's own browser page. StreamGo never handles your password, tokens, or cookies.
- Search, explore topics, and saved links use Chrome mode when enabled, retaining the browser's YouTube login session.
- One-tap access to YouTube subscriptions and your YouTube library.
- Android sharing: from a video in Chrome or YouTube, choose **Share → StreamGo Lite Beta** to save the YouTube link in StreamGo.
- The original lightweight internal viewer is still accessible by disabling signed-in Chrome mode in Settings. Google login is intentionally **not supported** in that WebView.
- Existing theme, recent search, local bookmarks, and hide-thumbnail settings remain. Hide thumbnails applies only in the **internal WebView**, not in Chrome.
- Open the official YouTube app when Premium-only app features like offline downloads or background play are needed. A Premium subscription is required; sign-in alone is not a membership.

### Why a separate Beta app?

GitHub Actions debug APKs use an ephemeral development signing key, so new builds might not update the earlier installed v1.0. The Beta adds an `applicationIdSuffix '.beta'` so it **installs alongside** the working v1.0 instead of overwriting it. Beta app bookmarks and history are separate from v1.0.

### Build the APK

On GitHub, open **Actions → Build StreamGo Lite APK**. Every push to `main` invokes a build. Open the green build and download the **StreamGo-Lite-APK** artifact, then extract and install `app-debug.apk`.

Requirements: Android 8.0+, Android SDK 35, JDK 17, Gradle 8.9, AndroidX Browser 1.8.0.

### Security and limitations

- No StreamGo sign-in service or app account. Chrome manages YouTube authentication.
- Chrome Custom Tabs share Chrome cookies but do not expose them to StreamGo; on a device without Chrome, the default compatible browser may be used.
- StreamGo cannot verify Premium membership itself, guarantee a particular Premium benefit within a browser, or force YouTube playback quality.
- No YouTube download engine, playback modification, background playback workaround, or ad blocking.
- App needs internet only; saved pages/search terms are stored locally, separately from the original v1.0 installation.

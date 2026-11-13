# SoundBomb: Android app

Package / applicationId: `mamun.sound.fire`
Sources: `app/src/main/java/mamun/sound/fire/`

## Build
1. Open this `Android` folder in Android Studio (JDK 17). It creates the Gradle wrapper and syncs.
2. `AppConfig.BASE_URL` (in `config/AppConfig.java`) = the HTTPS URL of your Web folder's `public/`, ending with a slash.
3. The applicationId cannot change after release. Keep `mamun.sound.fire` once you publish.
4. Create a release keystore and BACK IT UP in two places. Updates only install if signed with the same key.
   Copy `keystore.properties.example` to `keystore.properties` and fill it in.
5. Build > Generate Signed Bundle / APK (APK). `versionCode` must match `latest_version_code` on the server.
6. Test with the RELEASE signing key: the device id (Android ID) changes with the signing key.

## First run
Sign up on the phone, make that user admin (see `Web/README.md`, step 7), then sign out and in.
Settings > Admin panel: add categories first, then sounds; set the Telegram admin link in App settings.

## Files
- `TESTING.md`: the device test plan (run it on a release-signed build).
- `SETUP_STEP3.md`: notes on each feature (update flow, overlay, admin panel, and so on).

## Known gaps
Skeleton loading cards, an upload progress bar for admin uploads, the admin panel in Bangla (falls back to English),
and the device test pass. Check the light theme by eye: it was never seen rendered.
This code has not been compiled yet: send the first build errors and they can be fixed quickly.

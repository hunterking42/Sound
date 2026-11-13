# Setup for the networking + Splash files

## app/build.gradle (dependencies)
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    android { buildFeatures { buildConfig true } }   // needed for BuildConfig.VERSION_CODE
    // minSdk 24, defaultConfig versionCode must match server latest/min_version_code

## AndroidManifest.xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <application android:name=".SoundBombApp" android:usesCleartextTraffic="false" ...>
        <activity android:name=".ui.splash.SplashActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>

## Still needed so it compiles (built next)
LoginActivity, OnboardingActivity, MainActivity, BlockedActivity, MaintenanceActivity, ForceUpdateActivity
(ForceUpdateActivity must fetch version_check itself when started with no extras.)

## Before running
Set AppConfig.BASE_URL to your HTTPS server.

## Manifest: register these too (exported="false")
    .ui.auth.LoginActivity, .ui.auth.SignupActivity, .ui.blocked.BlockedActivity, .ui.maintenance.MaintenanceActivity
Theme: all activities need an AppCompat theme (e.g. Theme.SoundBomb, parent Theme.AppCompat.NoActionBar,
windowBackground @color/ink). Not created yet.

## Force update: manifest additions
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>

    <activity android:name=".ui.update.ForceUpdateActivity" android:exported="false"
        android:configChanges="orientation|screenSize|keyboardHidden"/>   <!-- keeps the download alive on rotation -->

    <provider android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider" android:exported="false"
        android:grantUriPermissions="true">
        <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths"/>
    </provider>

## Testing the update flow
1. Build the new APK with a higher versionCode, signed with the SAME keystore.
2. `sha256sum app-release.apk` -> put it in app_settings.apk_sha256 (64 hex chars; required).
3. Host the APK on HTTPS; set apk_url, latest_version_code/name, changelog.
4. Set min_version_code above the installed app's versionCode, then open the old app.
5. Wrong hash must show the "damaged" message and never open the installer.
Not allowed on Google Play: if you publish there, remove this flow and use Play in-app updates.

## Onboarding + Main: more setup
Dependencies:
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.viewpager2:viewpager2:1.1.0'

Manifest: set the theme on <application android:theme="@style/Theme.SoundBomb"> and register
(exported="false"): .ui.onboarding.OnboardingActivity, .ui.main.MainActivity.
Add <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/> when the overlay service is built (Step 6).
The theme sets viewInflaterClass to AppCompatViewInflater so Buttons keep the gradient background
(Material would otherwise swap them for MaterialButton and ignore it).

Compile check: every class Splash references now exists. Replace AppConfig.BASE_URL before running.

## Settings, Premium, Profile
Manifest (exported="false"): .ui.premium.PremiumActivity, .ui.profile.ProfileActivity
No new dependencies and no new permissions.
Server: settings.php, change_password.php and logout.php are new. Admins set telegram_admin / telegram_channel /
telegram_group / share_text / privacy_url / terms_url in app_settings (the admin panel will edit them in Step 7).
For now: sqlite3 data/soundbomb.sqlite "INSERT OR REPLACE INTO app_settings(key,value) VALUES('telegram_admin','https://t.me/yourname')"
Only https links are opened; "@name" and "t.me/name" are accepted for Telegram.

## Overlay (Step 6): manifest and dependency
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

    <service android:name=".service.OverlayService" android:exported="false"
        android:foregroundServiceType="specialUse">
        <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
            android:value="Floating soundboard bubble that plays sounds over other apps"/>
    </service>

    implementation 'androidx.core:core:1.13.1'      // ServiceCompat; compileSdk must be 34 or higher

Test on a real phone (the emulator cannot judge call audio):
1. Settings > Floating bubble > on. Allow "Display over other apps", come back: it turns on by itself.
2. Open a game, then tap the bubble: the panel opens with favorites first. Tap a sound, then the stop button.
3. Drag the bubble: it snaps to a side and remembers the spot. Rotate the phone: it stays on screen.
4. Pull down the notification: "Stop sound" and "Turn off" work. Sign out: the bubble disappears.
5. Play Store: the overlay permission and a specialUse foreground service are reviewed closely (publish via direct APK first).

## Admin panel, part 1
Manifest (exported="false"): .ui.admin.AdminActivity
First admin (never through the app): sqlite3 data/soundbomb.sqlite "UPDATE users SET role='admin' WHERE username='yourname'"
then sign out and in (or reopen the app) so the app learns the role. The "Admin panel" row appears in Settings > Account.

## Server setup for the admin endpoints (part 2 of Step 7)
- SOUNDBOMB_BASE_URL must be the public URL of Web/public/ (used to build links to uploaded files).
- PHP needs the GD extension (image resizing) and fileinfo. For APK upload raise upload_max_filesize and
  post_max_size to about 200M in php.ini.
- Nginx: block scripts in uploads: location ^~ /uploads/ { location ~ \.php$ { return 403; } }
- Run: php install.php (creates the upload folders).

## Admin panel, part 2 (sounds, categories, banners)
No new permissions or dependencies. Files are chosen with the system file picker (no storage permission needed).
Upload limits match the server: image 5 MB (cropped to 512x512 there), audio 8 MB (mp3 or ogg).
Test: Admin panel > Categories > add "Funny" > Sounds > add a sound with an mp3 and an image > open the app as a user
and pull the Home screen (the new sound is there; a hidden sound is not).

## Notices and banners (user side)
Manifest (exported="false"): .ui.notices.NoticesActivity
No new permissions or dependencies (ViewPager2 is already used by Onboarding).
Test: as admin, Admin panel > Broadcast > send a notice with "pop-up" on. On a second phone (or after reopening the
app 10+ minutes later) the pop-up appears, and the bell on Home shows a dot until the list is opened.
Add a banner (https image + optional https link) and it appears on Home and rotates every 5 seconds.
The first run on a new install or account sets a baseline: old notices never pop up.

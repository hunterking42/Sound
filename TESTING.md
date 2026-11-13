# SoundBomb: test plan (Step 8)

Nothing in this project has been run yet, so this plan is also the way to find the first bugs.
Test with a RELEASE-signed build on a real phone (emulators cannot judge call audio, overlays or Android ID).
Keep two phones if you can (account rules need a second device). Mark each line OK / FAIL / notes.

## A. Backend (about 5 minutes)
Run `./smoke_test.sh` from the Web folder against your server (see Web/README.md). It covers these checklist items automatically:
- [ ] Signup twice from the same device is blocked (409)
- [ ] Login from a second device is rejected (403)
- [ ] Wrong password gives the same message as an unknown user
- [ ] Old app version is rejected (426)
- [ ] Banned user is blocked on every endpoint
- [ ] Normal user calling any /admin endpoint gets 403
- [ ] Admin cannot ban or delete themselves or another admin (422)
- [ ] Password change signs out the old token; logout kills the token
- [ ] Forcing an update without an APK, or min version above latest, is refused
If any line fails, fix the server before testing the app.

## B. Accounts and devices
- [ ] Fresh install: Splash > Onboarding (4 slides, "Not now" on the overlay slide works) > Signup
- [ ] Signup with a 2-letter username shows the inline message, no request is sent
- [ ] Kill the app (swipe away), reopen: goes straight to Home (auto login)
- [ ] Airplane mode + reopen: Home opens with saved sounds and the "Offline" banner
- [ ] Second phone, same username/password: "linked to another phone" on Login
- [ ] Admin > Users > that user > Reset device, then sign in on the second phone: works; the first phone is signed out on next launch
- [ ] Admin bans the user: Blocked screen with the reason, Back closes the app, "Check again" works after unban
- [ ] Admin locks for 2 minutes: Blocked screen shows the time; after it passes, "Check again" lets the user in
- [ ] Maintenance mode ON (Admin > App settings): normal user sees Maintenance; the admin still gets in; OFF restores
- [ ] Change password (Settings): old password stops working; the phone stays signed in
- [ ] Sign out: Login screen; reopening does not auto login; cached sounds are gone

## C. Premium
- [ ] Free user taps a premium sound: Premium screen opens (lock icon visible on the card)
- [ ] Admin gives 1 day: the sound plays after reopening the Premium screen or the app
- [ ] Admin removes premium: tap on a premium sound no longer plays, even if it was downloaded before
- [ ] "Buy via Telegram" opens the admin chat (message also copied to the clipboard)

## D. Sounds, grid and offline
- [ ] Tap plays; the card glows; tap again stops; tapping another sound switches
- [ ] First tap on a new sound shows the spinner, the second play is instant and works in airplane mode
- [ ] Mini player shows the name; its stop button silences everything
- [ ] Heart on Home appears on Favorites and on Category without reloading; toggling offline reverts with a message
- [ ] Long press: Favorite / Share work
- [ ] Search: typing waits a moment, "Search" key saves a recent search, clear (X) restores the list
- [ ] Sort chips (All / Trending / New) and Category chips change the list
- [ ] Settings: volume slider changes loudness; sounds per row 2/3/4 apply when you return to the Home tab
- [ ] Banner slider rotates every 5 s, hides while searching, tap opens its https link
- [ ] Replace a sound's audio in Admin: the app plays the new file (not the old cached one)

## E. Notices
- [ ] Admin sends a notice with pop-up: another phone shows it when the app comes to the front (wait up to 10 min, or restart the app)
- [ ] Bell shows a dot until the list is opened
- [ ] A brand-new account does not get old notices as pop-ups

## F. Overlay (the most device-dependent part)
- [ ] Settings > Floating bubble: permission page opens; coming back turns it on by itself
- [ ] Bubble stays above a game and above the dialer; drag it, it snaps to a side and remembers the place
- [ ] Tap opens the panel with favorites first; a tap outside closes it; the game keeps its keys and touches
- [ ] Notification buttons: "Stop sound" and "Turn off" work
- [ ] Rotate the phone: bubble stays on screen
- [ ] Sign out removes the bubble
- [ ] Android 13+: notification permission dialog appears once
- [ ] REAL CALL TEST: call a friend, speaker on, play a sound. Can they hear it? How loud? (Phones apply echo cancellation: results differ by model. Note them for the FAQ.)

## G. Update flow (needs a second, higher versionCode build signed with the SAME keystore)
- [ ] Admin > Update manager: upload the new APK, set latest and minimum version, turn on force update
- [ ] Old app opens: update screen, Back and Exit close the app
- [ ] "Update now": progress bar, "Install unknown apps" guide the first time, installer opens
- [ ] Cancel the installer, press Update now again: no second download
- [ ] Turn off Wi-Fi mid-download, then retry: it resumes
- [ ] Wrong checksum (edit one character in the SHA-256 field with a link-based update): "damaged" message, installer never opens
- [ ] Old app calling any API after min version is raised gets the update screen (426 path)

## H. Language, theme, vibration
- [ ] Settings > Language > Bangla: screens switch; the Bangla text fits on buttons and chips
- [ ] Theme: Dark (default), Light, System. Check every screen in Light: text on the gradient buttons stays white, hearts readable on images
- [ ] Vibration off: no buzz on play

## I. Admin panel
- [ ] Add category, add sound with image + mp3, edit it, hide it, delete it (files disappear from the server uploads folder)
- [ ] File over the limit (9 MB mp3) is refused on the phone before uploading
- [ ] Users: search, filters, every action, temporary password dialog (Copy works)
- [ ] Broadcast: send, delete
- [ ] Activity log shows every action above, without any password in it

## J. Robustness
- [ ] Rotation on every screen, mid-load: no crash, no duplicate dialogs
- [ ] Developer options > "Don't keep activities": go through the main screens
- [ ] Developer options > Background process limit "No background processes": the bubble comes back when the app is reopened
- [ ] Low storage: playing a new sound gives a message, not a crash
- [ ] Rapid taps on sounds and hearts: no crash, state ends up consistent
- [ ] Slow network (Developer options or a throttled hotspot): spinners, then results or the retry button

## K. Release build (R8)
R8 can break Gson/Retrofit only in release builds, so repeat A-D on the signed release APK:
- [ ] Login, sounds list, favorites, Settings, Admin lists all load (blank lists with no error mean a keep rule is missing)
- [ ] `adb logcat | grep -i "AndroidRuntime\|ClassCast\|Missing type"` stays quiet
- [ ] APK size is sensible and the keystore backup exists in two places

Record failures with: screen, steps, phone model, Android version, and `adb logcat` output. Send them to me and I will fix them.

# Water & Posture — Android reminder app

Reminds you to **drink water and sit up straight** every 30 minutes (adjustable).
Stays completely silent while **Airplane mode** or **Do Not Disturb** is on.

## How it works
- **Opens already ON.** The first launch starts reminders automatically.
- **One big button** turns reminders on or off.
- **Interval:** tap a preset (15 / 30 / 45 / 60 / 90 min) or use −5 / +5 (range 5–240 min).
- **Reminder:** one tap on **Done ✓** (or on the notification) clears it. It also clears itself before the next one.
- **Airplane / DND:** the reminder is skipped silently and the cycle continues.
- **Reboot:** reminders restart automatically.

## Install on your phone (about 10 minutes, one time)
1. Install **Android Studio** (free): https://developer.android.com/studio
2. Unzip this folder, then in Android Studio choose **File → Open** and select `HydratePosture`.
   Wait for the "Gradle sync" to finish. It downloads the build tools the first time.
   If it offers to upgrade the Android Gradle Plugin, accept.
3. On your phone, enable developer mode: **Settings → About phone → tap "Build number" 7 times**.
   Then go to **Settings → Developer options → turn on USB debugging**.
4. Connect the phone by USB, allow the prompt on the phone, select it in the top toolbar,
   and press the green **Run ▶** button.
5. On first open, tap **Allow** for notifications.
   If you see **"Allow on-time reminders"**, tap it once and switch it on.
   Without it, reminders can arrive a few minutes late.

To get a standalone APK file instead: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Copy the APK to your phone and open it.

## If reminders stop on some phones
Some brands (Xiaomi, Oppo, Vivo, OnePlus, Samsung, Realme) aggressively stop background apps.
Go to **Settings → Apps → Water & Posture → Battery** and choose **Unrestricted / No restrictions**.
On Xiaomi, also enable **Autostart**.

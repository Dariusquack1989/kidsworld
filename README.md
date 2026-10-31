# Kids World

A home screen for Android TV made for children, built on the
[Clean Your TV](https://github.com/Gathin23/cleanyourtv) launcher. Children see four illustrated worlds with a mascot
and only the apps a parent allowed. Homework earns extra screen time: a child reports a task on the TV, a parent
confirms it on the phone, and the minutes are added.

![Kids World in motion: worlds, tasks and profiles](media/demo.gif)

The app follows the TV's language: English, or German on a German TV. A parent can also fix the language in the parent area.

## Screenshots

| | |
|---|---|
| ![Outer space world](media/home.jpg) | ![Under the sea world](media/world-sea.jpg) |
| ![Jungle world](media/world-jungle.jpg) | ![Magic world](media/world-magic.jpg) |
| ![Task list on the TV](media/tasks.jpg) | ![Who is playing today](media/profiles.jpg) |
| ![PIN entry](media/parent-pin.jpg) | ![Parent area](media/parent.jpg) |
| ![Turning apps on and off](media/parent-apps.jpg) | ![Phone page: tasks](media/phone-tasks.jpg) |
| ![Phone page: screen time](media/phone-time.jpg) | |

The pictures come from the Android TV emulator with sample data. The network address in the parent area is blurred.

## What it does

- Child profiles, each with its own allowed apps and favourites.
- Daily screen time, bedtime and a parent PIN.
- Tasks: one-off or on chosen weekdays, with extra minutes and optional parent confirmation. Credits are stored once
  per task and day, and the daily maximum is never exceeded.
- Apps that count against screen time, or apps a parent marks as free.
- A web page for the parent's phone on the home network (port 8686): confirm tasks, change time, see usage, send a
  message to the TV. A phone pairs once with a code shown on the TV.

## Build and install

You need JDK 17 and the Android SDK.

```sh
./gradlew assembleRelease
adb connect <tv ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell cmd package set-home-activity io.github.dariusquack.kidsworld/io.github.dariusquack.kidsworld.HomeActivity
adb shell settings put secure enabled_accessibility_services io.github.dariusquack.kidsworld/io.github.dariusquack.kidsworld.addon.kidsmode.KidsGuard
adb shell settings put secure accessibility_enabled 1
```

The accessibility line replaces the list of enabled services. If the TV already has some, keep them, separated by `:`.
Release builds are signed with the debug key, so build from source and keep the build you installed.

To set it up, hold OK for 5 seconds on the home screen and choose a PIN. The phone address is shown in the parent
area. Close network ADB when you are done.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Serves the parent page on the home network. The app sends nothing to the internet. |
| `BIND_ACCESSIBILITY_SERVICE` (`KidsGuard`) | Sees which app is in front, sends the TV home for apps that are not allowed, filters a few remote keys, counts screen time and shows messages. It does not read content inside apps. |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `ACCESS_FINE_LOCATION`, `WRITE_SECURE_SETTINGS` and others | Inherited from the base launcher, see `app/src/main/AndroidManifest.xml`. Read that file before you install. |

## Limits

- This is a hurdle for children, not a guarantee. Android offers no system-wide lock to an app like this.
- The phone page uses plain HTTP. Use it only on a home network you trust.
- A force-stop in Android's settings turns the guard off, and Android does not restart it. The parent area shows the
  state, and the phone page stops responding.
- Time counts while a counted app is in front, including a paused video.
- A clock set forward starts a new day. Settings stay behind the PIN for that reason.

## Tested on

waipu tv stick (SEI700WP), Android 11, and the Android TV 11 emulator. Remote keys, the phone page on a real phone and
live TV are still being tested.

## Credits

Base launcher: [Clean Your TV](https://github.com/Gathin23/cleanyourtv) by Gathin, MIT. Fonts: see the licence files.
Pictures: made with Google Gemini for this project.

# Remote and hardware test

For the waipu tv stick (SEI700WP) and its original remote. The key list comes from the stick's own key layout,
`Vendor_2d01_Product_c006.kl`. Other remotes differ.

Fill in "Actual" and mark each row OK or NOT OK. Test once on the home screen and once inside an allowed app.

## Before you start

- At least one child with two allowed apps, and one app that is off.
- Time account on, with time left.
- Keep the phone page open to see the result.

## Keys

| Key | Android name | Expected on the Kids World home screen | Expected inside an allowed app | Actual | OK? |
|---|---|---|---|---|---|
| Up, Down | `DPAD_UP`, `DPAD_DOWN` | Change world | Handled by the app | | |
| Left, Right | `DPAD_LEFT`, `DPAD_RIGHT` | Move between tiles | Handled by the app | | |
| OK, short | `DPAD_CENTER` | Open the tile | Handled by the app | | |
| OK, 5 seconds | `DPAD_CENTER` | Open the PIN screen | Handled by the app | | |
| Back | `BACK` | To the first tile, then to the profile choice | Handled by the app, usually one step back | | |
| Home | `HOME` | Back to the first tile | Back to Kids World | | |
| Volume up, down, mute | `VOLUME_*` | Volume changes | Volume changes | | |
| Play/Pause | `MEDIA_PLAY_PAUSE` | Nothing | Handled by the app | | |
| Rewind, Forward, Previous | `MEDIA_REWIND`, `MEDIA_FAST_FORWARD`, `MEDIA_PREVIOUS` | Nothing | Handled by the app | | |
| Record | `MEDIA_RECORD` | Nothing | Handled by the app | | |
| Channel up, down | `CHANNEL_UP`, `CHANNEL_DOWN` | Nothing | Handled by the app | | |
| Digits 0 to 9 | `0` to `9` | Nothing, except in PIN entry | Handled by the app | | |
| Guide | `GUIDE` | Nothing, or back to Kids World if an app opens | Handled by the app | | |
| Info | `INFO` | Nothing, or an overlay | Handled by the app | | |
| TV | `TV` | Back to Kids World if the waipu app is off for this child | Same | | |
| Assistant | `ASSIST` | Back to Kids World if a full screen opens | Same | | |
| Vendor key | `NES_SHORTCUT_09` | Back to Kids World if a blocked app opens | Same | | |
| Netflix | `STEM_1` | Opens only if allowed, otherwise back to Kids World | Same | | |
| waipu-thek | `NES_SHORTCUT_02` | Opens only if allowed, otherwise back to Kids World | Same | | |
| YouTube | `NES_SHORTCUT_04` | Opens only if allowed, otherwise back to Kids World | Same | | |
| Power | `POWER` | Standby | Standby | | |

## Situations

| Test | Expected | Actual | OK? |
|---|---|---|---|
| Change between two allowed apps with Home | No error, the time keeps counting | | |
| Open an app that is off through a remote key | Back to Kids World within a moment | | |
| Time runs out inside an app | The app closes and the home screen stays usable | | |
| Bedtime starts inside an app | The app closes and the bedtime screen shows | | |
| Lock the TV from the phone | Only OK and volume work on the home screen | | |
| Message from the phone during a video | The message covers the app and OK closes it | | |
| Standby for 5 minutes during an app | Standby time is not counted | | |
| Wake from standby | Guard running, phone page reachable | | |
| Pull the power, plug it in again | Kids World starts, the guard runs | | |
| Wi-Fi off and on | The phone shows the warning, then recovers by itself | | |

## HDMI and live TV

| Test | Expected | Actual | OK? |
|---|---|---|---|
| TV switches its own HDMI input away and back | Kids World is still there | | |
| Live TV in the waipu app, allowed | Plays and counts as screen time | | |
| Live TV in the waipu app, off | Back to Kids World | | |
| HDMI-CEC: TV off | Stick goes to standby if CEC is on | | |
| HDMI-CEC: TV on | Stick wakes, Kids World or the last app shows | | |
| Picture and sound in Netflix, Disney+, Prime Video | Normal | | |

# FAQ

## Phone page

### The phone shows `ERR_TOO_MANY_RETRIES`, or the page will not open

First try `http://<tv ip>:8686/icon-192.png`, then your router's address, for example `http://192.168.178.1`.

If neither opens but the internet works, the browser has no access to your home network. Check these in order:

1. Open the phone's app settings for the browser, go to Permissions and allow **Nearby devices** or **Local network**.
   This is the usual cause on newer Android versions, including Samsung phones.
2. On an iPhone, open Settings, then the browser, and turn on **Local Network**.
3. Turn off VPN apps, ad blockers and parental control apps.
4. In the Wi-Fi details, set Proxy to None and IP settings to DHCP.
5. Make sure the phone is on the same Wi-Fi as the TV, not a guest network, and turn off mobile data for a moment.

If the router opens but the page does not, check the address. It must start with `http://`, not `https://`, and end
with `:8686`. Turn off your browser's "Always use secure connections" option if it keeps switching to `https://`.

### Why can I only make a shortcut, not install an app?

Browsers install web apps only over HTTPS, and this page uses plain HTTP on your home network. Use the browser's menu
and choose "Add to Home screen". It gives you a shortcut with the Kids World icon.

### The shortcut stopped working

The TV probably got a new address. The current one is in the parent area on the TV. Give the TV a fixed address in your
router and make the shortcut again.

### A second phone asks for a code

Each phone pairs once. Enter the PIN, then the 6-digit code that appears on the TV. The code lasts 5 minutes.

### I do not see a change after an update

Reload the page and sign in again. An update ends open sessions. If the old page stays, open a new tab.

### Can I see the MAC address of a paired phone?

No. Android does not give apps this information, and many phones use a random one on each network. The device list
shows the IP address instead, which you can look up in your router.

## On the TV

### How do I open the parent area?

Hold OK for 5 seconds on the home screen.

### I forgot the PIN

There is no reset. Remove the app and install it again, which deletes the PIN, children, tasks and time accounts.

### How do I switch to another child?

Press Back until the first tile is selected, then press Back once more. Back also leaves the profile choice.

### An app is missing on the home screen

Apps are set per child. Check them in the parent area under "Turn apps on and off", or on the phone under Screen time.

### A task says "not today"

It is not available on this weekday, or it is excluded at weekends. It stays in the list but cannot be reported.

### How do I install a new app?

Turn on the Play Store for the child, install the app, and turn the Play Store off again. Then turn on the new app. If
the Play Store is left on, a child can install apps. Screen time and the time window also apply to it.

### A streaming app stays on its logo or a black screen

Open the app in Android's settings and clear its data, then sign in again. We saw this with Disney+ when its stored
sign-in had expired. Updating the app can also help.

### Kids World does not become the home screen

Some TVs keep their own launcher. See the note under Install in the [README](../README.md). On a waipu tv stick,
Kids World became Home without further steps.

## Screen time

### Time counts although the TV is off

Without HDMI-CEC, the stick does not know the TV is off. It stays awake, the app keeps playing and the time keeps
counting. Turn on HDMI-CEC on the TV and the stick, or put the stick into standby with its own remote.

### Time counts while a video is paused

Yes. Kids World sees which app is in front, not whether it is playing, and not every app reports this reliably.

### A task gave fewer minutes than it says

The extra time for that day was already used up. The log shows the minutes that were credited.

### Children can reach an app after the end time

The guard checks about every 30 seconds, so an app can stay open for that long. It closes the app after that.

### The home screen says to call a parent

The guard is not running. Counted apps do not start until it is back. Turn it on with the commands under Install, then
restart the device. This can happen after a force-stop in Android's settings.

## Security

### Is the phone page safe?

It is made for a home network you trust. See [SECURITY.md](../SECURITY.md). Do not forward port 8686 to the internet.

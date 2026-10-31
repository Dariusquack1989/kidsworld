# Security

Kids World is a hurdle for children. It is not a security product.

## What the phone page protects

- A phone needs the parent PIN and a one-time code shown on the TV. The code has 6 digits, lasts 5 minutes and allows
  5 attempts.
- A paired phone keeps a random device key. The TV stores only a hash of it.
- After sign-in, each request needs a random session token that lasts one hour.
- Five wrong PINs lock the PIN for one minute.

## What it does not protect

- The page uses plain HTTP. The PIN, device key and session token travel unencrypted, so someone who can read traffic on
  your network can capture them. Use a home network you trust, and do not forward port 8686 to the internet.
- Anyone on the network can use up the PIN attempts. That locks the PIN on the TV for a minute too.
- Removing paired phones or changing the PIN does not end sessions that are already open. They end after an hour, or
  when the guard restarts.
- The server listens on every network of the device, including a VPN if one runs on it.

Report a security problem through this repository's issue tracker. Do not post a PIN or device key there.

# ɱTip

Monero gift wallets, simplified.

Create disposable Monero wallets, fund them, print or share QR codes, and let recipients sweep the funds to their own wallet. Crypto gift cards for tips.

## How It Works

**Creator:**
1. Open ɱTip → Create Gift Wallet
2. Fund the displayed address (send XMR to it)
3. Share the QR code or print the PDF gift card

**Recipient:**
1. Scan the QR code with any QR scanner — ɱTip launches automatically
2. Enter your Monero wallet address
3. Funds are swept to your wallet

## Recommended Wallets (to receive funds)

- [Cake Wallet](https://play.google.com/store/apps/details?id=com.cakewallet.cake_wallet) — available on Google Play
- [Monfluo](https://codeberg.org/acx/monfluo) — a pure Monero wallet for Android

## Building

Requires JDK 17 and Android SDK.

**Debug (stagenet):**
```
gradle wrapper --gradle-version 8.9 && ./gradlew :app:assembleDebug
```

**Release (mainnet):**
```
gradle wrapper --gradle-version 8.9 && ./gradlew :app:assembleRelease
```

The debug build connects to stagenet (test network).

The release build connects to mainnet (real XMR).

APK output: `app/build/outputs/apk/{debug,release}/`

## Native Library

ɱTip uses `libwallet2_api_c.so` (built via actions workflow) from [monero_c](https://github.com/MrCyjaneK/monero_c) for Monero operations.

If building locally, place pre-built binaries for arm64-v8a, armeabi-v7a, and x86_64 into `app/src/main/jniLibs/`.


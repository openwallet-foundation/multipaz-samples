# Multipaz Transit Terminal

A sample transit faregate built with [Multipaz](https://github.com/openwallet-foundation/multipaz).
It reads a rider's Digital Payment Credential (DPC) over ISO 18013-5 proximity (NFC tap),
records a journey, and settles the selected fare through a device-attested terminal backend.

This is a development sample. It uses a fictional `Utopia Transit` operator, USD fares, local HTTP,
and development attestation settings.

## How it works

```text
Rider wallet ── NFC tap ──> Transit terminal app ── attested RPC ──> Transit backend ──> Records server
     DPC                                  check in / choose exit / pay          payment key              ledger
```

1. At entry, the terminal reads the rider's DPC and records an open journey using its payment
   instrument ID.
2. The operator selects an exit station (or randomly gets selected for demo purposes). The backend
   stores the fare and the app asks the wallet to authorize that exact amount as ISO 18013
   transaction data.
3. At exit, the same credential must be presented. The terminal backend submits the signed
   presentment to the records server, which verifies it and settles the payment.

The Android app contains no payment-processor signing key. It authenticates to the backend using
device attestation; the backend owns the key used to authenticate to the records server.

## Age-based fares

The terminal can request an identity credential alongside the DPC to determine the rider's fare
class. It accepts an mDL, Photo ID, or EU Personal ID and derives the class from issuer-signed age
claims in this order:

| Class    | Eligibility                          | Fare applied      |
|----------|--------------------------------------|-------------------|
| Child    | Under 18                             | 33% discount      |
| Senior   | 65 or older                          | 50% discount.     |
| Standard | No qualifying age claim or age 18–64 | Full listed fare. |

The app uses `age_over_65`, `age_over_18`, `age_in_years`, or `birth_date` where available (in that
order). If the wallet does not present an identity credential, the journey continues with the
standard fare. The derived fare class is recorded at check-in and the discounted amount is what the
wallet authorizes at checkout; it is not a user-selectable discount.

## Project layout

| Path              | Purpose                                                                                  |
|-------------------|------------------------------------------------------------------------------------------|
| `androidApp/`     | Android application entry point and Android permissions.                                 |
| `shared/`         | Kotlin Multiplatform Compose UI, proximity reader, journey client, and payment client.   |
| `transitBackend/` | JVM/Ktor backend that verifies app attestation, stores journeys, and proxies settlement. |
| `iosApp/`         | Xcode host application for the shared Compose UI.                                        |

The primary configuration points are:

- `shared/src/commonMain/kotlin/org/multipaz/transit/Constants.kt` — backend URL, payee account,
  currency, and terminal identity.
- `transitBackend/src/main/resources/resources/default_configuration.json` — server port, records
  server URL, app-attestation requirements, and the development payment-processor identity.

## Prerequisites

- JDK 17.
- Android Studio with an Android SDK, plus a physical NFC-capable Android device running API 29 or newer.
- `adb` when using a physical Android device.
- A compatible Multipaz Utopia records server and a holder wallet containing a DPC
  (`org.multipaz.payment.sca.1`). The records server must trust the DPC issuer, trust this
  terminal's payment-processor root, and contain both payer and payee accounts. The committed
  payee account is `30000001`.

For iOS development, use macOS and Xcode. NFC reader mode requires a signed device build with the
appropriate Core NFC entitlement.

## Run it end to end

### 1. Start the Utopia universe server

- use [this branch](https://github.com/VishnuSanal/multipaz-utopia/tree/transit) - this adds a enw
  account for the transit terminal inthe SOR

Start a Multipaz Utopia server that is compatible with the snapshot dependency declared in
`gradle/libs.versions.toml`.

```bash
./gradlew run
```

The backend's default is `http://localhost:8004`. If the records server is elsewhere, pass its base
URL when starting the backend. The backend adds `/rpc` itself:

```bash
./gradlew :transitBackend:run --args="-param records_server_url=http://localhost:8100/registry"
```

### 2. Start the transit backend

```bash
./gradlew :transitBackend:run
```

The default backend listens on port `8011`. Confirm it is available at
`http://localhost:8011/`; it responds with `MultipazTransitTerminal backend is running`.

### 3. Install the Android app and expose the backend

```bash
./gradlew :androidApp:installDebug
adb reverse tcp:8011 tcp:8011
```

`Constants.kt` intentionally uses `http://localhost:8011/rpc`. `adb reverse` makes that address
on a physical device reach the development machine. For a remote terminal backend, set the
URL in `Constants.kt` to its reachable HTTPS endpoint.

### 4. Issue DPC & age credential

- issue a DPC from the utopia universe bank
- either generate (or issue) an age credential in the holder you are using. if you are using
  testapp, you can
  use [this branch](https://github.com/VishnuSanal/multipaz-identity-credential/tree/age) and
  generate child and senior citizen credentials locally

### 4. Run a journey

1. Open the app and permissions as needed.
2. At **check-in**, present the rider wallet with NFC - this requests a DPC and an age credential.
3. Choose an exit station and fare (or this gets automatically selected with a timeout)
4. At **checkout**, present the same wallet again. A successful settlement shows a receipt; a
   failed attestation, credential, or payment shows the decline reason.

## Build and test

```bash
./gradlew :androidApp:assembleDebug
./gradlew :transitBackend:run
./gradlew :shared:testAndroidHostTest
```

Open `iosApp/` in Xcode to build and run the iOS host app. Kotlin/Native iOS targets require a
macOS build host.

## Development configuration and security

`default_configuration.json` is deliberately suitable only for local development:

- it listens on HTTP and accepts software-level Android keystore attestation;
- the app package and debug signing-certificate digest are pinned in `client_requirements`;
- the payment-processor private key is in the sample configuration; and
- the debug Android manifest enables cleartext traffic.

Before using this pattern beyond a demo, use TLS, hardware-backed / Play Integrity attestation as
appropriate, production issuer and processor trust roots, and store the payment key in a secure
server-side key-management system or HSM. Never ship a payment-processor private key in an app.

If you sign the Android app with a different certificate or use a different application ID, update
`client_requirements.android.app_signature_certificate_digests` and `app_packages` in the backend
configuration. A mismatch prevents the app from registering with the backend.

## Troubleshooting

| Symptom                            | Likely cause                                                                                                     | Resolution                                                                 |
|------------------------------------|------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| Cannot connect to terminal backend | Backend is stopped or device port forwarding is missing.                                                         | Start `:transitBackend:run` and run `adb reverse tcp:8011 tcp:8011`.       |
| Registration / attestation fails   | The APK package or signing digest differs from the backend configuration.                                        | Update `client_requirements` to match the installed build.                 |
| Payment transaction is rejected    | The records server does not trust the DPC issuer or terminal payment-processor root, or the accounts are absent. | Configure the same trust roots and seed the payer plus account `30000001`. |
| The second tap fails               | A different DPC was presented at checkout.                                                                       | Present the same credential used at check-in.                              |
| NFC does not start                 | Device NFC is disabled or unsupported, or the wallet does not support NFCv2.                                     | Enable NFC and use an NFCv2-compatible wallet.                             |

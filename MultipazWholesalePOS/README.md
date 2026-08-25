# Multipaz Wholesale POS

A point-of-sale terminal that accepts payments from a customer's **Digital Payment Credential
(DPC)** presented over **ISO 18013-5 proximity** (NFC tap or QR + BLE),
using [Multipaz](https://github.com/openwallet-foundation/multipaz).

The terminal reads the customer's DPC, has the customer's device **cryptographically authorize the
exact amount** (SCA payment `transaction_data`), and **settles the payment on a ledger** — moving
funds from the customer's account to the merchant's account on a Multipaz **records server** (the
"System of Record", or SoR). The POS app itself holds **no signing key**: it proves it is a genuine
terminal build via **device attestation** to a small **terminal backend**, which holds the payment
key.

---

## What happens when a customer pays

```
┌──────────┐   QR / NFC    ┌───────────────┐  device attestation   ┌───────────────────┐   pp-leaf   ┌────────────────────┐
│  Holder  │  proximity    │   POS app     │  (Android key attest, │  Terminal backend │  signature  │                    │
│ (wallet, ├──────────────▶│ (reader + UI) ├──────────────────────▶│      :8110        ├────────────▶│  Records server    │
│  a DPC)  │  presentment  │               │  RpcAuthorizedDevice  │  holds pp-leaf,   │  Payment    │  verifies mdoc +   │
└──────────┘               └───────────────┘  Client               │  checks app sig   │  Processor  │  moves funds       │
                                                                   └───────────────────┘   RPC       └────────────────────┘
```

1. **Reserve** — once proximity engagement has happened and the reader needs a request, the app
   calls
   the terminal backend's `createTransaction(amount)`, which forwards to the SoR and gets back a
   server-minted `transactionId` (plus a nonce and the payee's display name).
2. **Read + bind** — the app builds an ISO 18013-5 `DeviceRequest` for the DPC and attaches an SCA
   `transaction_data` object (`PaymentTransaction.Payload`) carrying that `transactionId` +
   amount/currency/payee, in the doc request's `DocRequestInfo.otherInfo`. The customer's wallet
   **device-signs the hash** of it as part of the response.
3. **Settle** — the app wraps the response in an `Iso18013PresentmentRecord` and sends it to the
   terminal backend's `commitTransaction`, which forwards it to the SoR. **The SoR is the
   authority**: it re-verifies the mdoc issuer + device signatures, looks up the pending
   transaction by the device-signed `transactionId`, checks the signed amount/currency match what
   was reserved, checks the issuer trust chain, and then **debits the payer / credits the payee** on
   its ledger. The app deliberately does *not* re-enforce the amount binding locally.
4. **Result** — on success the screen shows **SETTLED / TRANSACTION VERIFIED** with the ledger
   confirmation id, an "AMOUNT — AUTHORIZED BY CARD" row, and cardholder/issuer/masked-card details
   read back out of the presentment for the receipt. Any failure (backend unreachable, unbound
   amount, untrusted issuer, unknown account, RPC error, cashier cancel) lands on a full-screen
   **failure** screen showing the decline reason, with **NEW TRANSACTION** to retry.

The customer's DPC is an ISO 18013-5 mdoc with docType/namespace `org.multipaz.payment.sca.1`
(`DigitalPaymentCredential.CARD_DOCTYPE`), whose `payment_instrument_id` claim is the customer's
ledger account number.

---

## Architecture & the three trust gates

Three independent checks must all pass for a payment to settle:

| Gate                       | Question                                                               | Where it's enforced                                                       | How it's satisfied here                                                                                                                                                                        |
|----------------------------|------------------------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1. App genuineness**     | Is this a real POS terminal build?                                     | Terminal backend, via `RpcAuthInspectorAssertion` + `client_requirements` | The app does **Android key attestation**; the backend checks the attestation's app **signing-cert digest + package**                                                                           |
| **2. Terminal → SoR auth** | Is this terminal allowed to move money?                                | SoR, via `RpcAuthInspectorSignature` against its `PAYMENT_PROCESSOR` root | The terminal backend signs with **`pp-leaf`**, whose cert chains to **`pp-root`**, which the SoR trusts (`root_identities.payment_processor`)                                                  |
| **3. Credential trust**    | Is the DPC from a trusted issuer, and did the holder sign this amount? | SoR, in `commitTransaction`                                               | Issuer chain verified against the SoR's `TrustManager`; the device-signed `transaction_data` amount + currency must equal the reserved transaction; the transaction may only be committed once |

Gate 1 is the point of this design: the payment key (`pp-leaf`) lives **only** on the terminal
backend, never in the distributed APK. The app authenticates by *being a genuine build*, checked
cryptographically via key attestation.

---

## Repository layout

```
MultipazWholesalePOS/
├── androidApp/       Android entry point (MainActivity → App())
├── shared/           KMP shared code — the whole app: Compose UI, proximity reader, settlement client
│   └── src/
│       ├── commonMain/    App(), Constants, ui/, proximity/, payment/
│       ├── androidMain/    Platform.android.kt (ktor Android engine)
│       └── iosMain/        Platform.ios.kt (ktor Darwin engine) + MainViewController
├── iosApp/           iOS entry point (see “iOS status” below)
└── terminalBackend/  JVM server: the merchant's terminal backend (device-attested PaymentProcessor proxy)
```

Key files:

- `shared/…/App.kt` — the whole flow: creates the `ProximityReaderModel` + `RpcPaymentSettler`,
  drives `PosAppState` between the amount-entry, checkout, settlement and failure screens, and
  calls `commit` when the transfer completes.
- `shared/…/Constants.kt` — terminal backend URL, payee account, payee name/id, currency.
- `shared/…/proximity/ProximityReaderModel.kt` — the ISO 18013-5 reader state machine (engagement →
  request → response), plus `ProximityUtils.kt` for the NFC-handover and QR-scan entry points.
- `shared/…/proximity/VerificationProximityTransferScreen.kt` — the checkout screen: NFC/QR
  engagement UI, reserves the transaction, builds the `DeviceRequest` with the SCA
  `transaction_data`, and hands the finished `Iso18013PresentmentRecord` back to `App()`.
- `shared/…/payment/RpcPaymentSettler.kt` — the settlement client: device-attestation handshake
  (`RpcAuthorizedDeviceClient`) + the two `PaymentProcessor` RPCs. Fully common; platforms inject
  only an HTTP engine (`expect object Platform`), and the `SecureArea` comes from Multipaz's own
  `org.multipaz.util.Platform`.
- `shared/…/payment/PaymentProcessor.kt` — the client-side copy of the `PaymentProcessor` RPC
  interface + its request/response types, KSP-generated into a stub by `multipaz-cbor-rpc`.
- `shared/…/payment/PaymentCardClaims.kt` — best-effort extraction of the cardholder/issuer/masked
  card claims from the settled presentment, for the receipt.
- `shared/…/payment/Settlement.kt` — `SettlementResult` (`Approved`/`Declined`),
  `PaymentCardDetails`,
  scan-mode enums.
- `terminalBackend/…/TerminalPaymentProcessor.kt` — implements `PaymentProcessor` with device
  attestation, forwards to the SoR with `pp-leaf`.
- `terminalBackend/…/resources/resources/default_configuration.json` — the backend's baked-in config
  (port, `client_requirements`, `records_server_url`, and `pp-leaf`).

---

## Prerequisites

1. **A running records server (SoR) + multipaz utopia stack** — the Utopia Registry, run either directly or from the Docker Utopia
   stack (both covered below). It must have `pp-root` as its `root_identities.payment_processor`,
   trust the DPC's issuer, and have the payer/payee accounts seeded.
2. **A holder** — a wallet holding a DPC (`org.multipaz.payment.sca.1`) whose
   `payment_instrument_id` is a seeded payer account. The Registry's `TrustManager` trusts **only
   its own IACA root**, so the DPC has to be issued by an issuer enrolled with that Registry (e.g.
   the Utopia Bank of Utopia backend pointed at the same Registry) — a credential issued under some
   other root is refused at `commitTransaction`.
3. **An Android device or emulator**, and `adb`.

---

## Build & run — end to end

The terminal backend and app are the same either way; only how you run the SoR differs.

### Step 1 — Start Multipaz Utopia

```
cd /path/to/multipaz-utopia
./gradlew run
```

- SoR link in this case directly (matches the backend's committed default of `http://localhost:8004`)

**Docker Utopia stack:**

```
cd /path/to/multipaz-utopia
./gradlew :deployment:buildDockerImage
docker run --rm -p 8100:8100 multipaz-utopia/server-bundle:latest
# the Registry (SoR) is behind the nginx front door at http://localhost:8100/registry/
```

With the bundle, point the terminal backend at the proxied Registry — the backend appends `/rpc`
itself:

```
./gradlew :terminalBackend:run --args="-param records_server_url=http://localhost:8100/registry"
```

### Step 2 — start the terminal backend

```
./gradlew :terminalBackend:run
```

No arguments needed for the direct-Registry path — everything (including `pp-leaf`) is in its baked
`default_configuration.json`. It listens on **:8110**.

### Step 3 — build, install, and bridge the port

```
./gradlew :androidApp:installDebug
adb reverse tcp:8110 tcp:8110      # device localhost:8110 → host terminal backend
```

### Step 4 — issue a DPC

- issue a DPC to your wallet from the bank of utopia from the multipaz utopia (if you haven't already)

### Step 5 — take a payment

In the app: enter an amount → **CHECKOUT NOW** → hold the holder's DPC to the phone (NFC) or switch
to QR and scan its share QR. On success you'll see **SETTLED / TRANSACTION VERIFIED** with the
ledger confirmation id and the card details.

### Step 6 — verify funds moved

Open the Registry front-end and drill into the merchant identity ("Utopia Wholesale POS", account
`20000001`) to see the transaction. In the Docker bundle it is served at
`http://localhost:8100/registry/`; run locally it is a separate Kotlin/JS module
(`:organizations:registry:frontend`).

---

## Configuration reference

### Terminal backend (`terminalBackend/…/default_configuration.json`)

```jsonc
{
  "server_port": 8110,
  "database_engine": "ephemeral",                       // no db file; re-registers clients each run
  "admin_password": "multipaz",
  "records_server_url": "http://localhost:8004",        // the SoR; override via -param for Docker
  "client_requirements": {                              // GATE 1 — who may call this backend
    "android": {
      "gms_attestation": false,                         // no Play Integrity (dev)
      "verified_boot_green": false,
      "keystore_security_level": "software",            // accept emulator/software-level attestation
      "app_signature_certificate_digests": ["<APK signing SHA-256>"],
      "app_packages": ["org.multipaz.pos"]
    }
  },
  "server_identities": { "payment_processor": { …pp-leaf JWK… } }   // GATE 2 — the terminal's key
}
```

Get your APK's signing digest (debug builds use `~/.android/debug.keystore`):

```
keytool -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA256
```

### App (`shared/…/Constants.kt`)

- `DEFAULT_TERMINAL_URL = http://localhost:8110/rpc` — the terminal backend (via `adb reverse`).
  A commented-out `trycloudflare.com` URL is kept alongside it for tunnelling to a non-local
  backend.
- `DEFAULT_PAYEE_ACCOUNT = 20000001` — the merchant account sent in `createTransaction`.
- `TERMINAL_PAYEE_NAME` / `TERMINAL_PAYEE_ID` / `TERMINAL_CURRENCY` — what goes into the
  device-signed `transaction_data` payee and currency.

### Keys (`pp-root` / `pp-leaf`)

A single EC P-256 keypair chain: **`pp-root`** is a self-signed CA (the SoR's `PAYMENT_PROCESSOR`
trust root), **`pp-leaf`** is a cert signed by it (the terminal's identity). `pp-leaf` lives in the
terminal backend config under `server_identities.payment_processor`; `pp-root` lives in the SoR
config under `root_identities.payment_processor`.

---

## Security model & dev-vs-production

This sample runs at a **dev tier**. What's real vs. what a production terminal would change:

| Aspect              | Here (dev)                                                                            | Production                                                   |
|---------------------|---------------------------------------------------------------------------------------|--------------------------------------------------------------|
| App verification    | Android key attestation, **software** keystore level, no Play Integrity               | `gms_attestation: true` (Play Integrity) + hardware keystore |
| Payment key custody | `pp-leaf` committed in the backend config (like the Wallet's dev `server_identities`) | key in a secret manager / HSM, never in git                  |
| SoR trust root      | shared self-signed `pp-root`                                                          | terminal **enrolls** with the SoR's CA for a real cert       |
| Cleartext HTTP      | allowed (`usesCleartextTraffic` in the **debug** manifest, `http://…`)                | TLS everywhere                                               |
| Issuer trust        | the SoR's own local IACA root only                                                    | the real payment-network trust anchors                       |

What is *already* production-shaped: the app holds no key and is verified by attestation; the
payment key is server-side; settlement is card-bound and authoritative on the SoR; the mdoc issuer +
device signatures are verified.

---

## Troubleshooting

| Symptom (Declined / server error)                                            | Cause                                                                       | Fix                                                                                                                              |
|------------------------------------------------------------------------------|-----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `encryptionInfo is required for verifyNonce`                                 | SoR built **without** the proximity nonce patch                             | Ensure the SoR resolves the patched `0.101.0-SNAPSHOT` from mavenLocal (Docker: `--refresh-dependencies` / `exclusiveContent`)   |
| `Could not connect to terminal backend - is the backend running/accessible?` | `:terminalBackend:run` not up, or no `adb reverse tcp:8110 tcp:8110`        | Start the backend and re-run the port bridge; the app retries the handshake on the next transaction                              |
| `Transaction '…' is invalid or expired`                                      | reservation expired (20 min) or SoR restarted between reserve and commit    | Start a new transaction                                                                                                          |
| `Inconsistent transaction amount or currency`                                | device-signed `transaction_data` doesn't match the reserved amount          | Both sides must agree on `TERMINAL_CURRENCY` and the amount; retry the sale                                                      |
| `Unknown account …`                                                          | payer (`payment_instrument_id`) or payee (`20000001`) not seeded            | Seed both via `/identity/load` (Docker seeds from `records.json`)                                                                |
| `Payment instrument is not issued by a trusted issuer`                       | the DPC was issued under a root the SoR doesn't trust                       | Issue the DPC from an issuer enrolled with this Registry's CA, or add the issuer cert to the SoR's `createTrustManager()`        |
| RPC auth / signature failure at the SoR                                      | `pp-root` not in the SoR config, or `pp-leaf` doesn't chain to it           | Set `root_identities.payment_processor = pp-root` on the SoR                                                                     |
| Attestation `register` fails at the terminal backend                         | app signing digest / package mismatch                                       | The backend logs the digests it saw ("Digest N: …"); copy it into `client_requirements`                                          |
| `Not a payment transaction`                                                  | the response carried no DPC, or the wallet didn't attach `transaction_data` | Holder must have a `org.multipaz.payment.sca.1` credential and a wallet that registers `PaymentTransaction` (`addUtopiaTypes()`) |

---

## iOS status

**iOS is implemented** — it just needs a macOS build host. The whole flow is multiplatform: the
Compose UI, the proximity reader, and the settlement client (`RpcPaymentSettler`, the
`PaymentProcessor` wire calls, and `RpcAuthorizedDeviceClient`) all live in `commonMain`, and
`MainViewController` is simply `ComposeUIViewController { App() }` — the same `App()` Android runs.

Each platform supplies only the HTTP engine, via `expect object Platform`:

|             | Android (`Platform.android.kt`)                                     | iOS (`Platform.ios.kt`)                                                                               |
|-------------|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| HTTP engine | ktor `Android`                                                      | ktor `Darwin`                                                                                         |
| Attestation | Android key attestation                                             | **App Attest** on a real device (hardware `DeviceAttestationIos`); software fallback on the simulator |

So settlement works the same way on iOS — the terminal backend just needs an `ios` (and/or
`software`) block in its `client_requirements`, alongside the `android` one.

Kotlin/Native Apple targets (`iosArm64`, `iosSimulatorArm64`) compile only on macOS, and building
the POS's iOS targets against the *patched* SDK needs the SDK's `*-iosarm64` klibs in mavenLocal —
which `publishToMavenLocal` emits only when run on macOS. On a Linux host, no iOS target of this
project can be compiled at all; on a Mac, `publishToMavenLocal -Psnapshot=true` + opening `iosApp/`
in Xcode is all it takes.

**Caveats on iOS:** NFC reader mode needs the CoreNFC entitlement on a **signed** build (no
reader-mode NFC on the simulator); the QR + BLE path works without it. App Attest needs a
provisioned app on a real device; the simulator uses the software-attestation fallback.

---

### Running tests

```
./gradlew :shared:testAndroidHostTest
```

The test source sets (`commonTest`, `androidHostTest`, `iosTest`) currently hold placeholder tests
only.

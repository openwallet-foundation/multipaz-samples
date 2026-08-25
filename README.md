This repository contains samples for the [Multipaz SDK](https://github.com/openwallet-foundation/multipaz).

- [MultipazGettingStartedSample](MultipazGettingStartedSample/) contains a simple app that demonstrates the code snippets from the [developer documentation](https://developer.multipaz.org/docs). It runs on both Android and iOS.
- [MultipazWholesalePOS](MultipazWholesalePOS/) contains a point-of-sale terminal that takes payments from a Digital Payment Credential presented over ISO 18013-5 proximity (NFC or QR + BLE), settling them on a Multipaz records server. It runs on both Android and iOS, and ships with a small terminal backend.
- [MultipazCodelab](MultipazCodelab/) contains the Utopia Wholesale codelab: a [Holder](MultipazCodelab/Holder/) wallet app and a [LoyaltyReader](MultipazCodelab/LoyaltyReader/) proximity reader (a git submodule).
- [SimpleVerifierStandalone](SimpleVerifierStandalone/) contains a ISO/IEC 18013-5:2021 mdoc reader for Android using the multipaz-android-legacy library.

This is not an official or supported Google product.

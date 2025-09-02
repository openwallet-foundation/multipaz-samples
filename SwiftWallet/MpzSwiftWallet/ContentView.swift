//
//  ContentView.swift
//  MpzSwiftWallet
//
//  Created by David Zeuthen on 6/7/25.
//

import SwiftUI
import Multipaz

struct WalletData {
    let storage: Storage
    let secureArea: SecureArea
    let secureAreaRepository: SecureAreaRepository
    let documentTypeRepository: DocumentTypeRepository
    let documentStore: DocumentStore
    let readerTrustManager: TrustManagerLocal

    let presentmentModel = PresentmentModel()
    
    init() async {
        storage = Platform.shared.nonBackedUpStorage
        secureArea = try! await Platform.shared.getSecureArea()
        secureAreaRepository = SecureAreaRepository.Builder()
            .add(secureArea: secureArea)
            .build()
        documentTypeRepository = DocumentTypeRepository()
        documentTypeRepository.addDocumentType(documentType: DrivingLicense.shared.getDocumentType())
        documentStore = DocumentStore.Builder(
            storage: storage,
            secureAreaRepository: secureAreaRepository
        ).build()
        if (try! await documentStore.listDocuments().isEmpty) {
            let now = KotlinClockCompanion().getSystem().now()
            let signedAt = now
            let validFrom = now
            let validUntil = now.plus(duration: 365*86400*1000*1000*1000)
            let iacaKey = Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
            let iacaCert = MdocUtil.shared.generateIacaCertificate(
                iacaKey: iacaKey,
                subject: X500Name.companion.fromName(name: "CN=Test IACA Key"),
                serial: ASN1Integer.companion.fromRandom(numBits: 128, random: KotlinRandom.companion),
                validFrom: validFrom,
                validUntil: validUntil,
                issuerAltNameUrl: "https://issuer.example.com",
                crlUrl: "https://issuer.example.com/crl"
            )
            let dsKey = Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
            let dsCert = MdocUtil.shared.generateDsCertificate(
                iacaCert: iacaCert,
                iacaKey: iacaKey,
                dsKey: dsKey.publicKey,
                subject: X500Name.companion.fromName(name: "CN=Test DS Key"),
                serial:  ASN1Integer.companion.fromRandom(numBits: 128, random: KotlinRandom.companion),
                validFrom: validFrom,
                validUntil: validUntil
            )
            let document = try! await documentStore.createDocument(
                displayName: "Erika's Driving License",
                typeDisplayName: "Utopia Driving License",
                cardArt: nil,
                issuerLogo: nil,
                other: nil
            )
            let _ = try! await DrivingLicense.shared.getDocumentType().createMdocCredentialWithSampleData(
                document: document,
                secureArea: secureArea,
                createKeySettings: CreateKeySettings(
                    algorithm: Algorithm.esp256,
                    nonce: ByteStringBuilder(initialCapacity: 3).appendString(string: "123").toByteString(),
                    userAuthenticationRequired: true
                ),
                dsKey: dsKey,
                dsCertChain: X509CertChain(certificates: [dsCert]),
                signedAt: signedAt,
                validFrom: validFrom,
                validUntil: validUntil,
                expectedUpdate: nil,
                domain: "mdoc")
        }
        let ephemeralStorage = EphemeralStorage(clock: KotlinClockCompanion().getSystem())
        readerTrustManager = TrustManagerLocal(storage: ephemeralStorage, identifier: "default", partitionId: "default_default")
        try! await readerTrustManager.addX509Cert(
            certificate: X509Cert.companion.fromPem(
                pemEncoding: """
                    -----BEGIN CERTIFICATE-----
                    MIICYTCCAeegAwIBAgIQOSV5JyesOLKHeDc+0qmtuTAKBggqhkjOPQQDAzAzMQsw
                    CQYDVQQGDAJVUzEkMCIGA1UEAwwbTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENB
                    MB4XDTI1MDcwNTEyMjAyMVoXDTMwMDcwNTEyMjAyMVowMzELMAkGA1UEBgwCVVMx
                    JDAiBgNVBAMMG011bHRpcGF6IElkZW50aXR5IFJlYWRlciBDQTB2MBAGByqGSM49
                    AgEGBSuBBAAiA2IABD4UX5jabDLuRojEp9rsZkAEbP8Icuj3qN4wBUYq6UiOkoUL
                    MOLUb+78Ygonm+sJRwqyDJ9mxYTjlqliW8PpDfulQZejZo2QGqpB9JPInkrCBol5
                    T+0TUs0ghkE5ZQBsVKOBvzCBvDAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgw
                    BgEB/wIBADBWBgNVHR8ETzBNMEugSaBHhkVodHRwczovL2dpdGh1Yi5jb20vb3Bl
                    bndhbGxldC1mb3VuZGF0aW9uLWxhYnMvaWRlbnRpdHktY3JlZGVudGlhbC9jcmww
                    HQYDVR0OBBYEFM+kr4eQcxKWLk16F2RqzBxFcZshMB8GA1UdIwQYMBaAFM+kr4eQ
                    cxKWLk16F2RqzBxFcZshMAoGCCqGSM49BAMDA2gAMGUCMQCQ+4+BS8yH20KVfSK1
                    TSC/RfRM4M9XNBZ+0n9ePg9ftXUFt5e4lBddK9mL8WznJuoCMFuk8ey4lKnb4nub
                    v5iPIzwuC7C0utqj7Fs+qdmcWNrSYSiks2OEnjJiap1cPOPk2g==
                    -----END CERTIFICATE-----
                    """.trimmingCharacters(in: .whitespacesAndNewlines)
            ),
            metadata: TrustMetadata(
                displayName: "Multipaz Identity Reader",
                displayIcon: nil,
                privacyPolicyUrl: nil,
                testOnly: true,
                extensions: [:]
            )
        )
        try! await readerTrustManager.addX509Cert(
            certificate: X509Cert.companion.fromPem(
                pemEncoding: """
                    -----BEGIN CERTIFICATE-----
                    MIICiTCCAg+gAwIBAgIQQd/7PXEzsmI+U14J2cO1bjAKBggqhkjOPQQDAzBHMQsw
                    CQYDVQQGDAJVUzE4MDYGA1UEAwwvTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENB
                    IChVbnRydXN0ZWQgRGV2aWNlcykwHhcNMjUwNzE5MjMwODE0WhcNMzAwNzE5MjMw
                    ODE0WjBHMQswCQYDVQQGDAJVUzE4MDYGA1UEAwwvTXVsdGlwYXogSWRlbnRpdHkg
                    UmVhZGVyIENBIChVbnRydXN0ZWQgRGV2aWNlcykwdjAQBgcqhkjOPQIBBgUrgQQA
                    IgNiAATqihOe05W3nIdyVf7yE4mHJiz7tsofcmiNTonwYsPKBbJwRTHa7AME+ToA
                    fNhPMaEZ83lBUTBggsTUNShVp1L5xzPS+jK0tGJkR2ny9+UygPGtUZxEOulGK5I8
                    ZId+35Gjgb8wgbwwDgYDVR0PAQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAw
                    VgYDVR0fBE8wTTBLoEmgR4ZFaHR0cHM6Ly9naXRodWIuY29tL29wZW53YWxsZXQt
                    Zm91bmRhdGlvbi1sYWJzL2lkZW50aXR5LWNyZWRlbnRpYWwvY3JsMB0GA1UdDgQW
                    BBSbz9r9IFmXjiGGnH3Siq90geurxTAfBgNVHSMEGDAWgBSbz9r9IFmXjiGGnH3S
                    iq90geurxTAKBggqhkjOPQQDAwNoADBlAjEAomqjfJe2k162S5Way3sEBTcj7+DP
                    vaLJcsloEsj/HaThIsKWqQlQKxgNu1rE/XryAjB/Gq6UErgWKlspp+KpzuAAWaKk
                    +bMjcM4aKOKOU3itmB+9jXTQ290Dc8MnWVwQBs4=
                    -----END CERTIFICATE-----
                    """.trimmingCharacters(in: .whitespacesAndNewlines)
            ),
            metadata: TrustMetadata(
                displayName: "Multipaz Identity Reader (Untrusted Devices)",
                displayIcon: nil,
                privacyPolicyUrl: nil,
                testOnly: true,
                extensions: [:]
            )
        )
    }
}


var walletData: WalletData? = nil

struct ContentView: View {
    @State private var presentmentState: PresentmentModel.State = .idle
    @State private var qrCode: UIImage? = nil
    
    var body: some View {
        VStack {
            switch presentmentState {
            case .idle:
                handleIdle()
                
            case .connecting:
                handleConnecting()

            case .waitingForSource:
                handleWaitingForSource()

            case .processing:
                handleProcessing()
                
            case .waitingForDocumentSelection:
                handleWaitingForDocumentSelection()
                
            case .waitingForConsent:
                handleWaitingForConsent()
                
            case .completed:
                handleCompleted()
            }
        }
        .padding()
        .onAppear {
            Task {
                walletData = await WalletData()
                for await state in walletData!.presentmentModel.state {
                    presentmentState = state
                }
            }
        }
    }

    private func handleIdle() -> some View {
        return Button(action: {
            Task {
                walletData!.presentmentModel.reset()
                walletData!.presentmentModel.setConnecting()
                let connectionMethods = [
                    MdocConnectionMethodBle(
                        supportsPeripheralServerMode: false,
                        supportsCentralClientMode: true,
                        peripheralServerModeUuid: nil,
                        centralClientModeUuid: UUID.companion.randomUUID(),
                        peripheralServerModePsm: nil,
                        peripheralServerModeMacAddress: nil)
                ]
                let eDeviceKey = Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
                let advertisedTransports = try! await ConnectionHelperKt.advertise(
                    connectionMethods,
                    role: MdocRole.mdoc,
                    transportFactory: MdocTransportFactoryDefault.shared,
                    options: MdocTransportOptions(bleUseL2CAP: true)
                )
                let engagementGenerator = EngagementGenerator(
                    eSenderKey: eDeviceKey.publicKey,
                    version: "1.0"
                )
                engagementGenerator.addConnectionMethods(
                    connectionMethods: advertisedTransports.map({transport in transport.connectionMethod})
                )
                let encodedDeviceEngagement = ByteString(bytes: engagementGenerator.generate())
                let qrCodeUrl = "mdoc:" + encodedDeviceEngagement
                    .toByteArray(startIndex: 0, endIndex: encodedDeviceEngagement.size)
                    .toBase64Url()
                qrCode = generateQrCode(url: qrCodeUrl)!
                let transport = try! await ConnectionHelperKt.waitForConnection(
                    advertisedTransports,
                    eSenderKey: eDeviceKey.publicKey,
                    coroutineScope: walletData!.presentmentModel.presentmentScope
                )
                walletData!.presentmentModel.setMechanism(
                    mechanism: MdocPresentmentMechanism(
                        transport: transport,
                        eDeviceKey: eDeviceKey,
                        encodedDeviceEngagement: encodedDeviceEngagement,
                        handover: Simple.companion.NULL,
                        engagementDuration: nil,
                        allowMultipleRequests: false
                    )
                )
                qrCode = nil
            }
        }) {
            Text("Present mDL via QR")
        }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
    }

    private func handleConnecting() -> some View {
        return VStack {
            Text("Present QR code to reader")
            if (qrCode != nil) {
                Image(uiImage: qrCode!)
            }
            Button(action: {
                walletData!.presentmentModel.reset()
            }) {
                Text("Cancel")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        }
    }

    private func handleWaitingForSource() -> some View {
        walletData!.presentmentModel.setSource(source: SimplePresentmentSource(
            documentStore: walletData!.documentStore,
            documentTypeRepository: walletData!.documentTypeRepository,
            readerTrustManager: walletData!.readerTrustManager,
            zkSystemRepository: nil,
            preferSignatureToKeyAgreement: true,
            domainMdocSignature: "mdoc",
            domainMdocKeyAgreement: nil,
            domainKeylessSdJwt: nil,
            domainKeyBoundSdJwt: nil
        ))
        return EmptyView()
    }
    
    private func handleProcessing() -> some View {
        return Text("Communicating with reader")
    }

    private func handleWaitingForDocumentSelection() -> some View {
        // In this sample we just pick the first document, more sophisticated
        // wallets present a document picker for the user
        //
        walletData!.presentmentModel.documentSelected(
            document: walletData!.presentmentModel.availableDocuments.first
        )
        return EmptyView()
    }

    private func handleWaitingForConsent() -> some View {
        return VStack {
            let consentData = walletData!.presentmentModel.consentData
            if (consentData.trustPoint == nil) {
                Text("Unknown mdoc reader is requesting information")
                    .font(.title)
            } else {
                let displayName = consentData.trustPoint?.metadata.displayName ?? "Unknown"
                Text("Trusted mdoc reader **\(displayName)** is requesting information")
                    .font(.title)
            }
            VStack {
                ForEach(consentData.request.requestedClaims, id: \.self) { requestedClaim in
                    Text(requestedClaim.displayName)
                        .font(.body)
                        .fontWeight(.thin)
                        .textScale(.secondary)

                }
            }
            HStack {
                Button(action: {
                    walletData!.presentmentModel.reset()
                }) {
                    Text("Cancel")
                }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)

                Button(action: {
                    walletData!.presentmentModel.consentReviewed(consentObtained: true)
                }) {
                    Text("Consent")
                }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
            }
        }
    }
    
    private func handleCompleted() -> some View {
        Task {
            try! await delay(timeMillis: 2500)
            walletData!.presentmentModel.reset()
        }
        if (walletData!.presentmentModel.error == nil) {
            return VStack {
                Image(systemName: "checkmark.circle")
                    .renderingMode(.original)
                    .symbolRenderingMode(SymbolRenderingMode.multicolor)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 96, height: 96)
                    .symbolEffect(.bounce)
                Text("The information was shared")
            }
        } else {
            return VStack {
                Image(systemName: "xmark")
                    .renderingMode(.original)
                    .symbolRenderingMode(SymbolRenderingMode.multicolor)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 96, height: 96)
                    .symbolEffect(.bounce)
                Text("Something went wrong")
            }
        }
    }
    
    private func generateQrCode(url: String) -> UIImage? {
        let data = url.data(using: String.Encoding.ascii)
        if let filter = CIFilter(name: "CIQRCodeGenerator") {
            filter.setValue(data, forKey: "inputMessage")
            let scalingFactor = 4.0
            let transform = CGAffineTransform(scaleX: scalingFactor, y: scalingFactor)
            if let output = filter.outputImage?.transformed(by: transform) {
                // iOS QR Code generator doesn't add the proper Quiet Zone so we need
                // to do this ourselves. Add four modules as required by the standard.
                //
                let quietZonePadding = 4*scalingFactor
                let context = CIContext()
                let cgImage = context.createCGImage(
                    output,
                    from: CGRect(
                        x: -quietZonePadding,
                        y: -quietZonePadding,
                        width: output.extent.width + 2*quietZonePadding,
                        height: output.extent.height + 2*quietZonePadding
                    )
                )
                return UIImage(cgImage: cgImage!)
            }
        }
        return nil
    }
}

#Preview {
    ContentView()
}

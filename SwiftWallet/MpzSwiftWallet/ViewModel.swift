import Foundation
import UIKit
import Multipaz
import MultipazSwift
import Observation
import SwiftUI

@MainActor
@Observable
class ViewModel {

    var path = NavigationPath()

    var isLoading: Bool = true

    var storage: Storage!
    var secureArea: SecureArea!
    var secureAreaRepository: SecureAreaRepository!
    var documentTypeRepository: DocumentTypeRepository!
    var documentStore: DocumentStore!
    var documentModel: DocumentModel!
    var readerTrustManager: TrustManagerLocal!

    private let presentmentModel = PresentmentModel()

    var provisioningModel: ProvisioningModel!
    var provisioningState: ProvisioningModel.State = ProvisioningModel.Idle()
            
    func load() async {
        storage = IosStorage(
            storageFileUrl: FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: "group.org.multipaz.samples.MpzSwiftWallet.sharedgroup")!
                .appendingPathComponent("storage.db"),
            excludeFromBackup: true
        )
        secureArea = try! await Platform.shared.getSecureArea(storage: storage)
        secureAreaRepository = SecureAreaRepository.Builder()
            .add(secureArea: secureArea)
            .build()
        documentTypeRepository = DocumentTypeRepository()
        documentTypeRepository.addDocumentType(documentType: DrivingLicense.shared.getDocumentType())
        documentStore = DocumentStore.Builder(
            storage: storage,
            secureAreaRepository: secureAreaRepository
        ).build()
        readerTrustManager = TrustManagerLocal(storage: storage, identifier: "default", partitionId: "default_default")
        if (try! await readerTrustManager.getTrustPoints().isEmpty) {
            try! await readerTrustManager.addX509Cert(
                certificate: X509Cert.companion.fromPem(
                    pemEncoding: """
                    -----BEGIN CERTIFICATE-----
                    MIICYTCCAeegAwIBAgIQOSV5JyesOLKHeDc+0qmtuTAKBggqhkjOPQQDAzAzMQswCQYDVQQGDAJV
                    UzEkMCIGA1UEAwwbTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBMB4XDTI1MDcwNTEyMjAyMVoX
                    DTMwMDcwNTEyMjAyMVowMzELMAkGA1UEBgwCVVMxJDAiBgNVBAMMG011bHRpcGF6IElkZW50aXR5
                    IFJlYWRlciBDQTB2MBAGByqGSM49AgEGBSuBBAAiA2IABD4UX5jabDLuRojEp9rsZkAEbP8Icuj3
                    qN4wBUYq6UiOkoULMOLUb+78Ygonm+sJRwqyDJ9mxYTjlqliW8PpDfulQZejZo2QGqpB9JPInkrC
                    Bol5T+0TUs0ghkE5ZQBsVKOBvzCBvDAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgwBgEB/wIB
                    ADBWBgNVHR8ETzBNMEugSaBHhkVodHRwczovL2dpdGh1Yi5jb20vb3BlbndhbGxldC1mb3VuZGF0
                    aW9uLWxhYnMvaWRlbnRpdHktY3JlZGVudGlhbC9jcmwwHQYDVR0OBBYEFM+kr4eQcxKWLk16F2Rq
                    zBxFcZshMB8GA1UdIwQYMBaAFM+kr4eQcxKWLk16F2RqzBxFcZshMAoGCCqGSM49BAMDA2gAMGUC
                    MQCQ+4+BS8yH20KVfSK1TSC/RfRM4M9XNBZ+0n9ePg9ftXUFt5e4lBddK9mL8WznJuoCMFuk8ey4
                    lKnb4nubv5iPIzwuC7C0utqj7Fs+qdmcWNrSYSiks2OEnjJiap1cPOPk2g==
                    -----END CERTIFICATE-----
                    """.trimmingCharacters(in: .whitespacesAndNewlines)
                ),
                metadata: TrustMetadata(
                    displayName: "Multipaz Identity Reader",
                    displayIcon: nil,
                    displayIconUrl: "https://www.multipaz.org/multipaz-logo-200x200.png",
                    privacyPolicyUrl: nil,
                    disclaimer: nil,
                    testOnly: true,
                    extensions: [:]
                )
            )
            try! await readerTrustManager.addX509Cert(
                certificate: X509Cert.companion.fromPem(
                    pemEncoding: """
                    -----BEGIN CERTIFICATE-----
                    MIICiTCCAg+gAwIBAgIQQd/7PXEzsmI+U14J2cO1bjAKBggqhkjOPQQDAzBHMQswCQYDVQQGDAJV
                    UzE4MDYGA1UEAwwvTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBIChVbnRydXN0ZWQgRGV2aWNl
                    cykwHhcNMjUwNzE5MjMwODE0WhcNMzAwNzE5MjMwODE0WjBHMQswCQYDVQQGDAJVUzE4MDYGA1UE
                    AwwvTXVsdGlwYXogSWRlbnRpdHkgUmVhZGVyIENBIChVbnRydXN0ZWQgRGV2aWNlcykwdjAQBgcq
                    hkjOPQIBBgUrgQQAIgNiAATqihOe05W3nIdyVf7yE4mHJiz7tsofcmiNTonwYsPKBbJwRTHa7AME
                    +ToAfNhPMaEZ83lBUTBggsTUNShVp1L5xzPS+jK0tGJkR2ny9+UygPGtUZxEOulGK5I8ZId+35Gj
                    gb8wgbwwDgYDVR0PAQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQAwVgYDVR0fBE8wTTBLoEmg
                    R4ZFaHR0cHM6Ly9naXRodWIuY29tL29wZW53YWxsZXQtZm91bmRhdGlvbi1sYWJzL2lkZW50aXR5
                    LWNyZWRlbnRpYWwvY3JsMB0GA1UdDgQWBBSbz9r9IFmXjiGGnH3Siq90geurxTAfBgNVHSMEGDAW
                    gBSbz9r9IFmXjiGGnH3Siq90geurxTAKBggqhkjOPQQDAwNoADBlAjEAomqjfJe2k162S5Way3sE
                    BTcj7+DPvaLJcsloEsj/HaThIsKWqQlQKxgNu1rE/XryAjB/Gq6UErgWKlspp+KpzuAAWaKk+bMj
                    cM4aKOKOU3itmB+9jXTQ290Dc8MnWVwQBs4=
                    -----END CERTIFICATE-----
                    """.trimmingCharacters(in: .whitespacesAndNewlines)
                ),
                metadata: TrustMetadata(
                    displayName: "Multipaz Identity Reader (Untrusted Devices)",
                    displayIcon: nil,
                    displayIconUrl: "https://www.multipaz.org/multipaz-logo-200x200.png",
                    privacyPolicyUrl: nil,
                    disclaimer: nil,
                    testOnly: true,
                    extensions: [:]
                )
            )
            try! await readerTrustManager.addX509Cert(
                    certificate: X509Cert.companion.fromPem(
                        pemEncoding: """
                            -----BEGIN CERTIFICATE-----
                            MIICPzCCAcWgAwIBAgIQBpWf6aJhn7GaGv3AffPk8TAKBggqhkjOPQQDAzAiMSAwHgYDVQQDDBdN
                            dWx0aXBheiBURVNUIFJlYWRlciBDQTAeFw0yNTA3MjYyMDEwMzBaFw0zMDA3MjYyMDEwMzBaMCIx
                            IDAeBgNVBAMMF011bHRpcGF6IFRFU1QgUmVhZGVyIENBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE
                            L/cxWy6+d5Yf5LX/qkPQhyIhUGoPBIdlJxcaJ/l8gJOOvNSTQlBUvuzD8paQkZKs6fHvt3aGLiGL
                            /bLYMhiQHmO7kVpz9DCI6+X82aZfiaSLMiHCrBC9yF1QiqahaKZxo4G/MIG8MA4GA1UdDwEB/wQE
                            AwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMFYGA1UdHwRPME0wS6BJoEeGRWh0dHBzOi8vZ2l0aHVi
                            LmNvbS9vcGVud2FsbGV0LWZvdW5kYXRpb24tbGFicy9pZGVudGl0eS1jcmVkZW50aWFsL2NybDAd
                            BgNVHQ4EFgQU0B8Z/qjh8qzVXpR5JDdtmPmVx+kwHwYDVR0jBBgwFoAU0B8Z/qjh8qzVXpR5JDdt
                            mPmVx+kwCgYIKoZIzj0EAwMDaAAwZQIxALxFZApDi8GcLiF6DXM41Krw+gtjxg4xzQfScuwgBtXf
                            KPyHJ0RVMukttE+BEKNzjwIwHW7yJad8/+oSQf6hDo/JtMcdCvUk/gvzczJX7dDUpOGIxEmLmnCg
                            H2bY+I2qhZCt
                            -----END CERTIFICATE-----
                            """.trimmingCharacters(in: .whitespacesAndNewlines)
                    ),
                    metadata: TrustMetadata(
                        displayName: "David's Identity Verifier",
                        displayIcon: nil,
                        displayIconUrl: "https://www.multipaz.org/multipaz-logo-200x200.png",
                        privacyPolicyUrl: "https://apps.multipaz.org",
                        disclaimer: nil,
                        testOnly: true,
                        extensions: [:]
                    )
                )
            try! await readerTrustManager.addX509Cert(
                    certificate: X509Cert.companion.fromPem(
                        pemEncoding: """
                            -----BEGIN CERTIFICATE-----
                            MIICaTCCAe+gAwIBAgIQtzUvFDCKLUBWQAZ4UnCw5zAKBggqhkjOPQQDAzA3MQswCQYDVQQGDAJV
                            UzEoMCYGA1UEAwwfdmVyaWZpZXIubXVsdGlwYXoub3JnIFJlYWRlciBDQTAeFw0yNTA2MTkyMjE2
                            MzJaFw0zMDA2MTkyMjE2MzJaMDcxCzAJBgNVBAYMAlVTMSgwJgYDVQQDDB92ZXJpZmllci5tdWx0
                            aXBhei5vcmcgUmVhZGVyIENBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEa6oCzC8rfHfwVOmQf83W
                            yHEQFE8HrLK+NxsufJDrSFgMXjhRvPt3fIjlMyRAaf94Y25Ux9tXg+28EzzB/xG7q8P/FQ9nOSJk
                            w4cQJVdD/ufN599uVdfp1URdG95Vncuoo4G/MIG8MA4GA1UdDwEB/wQEAwIBBjASBgNVHRMBAf8E
                            CDAGAQH/AgEAMFYGA1UdHwRPME0wS6BJoEeGRWh0dHBzOi8vZ2l0aHViLmNvbS9vcGVud2FsbGV0
                            LWZvdW5kYXRpb24tbGFicy9pZGVudGl0eS1jcmVkZW50aWFsL2NybDAdBgNVHQ4EFgQUsYQ5hS9K
                            buq/6mKtvFHQgfdIhykwHwYDVR0jBBgwFoAUsYQ5hS9Kbuq/6mKtvFHQgfdIhykwCgYIKoZIzj0E
                            AwMDaAAwZQIwKh87sK/cMbzuc9PFvyiSRedr2RoP0fuFK0X8ddOpi6hEMOapHL/Gs/QByROCpDpk
                            AjEA2yLSJDZEu1GI8uChAsDBZwJPtv5KHUjq1Vpok69SNn+zzb1mNpqmiey+tchPBjZm
                            -----END CERTIFICATE-----
                            """.trimmingCharacters(in: .whitespacesAndNewlines)
                    ),
                    metadata: TrustMetadata(
                        displayName: "Multipaz Identity Verifier",
                        displayIcon: nil,
                        displayIconUrl: "https://www.multipaz.org/multipaz-logo-200x200.png",
                        privacyPolicyUrl: "https://apps.multipaz.org",
                        disclaimer: nil,
                        testOnly: true,
                        extensions: [:]
                    )
                )
            print("Added reader certificates to readerTrustManager")
        }

        self.provisioningModel = ProvisioningModel.companion.create(
            documentStore: documentStore,
            secureArea: secureArea,
            httpClient: HttpClient(engineFactory: Darwin()) { config in
                config.followRedirects = false
            },
            promptModel: Platform.shared.promptModel,
            documentMetadataInitializer: { documentMetadata, credentialDisplay, issuerDisplay in
                print("Setting metadata from \(credentialDisplay) and \(issuerDisplay)")
                try! await documentMetadata.setMetadata(
                    displayName: credentialDisplay.text,
                    typeDisplayName: credentialDisplay.text, // TODO: doctype instead
                    cardArt: credentialDisplay.logo,
                    issuerLogo: issuerDisplay.logo,
                    other: nil
                )
            }
        )
        
        if (try! await documentStore.listDocuments().isEmpty) {
            await self.addSelfsignedDocument()
        }
        
        let dcApi = DigitalCredentialsCompanion.shared.Default
        if (dcApi.available) {
            print("DC API available")
            try! await dcApi.startExportingCredentials(
                documentStore: documentStore,
                documentTypeRepository: documentTypeRepository
            )
        }
        
        documentModel = DocumentModel(documentTypeRepository: documentTypeRepository)
        await documentModel.setDocumentStore(documentStore: documentStore)

        startWatchingPresentmentModel()

        Task {
            for await state in provisioningModel.state {
                provisioningState = state
            }
        }
        
        isLoading = false
    }
    
    private func getIsRunningOnSimulator() -> Bool {
#if targetEnvironment(simulator)
        return true
#else
        return false
#endif
    }

    func addSelfsignedDocument() async {
        let now = Date.now
        let signedAt = now
        let validFrom = now
        let validUntil = Calendar.current.date(byAdding: .year, value: 1, to: validFrom)!
        print("validFrom: \(validFrom)")
        print("validUntil: \(validUntil)")
        let iacaKey = try! await Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
        let iacaCert = try! await MdocUtil.shared.generateIacaCertificate(
            iacaKey: AsymmetricKey.AnonymousExplicit(privateKey: iacaKey, algorithm: Algorithm.esp256),
            subject: X500Name.companion.fromName(name: "CN=Test IACA Key"),
            serial: ASN1Integer.companion.fromRandom(numBits: 128, random: KotlinRandom.companion),
            validFrom: validFrom.toKotlinInstant(),
            validUntil: validUntil.toKotlinInstant(),
            issuerAltNameUrl: "https://issuer.example.com",
            crlUrl: "https://issuer.example.com/crl"
        )
        print("foo")
        let dsKey = try! await Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
        let dsCert = try! await MdocUtil.shared.generateDsCertificate(
            iacaKey: AsymmetricKey.X509CertifiedExplicit(
                certChain: X509CertChain(certificates: [iacaCert]),
                privateKey: dsKey,
                algorithm: Algorithm.esp256
            ),
            dsKey: dsKey.publicKey,
            subject: X500Name.companion.fromName(name: "CN=Test DS Key"),
            serial:  ASN1Integer.companion.fromRandom(numBits: 128, random: KotlinRandom.companion),
            validFrom: validFrom.toKotlinInstant(),
            validUntil: validUntil.toKotlinInstant(),
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
                userAuthenticationRequired: getIsRunningOnSimulator() ? false : true,
                userAuthenticationTimeout: 0,
                validFrom: nil,
                validUntil: nil
            ),
            dsKey: AsymmetricKey.X509CertifiedExplicit(
                certChain: X509CertChain(certificates: [dsCert]),
                privateKey: dsKey,
                algorithm: Algorithm.esp256
            ),
            signedAt: signedAt.toKotlinInstant().truncateToWholeSeconds(),
            validFrom: validFrom.toKotlinInstant().truncateToWholeSeconds(),
            validUntil: validUntil.toKotlinInstant().truncateToWholeSeconds(),
            expectedUpdate: nil,
            domain: "mdoc",
            randomProvider: KotlinRandom.companion
        )
        print("Provisioned self-signed document")
    }
    
    var qrCode: UIImage? = nil
    
    private func startWatchingPresentmentModel() {
        Task {
            for await state in presentmentModel.state {
                print("state: \(state)")
                switch (state) {
                case .idle:
                    print("x")
                case .connecting:
                    print("x")
                    path.append(Destination.showQrView)
                case .waitingForSource:
                    let source = SimplePresentmentSource(
                        documentStore: documentStore,
                        documentTypeRepository: documentTypeRepository,
                        readerTrustManager: readerTrustManager,
                        zkSystemRepository: nil, // TODO
                        skipConsentPrompt: true,
                        dynamicMetadataResolver: { requester in nil },
                        preferSignatureToKeyAgreement: false,
                        domainMdocSignature: "mdoc",
                        domainMdocKeyAgreement: nil,
                        domainKeylessSdJwt: nil,
                        domainKeyBoundSdJwt: nil
                    )
                    presentmentModel.setSource(source: source)
                case .processing:
                    path.removeLast()
                    path.append(Destination.transferView)
                case .waitingForConsent:
                    print("x")
                case .completed:
                    print("x")
                }
                
            }
        }
    }

    func startPresentment() async {
        presentmentModel.reset()
        presentmentModel.setConnecting()
        let connectionMethods = [
            MdocConnectionMethodBle(
                supportsPeripheralServerMode: false,
                supportsCentralClientMode: true,
                peripheralServerModeUuid: nil,
                centralClientModeUuid: UUID.companion.randomUUID(random: KotlinRandom.companion),
                peripheralServerModePsm: nil,
                peripheralServerModeMacAddress: nil)
        ]
        let eDeviceKey = try! await Crypto.shared.createEcPrivateKey(curve: EcCurve.p256)
        let advertisedTransports = try! await ConnectionHelperKt.advertise(
            connectionMethods,
            role: MdocRole.mdoc,
            transportFactory: MdocTransportFactoryDefault.shared,
            options: MdocTransportOptions(bleUseL2CAP: false, bleUseL2CAPInEngagement: true)
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
            coroutineScope: presentmentModel.presentmentScope
        )
        presentmentModel.setMechanism(
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


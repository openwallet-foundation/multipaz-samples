//
//  ContentView.swift
//  MpzSwiftWallet
//
//  Created by David Zeuthen on 6/7/25.
//

import SwiftUI
import IdentityDocumentServices
import IdentityDocumentServicesUI

import Multipaz
import MultipazSwift

struct ContentView: View {
    @State private var viewModel = ViewModel()
    
    @State private var qrCode: UIImage? = nil
    
    var body: some View {
        
        NavigationStack(path: $viewModel.path) {
            VStack {
                if (viewModel.isLoading) {
                    VStack {
                        ProgressView()
                    }
                } else {
                    StartView()
                }
            }
            .navigationDestination(for: Destination.self) { destination in
                switch destination {
                case .startView: StartView()
                case .showQrView: ShowQrView()
                case .transferView: TransferView()
                }
            }
        }
        .environment(viewModel)
        .onAppear { Task { await viewModel.load() } }
    }

    /*
    private func handleIdle() -> some View {
        return VStack {
            Text("Number of documents: \(documentModel.documentInfos.count)")
            Button(action: {
                Task {
                    for docId in try! await viewModel.documentStore.listDocuments() {
                        try! await viewModel.documentStore.deleteDocument(identifier: docId)
                    }
                }
            }) {
                Text("Delete all documents")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)

            Button(action: {
                Task {
                    await viewModel.addSelfsignedDocument()
                }
            }) {
                Text("Add document")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)

            Button(action: {
                Task {
                    viewModel.presentmentModel.reset()
                    viewModel.presentmentModel.setConnecting()
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
                        coroutineScope: viewModel.presentmentModel.presentmentScope
                    )
                    viewModel.presentmentModel.setMechanism(
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
    }

    private func handleConnecting() -> some View {
        return VStack {
            Text("Present QR code to reader")
            if (qrCode != nil) {
                Image(uiImage: qrCode!)
            }
            Button(action: {
                viewModel.presentmentModel.reset()
            }) {
                Text("Cancel")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        }
    }

    private func handleWaitingForSource() -> some View {
        viewModel.presentmentModel.setSource(source: SimplePresentmentSource(
            documentStore: viewModel.documentStore,
            documentTypeRepository: viewModel.documentTypeRepository,
            readerTrustManager: viewModel.readerTrustManager,
            zkSystemRepository: nil,
            skipConsentPrompt: false,
            dynamicMetadataResolver: { requester in
                nil
            },
            preferSignatureToKeyAgreement: false,
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
    
    @State private var presentSheet = true
    
    private func handleWaitingForConsent() -> some View {
        return VStack {
            let consentData = viewModel.presentmentModel.consentData
            VStack {}
                .sheet(isPresented: $presentSheet) {
                    NavigationStack {
                        Consent(
                            credentialPresentmentData: consentData.credentialPresentmentData,
                            requester: consentData.requester,
                            trustPoint: consentData.trustPoint,
                            onConfirm: {
                                let selection = consentData.credentialPresentmentData.select(preselectedDocuments: [])
                                viewModel.presentmentModel.consentObtained(selection: selection)
                            }
                        )
                        .navigationTitle("Multipaz Wallet")
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .automatic) {
                                Button(action: {
                                    Task {
                                        viewModel.presentmentModel.reset()
                                    }
                                }) {
                                    Image(systemName: "xmark")
                                }
                            }
                        }
                    }
                    // TODO: would be nice to have this automatically adjust size
                    .presentationDetents([.fraction(0.8)])
                }
        }
    }
    
    private func handleCompleted() -> some View {
        Task {
            try! await delay(timeMillis: 2500)
            viewModel.presentmentModel.reset()
        }
        if (viewModel.presentmentModel.error == nil) {
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
     */
}

struct StartView: View {
    @Environment(ViewModel.self) private var viewModel
    
    var body: some View {
        VStack {
            Text("Number of documents: \(viewModel.documentModel.documentInfos.count)")
            Button(action: {
                Task {
                    for docId in try! await viewModel.documentStore.listDocuments() {
                        try! await viewModel.documentStore.deleteDocument(identifier: docId)
                    }
                }
            }) {
                Text("Delete all documents")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
            
            Button(action: {
                Task {
                    await viewModel.addSelfsignedDocument()
                }
            }) {
                Text("Add document")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)

            Button(action: {
                Task {
                    await viewModel.startPresentment()
                }
            }) {
                Text("Present using QR code")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        }
    }
}

struct ShowQrView: View {
    @Environment(ViewModel.self) private var viewModel

    var body: some View {
        VStack {
            Text("Present QR code to reader")
            if (viewModel.qrCode != nil) {
                Image(uiImage: viewModel.qrCode!)
            }
            Button(action: {
                print("TODO: cancel")
            }) {
                Text("Cancel")
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
        }
        .onDisappear {
            print("QR vanished!")
        }
    }
}

struct TransferView: View {
    @Environment(ViewModel.self) private var viewModel

    var body: some View {
        VStack {
            Text("Transfer")
                .font(.largeTitle)
        }
        .onDisappear {
            print("Transfer vanished!")
        }
    }
}


#Preview {
    ContentView()
}

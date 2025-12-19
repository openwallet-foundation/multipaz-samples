//
//  DocumentProviderExtension.swift
//  DocumentProviderExtension
//
//  Created by David Zeuthen on 12/19/25.
//

import ExtensionKit
import IdentityDocumentServicesUI
import SwiftUI
import Multipaz
import MultipazSwift

func getPresentmentSource() async -> PresentmentSource {
    let storage = IosStorage(
        storageFileUrl: FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: "group.org.multipaz.samples.MpzSwiftWallet.sharedgroup")!
            .appendingPathComponent("storage.db"),
        excludeFromBackup: true
    )
    let secureArea = try! await Platform.shared.getSecureArea(storage: storage)
    let secureAreaRepository = SecureAreaRepository.Builder()
        .add(secureArea: secureArea)
        .build()
    let documentTypeRepository = DocumentTypeRepository()
    documentTypeRepository.addDocumentType(documentType: DrivingLicense.shared.getDocumentType())
    let documentStore = DocumentStore.Builder(
        storage: storage,
        secureAreaRepository: secureAreaRepository
    ).build()
    let readerTrustManager = TrustManagerLocal(storage: storage, identifier: "default", partitionId: "default_default")
    return SimplePresentmentSource.companion.create(
        documentStore: documentStore,
        documentTypeRepository: documentTypeRepository,
        zkSystemRepository: nil,
        resolveTrustFn: { requester in
            if let certChain = requester.certChain {
                let result = try! await readerTrustManager.verify(
                    chain: certChain.certificates,
                    atTime: KotlinClockCompanion().getSystem().now()
                )
                if result.isTrusted {
                    return result.trustPoints.first?.metadata
                }
            }
            return nil
        },
        showConsentPromptFn: { requester, trustMetadata, credentialPresentmentData, preselectedDocuments, onDocumentsInFocus in
            try! await promptModelSilentConsent(
                requester: requester,
                trustMetadata: trustMetadata,
                credentialPresentmentData: credentialPresentmentData,
                preselectedDocuments: preselectedDocuments,
                onDocumentsInFocus: { documents in onDocumentsInFocus(documents) }
            )
        },
        preferSignatureToKeyAgreement: false,
        domainMdocSignature: "mdoc"
    )
}

@main
struct DocumentProviderExtension: IdentityDocumentProvider {

    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            RequestAuthorizationView(
                requestContext: context,
                getPresentmentSource: {
                    return await getPresentmentSource()
                }
            )
        }
    }

    func performRegistrationUpdates() async {
        
    }

}

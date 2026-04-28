import ExtensionKit
import IdentityDocumentServices
import IdentityDocumentServicesUI
import SwiftUI
import Foundation
@preconcurrency import ComposeApp

private struct RequestAuthorizationThinView: View {
    let requestContext: ISO18013MobileDocumentRequestContext

    @State private var isProcessing = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 16) {
            Text("Share document")
                .font(.headline)

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
            }

            HStack(spacing: 12) {
                Button("Cancel") {
                    requestContext.cancel()
                }
                .buttonStyle(.bordered)
                .disabled(isProcessing)

                Button(isProcessing ? "Sharing..." : "Share") {
                    Task {
                        await share()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isProcessing)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.secondarySystemBackground))
    }

    private func share() async {
        isProcessing = true
        defer { isProcessing = false }

        do {
            NSLog("IdentityDocumentProvider: share started")
            try await requestContext.sendResponse { rawRequest in
                NSLog("IdentityDocumentProvider: sendResponse callback entered")
                NSLog("IdentityDocumentProvider: raw request bytes = \(rawRequest.requestData.count)")

                guard let requestString = String(data: rawRequest.requestData, encoding: .utf8) else {
                    NSLog("IdentityDocumentProvider: request payload decode failed")
                    throw NSError(
                        domain: "IdentityDocumentProvider",
                        code: 1001,
                        userInfo: [NSLocalizedDescriptionKey: "Unable to decode request payload"]
                    )
                }

                NSLog("IdentityDocumentProvider: request payload decoded, chars = \(requestString.count)")

                let origin = requestContext.requestingWebsiteOrigin?.getOrigin()
                    ?? requestContext.requestingWebsiteOrigin?.absoluteString
                    ?? ""
                NSLog("IdentityDocumentProvider: origin = \(origin)")

                let responseString = try await IosDocumentProviderBridgeKt.ProcessIosDocumentRequest(
                    requestData: requestString,
                    origin: origin
                )
                NSLog("IdentityDocumentProvider: Kotlin response chars = \(responseString.count)")

                guard
                    let responseJson = try JSONSerialization.jsonObject(with: Data(responseString.utf8)) as? [String: Any],
                    let responseData = responseJson["data"] as? [String: Any],
                    let responseBase64Url = responseData["response"] as? String,
                    let response = Data(base64URLEncoded: responseBase64Url)
                else {
                    NSLog("IdentityDocumentProvider: response parse/build failed")
                    throw NSError(
                        domain: "IdentityDocumentProvider",
                        code: 1002,
                        userInfo: [NSLocalizedDescriptionKey: "Unable to build mobile document response"]
                    )
                }

                NSLog("IdentityDocumentProvider: final response bytes = \(response.count)")
                return ISO18013MobileDocumentResponse(responseData: response)
            }
            NSLog("IdentityDocumentProvider: sendResponse completed")
        } catch {
            NSLog("IdentityDocumentProvider share failed: \(error)")
            errorMessage = "Failed to share document: \(error.localizedDescription)"
        }
    }
}

@main
struct DocumentProviderExtension: IdentityDocumentProvider {
    var body: some IdentityDocumentRequestScene {
        ISO18013MobileDocumentRequestScene { context in
            RequestAuthorizationThinView(requestContext: context)
        }
    }

    func performRegistrationUpdates() async {
        do {
            try await IosDocumentProviderBridgeKt.UpdateIosDocumentProviderRegistrations()
        } catch {
            // Keep extension alive even if background registration refresh fails.
        }
    }
}

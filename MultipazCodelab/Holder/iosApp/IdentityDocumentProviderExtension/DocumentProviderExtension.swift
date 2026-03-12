import ExtensionKit
import IdentityDocumentServices
import IdentityDocumentServicesUI
import SwiftUI
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
            try await requestContext.sendResponse { rawRequest in
                let requestString = String(data: rawRequest.requestData, encoding: .utf8) ?? "{}"
                let responseString = try await IosDocumentProviderBridgeKt.ProcessIosDocumentRequest(
                    requestData: requestString,
                    origin: requestContext.requestingWebsiteOrigin?.absoluteString
                )

                let responseJson = try JSONSerialization.jsonObject(
                    with: Data(responseString.utf8)
                ) as? [String: Any]
                let responseData = responseJson?["data"] as? [String: Any]
                let responseBase64Url = responseData?["response"] as? String ?? ""
                let response = Data(base64URLEncoded: responseBase64Url) ?? Data()

                return ISO18013MobileDocumentResponse(responseData: response)
            }
        } catch {
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

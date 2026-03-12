import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea()
                .onOpenURL(perform: { url in
                    let absoluteUrl = url.absoluteString
                    if MainViewControllerKt.IsIosUriSchemePresentmentUrl(url: absoluteUrl) {
                        Task {
                            do {
                                let redirectUri = try await MainViewControllerKt.ProcessIosUriSchemeRequest(
                                    requestUrl: absoluteUrl
                                )
                                if let redirectUrl = URL(string: redirectUri) {
                                    await MainActor.run {
                                        UIApplication.shared.open(redirectUrl)
                                    }
                                }
                            } catch {
                                print("Error processing OpenID4VP URI scheme request: \(error)")
                            }
                        }
                    } else {
                        MainViewControllerKt.HandleUrl(url: absoluteUrl)
                    }
                })
    }
}




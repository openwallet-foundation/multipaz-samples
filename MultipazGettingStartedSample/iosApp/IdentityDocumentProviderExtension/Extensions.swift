import Foundation

extension Data {
    init?(base64URLEncoded base64Url: String) {
        var base64 = base64Url.replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let padding = (4 - (base64.count % 4)) % 4
        if padding > 0 {
            base64 += String(repeating: "=", count: padding)
        }
        self.init(base64Encoded: base64)
    }
}

extension URL {
    func getOrigin() -> String {
        guard let scheme, let host else {
            return absoluteString
        }
        var origin = "\(scheme)://\(host)"
        if let port,
           (scheme == "http" && port != 80) || (scheme == "https" && port != 443) {
            origin += ":\(port)"
        }
        return origin
    }
}

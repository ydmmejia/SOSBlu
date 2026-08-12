import Foundation
import CryptoKit

/// CryptoManager handles Ed25519 asymmetric signing and keypair management using Apple CryptoKit
public class CryptoManager {
    public static let shared = CryptoManager()

    private let keyTag = "com.sosblu.ios.ed25519.privatekey"
    public private(set) var privateKey: Curve25519.Signing.PrivateKey
    public var publicKey: Curve25519.Signing.PublicKey { privateKey.publicKey }

    public var deviceIdData: Data {
        return Data(publicKey.rawRepresentation.prefix(8))
    }

    public var deviceIdHex: String {
        return deviceIdData.map { String(format: "%02hhx", $0) }.joined()
    }

    private init() {
        if let storedData = UserDefaults.standard.data(forKey: keyTag),
           let key = try? Curve25519.Signing.PrivateKey(rawRepresentation: storedData) {
            self.privateKey = key
        } else {
            let newKey = Curve25519.Signing.PrivateKey()
            UserDefaults.standard.set(newKey.rawRepresentation, forKey: keyTag)
            self.privateKey = newKey
        }
    }

    /// Sign data using Ed25519 private key
    public func sign(_ data: Data) -> Data? {
        do {
            let signature = try privateKey.signature(for: data)
            return signature
        } catch {
            print("CryptoManager Error: Failed to sign data: \(error)")
            return nil
        }
    }

    /// Verify Ed25519 signature
    public static func verify(signature: Data, for data: Data, publicKeyData: Data) -> Bool {
        guard let pubKey = try? Curve25519.Signing.PublicKey(rawRepresentation: publicKeyData) else {
            return false
        }
        return pubKey.isValidSignature(signature, for: data)
    }
}

import Foundation

protocol BarcodeScanner {
    func scan() async throws -> String
}

final class FakeBarcodeScanner: BarcodeScanner {
    var barcodeToReturn = "0029877030712"
    var delay: Duration = .milliseconds(300)
    var shouldFail = false

    func scan() async throws -> String {
        try await Task.sleep(for: delay)
        if shouldFail {
            throw ScannerError.cameraUnavailable
        }
        return barcodeToReturn
    }
}

enum ScannerError: LocalizedError {
    case cameraUnavailable
    case scanCancelled

    var errorDescription: String? {
        switch self {
        case .cameraUnavailable: "Camera is not available"
        case .scanCancelled: "Scan was cancelled"
        }
    }
}

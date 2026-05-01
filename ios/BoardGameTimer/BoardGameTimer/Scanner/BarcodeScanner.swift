import Foundation
import VisionKit
import SwiftUI

// MARK: - Protocol

protocol BarcodeScanner {
    func scan() async throws -> String
}

// MARK: - VisionKit Implementation

@available(iOS 16.0, *)
@MainActor
final class VisionKitBarcodeScanner: BarcodeScanner {
    private var continuation: CheckedContinuation<String, Error>?
    private var isScanning = false

    func scan() async throws -> String {
        guard DataScannerViewController.isSupported else {
            throw ScannerError.cameraUnavailable
        }

        guard !isScanning else {
            throw ScannerError.scanInProgress
        }

        isScanning = true
        defer { isScanning = false }

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
        }
    }

    func didScan(barcode: String) {
        continuation?.resume(returning: barcode)
        continuation = nil
    }

    func didCancel() {
        continuation?.resume(throwing: ScannerError.scanCancelled)
        continuation = nil
    }

    func didFail(with error: Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }
}

// MARK: - SwiftUI Wrapper

@available(iOS 16.0, *)
struct BarcodeScannerView: UIViewControllerRepresentable {
    let scanner: VisionKitBarcodeScanner

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scannerVC = DataScannerViewController(
            recognizedDataTypes: [
                .barcode(symbologies: [
                    .ean8,
                    .ean13,
                    .upce,
                    .code128,
                    .code39
                ])
            ],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true
        )

        scannerVC.delegate = context.coordinator
        return scannerVC
    }

    func updateUIViewController(_ uiViewController: DataScannerViewController, context: Context) {
        if !uiViewController.isScanning {
            try? uiViewController.startScanning()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(scanner: scanner)
    }

    class Coordinator: NSObject, DataScannerViewControllerDelegate {
        let scanner: VisionKitBarcodeScanner

        init(scanner: VisionKitBarcodeScanner) {
            self.scanner = scanner
        }

        func dataScanner(_ dataScanner: DataScannerViewController, didTapOn item: RecognizedItem) {
            switch item {
            case .barcode(let barcode):
                if let payloadString = barcode.payloadStringValue {
                    scanner.didScan(barcode: payloadString)
                    dataScanner.stopScanning()
                }
            default:
                break
            }
        }

        func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
            // Auto-scan first detected barcode
            guard let firstItem = addedItems.first else { return }

            switch firstItem {
            case .barcode(let barcode):
                if let payloadString = barcode.payloadStringValue {
                    scanner.didScan(barcode: payloadString)
                    dataScanner.stopScanning()
                }
            default:
                break
            }
        }

        func dataScannerDidCancel(_ dataScanner: DataScannerViewController) {
            scanner.didCancel()
        }

        func dataScanner(_ dataScanner: DataScannerViewController, didFailWithError error: Error) {
            scanner.didFail(with: error)
        }
    }
}

// MARK: - Fake Implementation

final class FakeBarcodeScanner: BarcodeScanner {
    var barcodeToReturn = "0029877030712"
    var delay: Duration = .milliseconds(300)
    var shouldFail = false
    var errorToThrow: ScannerError = .cameraUnavailable

    func scan() async throws -> String {
        try await Task.sleep(for: delay)
        if shouldFail {
            throw errorToThrow
        }
        return barcodeToReturn
    }
}

// MARK: - Errors

enum ScannerError: LocalizedError, Equatable {
    case cameraUnavailable
    case scanCancelled
    case scanInProgress
    case invalidBarcode

    var errorDescription: String? {
        switch self {
        case .cameraUnavailable:
            "Camera is not available on this device"
        case .scanCancelled:
            "Scan was cancelled"
        case .scanInProgress:
            "A scan is already in progress"
        case .invalidBarcode:
            "The barcode format is not supported"
        }
    }
}

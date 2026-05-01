import XCTest
@testable import BoardGameTimer

@MainActor
final class BarcodeScannerTests: XCTestCase {

    // MARK: - FakeBarcodeScanner Tests

    func testFakeScannerReturnsConfiguredBarcode() async throws {
        let scanner = FakeBarcodeScanner()
        scanner.barcodeToReturn = "1234567890123"

        let result = try await scanner.scan()

        XCTAssertEqual(result, "1234567890123")
    }

    func testFakeScannerThrowsWhenConfiguredToFail() async {
        let scanner = FakeBarcodeScanner()
        scanner.shouldFail = true

        do {
            _ = try await scanner.scan()
            XCTFail("Expected scanner to throw")
        } catch let error as ScannerError {
            XCTAssertEqual(error, .cameraUnavailable)
        } catch {
            XCTFail("Expected ScannerError, got \(error)")
        }
    }

    func testFakeScannerThrowsSpecificError() async {
        let scanner = FakeBarcodeScanner()
        scanner.shouldFail = true
        scanner.errorToThrow = .scanCancelled

        do {
            _ = try await scanner.scan()
            XCTFail("Expected scanner to throw")
        } catch let error as ScannerError {
            XCTAssertEqual(error, .scanCancelled)
        } catch {
            XCTFail("Expected ScannerError, got \(error)")
        }
    }

    func testFakeScannerRespectsDelay() async throws {
        let scanner = FakeBarcodeScanner()
        scanner.delay = .milliseconds(100)

        let start = ContinuousClock.now
        _ = try await scanner.scan()
        let elapsed = ContinuousClock.now - start

        XCTAssertGreaterThanOrEqual(elapsed, .milliseconds(100))
    }

    func testFakeScannerCanBeReused() async throws {
        let scanner = FakeBarcodeScanner()
        scanner.barcodeToReturn = "FIRST"

        let first = try await scanner.scan()
        XCTAssertEqual(first, "FIRST")

        scanner.barcodeToReturn = "SECOND"
        let second = try await scanner.scan()
        XCTAssertEqual(second, "SECOND")
    }

    // MARK: - VisionKitBarcodeScanner Tests

    @available(iOS 16.0, *)
    func testVisionKitScannerAcceptsBarcodeResult() async throws {
        let scanner = VisionKitBarcodeScanner()

        Task {
            try await Task.sleep(for: .milliseconds(50))
            scanner.didScan(barcode: "0029877030712")
        }

        let result = try await scanner.scan()
        XCTAssertEqual(result, "0029877030712")
    }

    @available(iOS 16.0, *)
    func testVisionKitScannerHandlesCancellation() async {
        let scanner = VisionKitBarcodeScanner()

        Task {
            try await Task.sleep(for: .milliseconds(50))
            scanner.didCancel()
        }

        do {
            _ = try await scanner.scan()
            XCTFail("Expected scanner to throw")
        } catch let error as ScannerError {
            XCTAssertEqual(error, .scanCancelled)
        } catch {
            XCTFail("Expected ScannerError, got \(error)")
        }
    }

    @available(iOS 16.0, *)
    func testVisionKitScannerHandlesFailure() async {
        let scanner = VisionKitBarcodeScanner()

        Task {
            try await Task.sleep(for: .milliseconds(50))
            scanner.didFail(with: ScannerError.invalidBarcode)
        }

        do {
            _ = try await scanner.scan()
            XCTFail("Expected scanner to throw")
        } catch let error as ScannerError {
            XCTAssertEqual(error, .invalidBarcode)
        } catch {
            XCTFail("Expected ScannerError, got \(error)")
        }
    }

    @available(iOS 16.0, *)
    func testVisionKitScannerPreventsMultipleSimultaneousScans() async {
        let scanner = VisionKitBarcodeScanner()

        // Start first scan but don't complete it
        Task {
            _ = try? await scanner.scan()
        }

        // Wait a bit to ensure first scan is in progress
        try? await Task.sleep(for: .milliseconds(50))

        // Try to start second scan
        do {
            _ = try await scanner.scan()
            XCTFail("Expected scanner to throw scanInProgress")
        } catch let error as ScannerError {
            XCTAssertEqual(error, .scanInProgress)
        } catch {
            XCTFail("Expected ScannerError.scanInProgress, got \(error)")
        }
    }

    // MARK: - Scanner Error Tests

    func testScannerErrorDescriptions() {
        XCTAssertNotNil(ScannerError.cameraUnavailable.errorDescription)
        XCTAssertNotNil(ScannerError.scanCancelled.errorDescription)
        XCTAssertNotNil(ScannerError.scanInProgress.errorDescription)
        XCTAssertNotNil(ScannerError.invalidBarcode.errorDescription)
    }

    // MARK: - Integration Tests

    func testScanViewModelExposesVisionKitScanner() {
        if #available(iOS 16.0, *) {
            let scanner = VisionKitBarcodeScanner()
            let service = MockBoardGameService()
            let viewModel = ScanViewModel(scanner: scanner, service: service)

            XCTAssertNotNil(viewModel.visionKitScanner)
            XCTAssertTrue(viewModel.visionKitScanner === scanner)
        }
    }

    func testScanViewModelDoesNotExposeVisionKitForFakeScanner() {
        let scanner = FakeBarcodeScanner()
        let service = MockBoardGameService()
        let viewModel = ScanViewModel(scanner: scanner, service: service)

        XCTAssertNil(viewModel.visionKitScanner)
    }

    func testScanViewModelHandlesBarcodeResolution() async {
        let scanner = FakeBarcodeScanner()
        scanner.barcodeToReturn = "0029877030712"
        let service = MockBoardGameService()
        let viewModel = ScanViewModel(scanner: scanner, service: service)

        await viewModel.resolveBarcode("0029877030712")

        if case .resolved(let scan) = viewModel.state {
            XCTAssertEqual(scan.gameId, "catan")
        } else {
            XCTFail("Expected resolved state, got \(viewModel.state)")
        }
    }

    func testScanViewModelHandlesResolutionFailure() async {
        let scanner = FakeBarcodeScanner()
        let service = MockBoardGameService()
        service.shouldFailNextCall = true
        let viewModel = ScanViewModel(scanner: scanner, service: service)

        await viewModel.resolveBarcode("0029877030712")

        if case .error = viewModel.state {
            // expected
        } else {
            XCTFail("Expected error state, got \(viewModel.state)")
        }
    }
}

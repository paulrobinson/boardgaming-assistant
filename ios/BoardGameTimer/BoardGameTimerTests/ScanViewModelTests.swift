import XCTest
@testable import BoardGameTimer

@MainActor
final class ScanViewModelTests: XCTestCase {

    func testStartsInIdleState() {
        let vm = makeScanViewModel()
        XCTAssertEqual(vm.state, .idle)
    }

    func testSuccessfulScanTransitionsToResolved() async {
        let vm = makeScanViewModel()
        await vm.startScan()
        if case .resolved(let scan) = vm.state {
            XCTAssertEqual(scan.gameId, "catan")
            XCTAssertTrue(scan.supported)
        } else {
            XCTFail("Expected resolved state, got \(vm.state)")
        }
    }

    func testUnsupportedBarcodeTransitionsToUnsupported() async {
        let scanner = FakeBarcodeScanner()
        scanner.barcodeToReturn = "9999999999999"
        let vm = makeScanViewModel(scanner: scanner)

        await vm.startScan()

        XCTAssertEqual(vm.state, .unsupported)
    }

    func testScannerFailureTransitionsToError() async {
        let scanner = FakeBarcodeScanner()
        scanner.shouldFail = true
        let vm = makeScanViewModel(scanner: scanner)

        await vm.startScan()

        if case .error = vm.state {
            // expected
        } else {
            XCTFail("Expected error state, got \(vm.state)")
        }
    }

    func testServiceFailureTransitionsToError() async {
        let service = MockBoardGameService()
        service.shouldFailNextCall = true
        let vm = makeScanViewModel(service: service)

        await vm.startScan()

        if case .error = vm.state {
            // expected
        } else {
            XCTFail("Expected error state, got \(vm.state)")
        }
    }

    func testResetGoesBackToIdle() async {
        let vm = makeScanViewModel()
        await vm.startScan()
        vm.reset()
        XCTAssertEqual(vm.state, .idle)
    }

    // --- helpers ---

    private func makeScanViewModel(
        scanner: BarcodeScanner? = nil,
        service: BoardGameService? = nil
    ) -> ScanViewModel {
        let s = scanner ?? FakeBarcodeScanner()
        let svc = service ?? MockBoardGameService()
        return ScanViewModel(scanner: s, service: svc)
    }
}

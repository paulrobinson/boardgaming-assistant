import Foundation

@MainActor
final class ScanViewModel: ObservableObject {
    enum State: Equatable {
        case idle
        case scanning
        case resolving
        case resolved(ScanResponse)
        case unsupported
        case error(String)
    }

    @Published var state: State = .idle

    private let scanner: BarcodeScanner
    private let service: BoardGameService

    init(scanner: BarcodeScanner, service: BoardGameService) {
        self.scanner = scanner
        self.service = service
    }

    func startScan() async {
        state = .scanning
        do {
            let barcode = try await scanner.scan()
            state = .resolving
            let response = try await service.resolveBarcode(barcode)
            if response.supported {
                state = .resolved(response)
            } else {
                state = .unsupported
            }
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func reset() {
        state = .idle
    }
}

extension ScanViewModel.State {
    static func == (lhs: ScanViewModel.State, rhs: ScanViewModel.State) -> Bool {
        switch (lhs, rhs) {
        case (.idle, .idle), (.scanning, .scanning), (.resolving, .resolving), (.unsupported, .unsupported):
            return true
        case (.error(let a), .error(let b)):
            return a == b
        case (.resolved(let a), .resolved(let b)):
            return a.gameId == b.gameId
        default:
            return false
        }
    }
}

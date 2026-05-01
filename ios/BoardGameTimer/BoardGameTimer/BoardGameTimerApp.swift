import SwiftUI

@main
struct BoardGameTimerApp: App {
    var body: some Scene {
        WindowGroup {
            AppCoordinator()
        }
    }
}

struct AppCoordinator: View {
    @State private var path = NavigationPath()

    private let service: BoardGameService = MockBoardGameService()
    private let scanner: BarcodeScanner = FakeBarcodeScanner()

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(path: $path)
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .scan:
                        ScanView(
                            viewModel: ScanViewModel(scanner: scanner, service: service),
                            path: $path)
                    case .gameConfirmation(let scan):
                        GameConfirmationView(scan: scan, path: $path)
                    case .groupProfile(let gameId):
                        GroupProfileFormView(gameId: gameId, path: $path)
                    case .estimate(let gameId, let profile):
                        EstimateResultView(
                            viewModel: EstimateViewModel(service: service),
                            gameId: gameId, profile: profile, path: $path)
                    case .feedback(let estimateId):
                        FeedbackView(
                            viewModel: FeedbackViewModel(service: service),
                            estimateId: estimateId, path: $path)
                    }
                }
        }
    }
}

enum Route: Hashable {
    case scan
    case gameConfirmation(ScanResponse)
    case groupProfile(gameId: String)
    case estimate(gameId: String, profile: GroupProfileDto)
    case feedback(estimateId: String)
}

extension ScanResponse: Hashable {
    static func == (lhs: ScanResponse, rhs: ScanResponse) -> Bool {
        lhs.gameId == rhs.gameId
    }
    func hash(into hasher: inout Hasher) {
        hasher.combine(gameId)
    }
}

extension GroupProfileDto: Hashable {
    func hash(into hasher: inout Hasher) {
        hasher.combine(playerCount)
        hasher.combine(groupFamiliarity)
        hasher.combine(turnPace)
        hasher.combine(analysisStyle)
        hasher.combine(childrenIncluded)
    }
}

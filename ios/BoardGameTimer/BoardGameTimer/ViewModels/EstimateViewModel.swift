import Foundation

@MainActor
final class EstimateViewModel: ObservableObject {
    enum State: Equatable {
        case idle
        case loading
        case loaded(EstimateResponse)
        case error(String)
    }

    @Published var state: State = .idle

    private let service: BoardGameService

    init(service: BoardGameService) {
        self.service = service
    }

    func createEstimate(gameId: String, profile: GroupProfileDto) async {
        state = .loading
        do {
            let request = EstimateRequest(gameId: gameId, groupProfile: profile)
            let response = try await service.createEstimate(request)
            state = .loaded(response)
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func reset() {
        state = .idle
    }
}

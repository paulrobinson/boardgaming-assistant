import Foundation

@MainActor
final class FeedbackViewModel: ObservableObject {
    enum State: Equatable {
        case editing
        case submitting
        case submitted
        case error(String)
    }

    @Published var state: State = .editing
    @Published var actualTeachMinutes: Int = 15
    @Published var actualPlayMinutes: Int = 60
    @Published var notes: String = ""

    private let service: BoardGameService

    init(service: BoardGameService) {
        self.service = service
    }

    func submit(estimateId: String) async {
        state = .submitting
        do {
            let request = FeedbackRequest(
                estimateId: estimateId,
                actualTeachMinutes: actualTeachMinutes,
                actualPlayMinutes: actualPlayMinutes,
                notes: notes.isEmpty ? nil : notes)
            _ = try await service.submitFeedback(request)
            state = .submitted
        } catch {
            state = .error(error.localizedDescription)
        }
    }
}

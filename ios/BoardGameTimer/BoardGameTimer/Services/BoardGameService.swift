import Foundation

protocol BoardGameService {
    func resolveBarcode(_ barcode: String) async throws -> ScanResponse
    func createEstimate(_ request: EstimateRequest) async throws -> EstimateResponse
    func submitFeedback(_ request: FeedbackRequest) async throws -> FeedbackResponse
}

final class MockBoardGameService: BoardGameService {
    var scanDelay: Duration = .milliseconds(500)
    var estimateDelay: Duration = .milliseconds(800)
    var shouldFailNextCall = false

    func resolveBarcode(_ barcode: String) async throws -> ScanResponse {
        try await Task.sleep(for: scanDelay)
        if shouldFailNextCall {
            shouldFailNextCall = false
            throw ServiceError.networkError("Connection failed")
        }
        if barcode == "0029877030712" {
            return ScanResponse(
                gameId: "catan", name: "Catan",
                officialPlayTimeMinutes: 60, minPlayers: 3, maxPlayers: 4,
                supported: true)
        }
        if barcode == "0824968717912" {
            return ScanResponse(
                gameId: "ticket-to-ride", name: "Ticket to Ride",
                officialPlayTimeMinutes: 45, minPlayers: 2, maxPlayers: 5,
                supported: true)
        }
        return ScanResponse(
            gameId: nil, name: nil,
            officialPlayTimeMinutes: 0, minPlayers: 0, maxPlayers: 0,
            supported: false)
    }

    func createEstimate(_ request: EstimateRequest) async throws -> EstimateResponse {
        try await Task.sleep(for: estimateDelay)
        if shouldFailNextCall {
            shouldFailNextCall = false
            throw ServiceError.networkError("Connection failed")
        }
        return EstimateResponse(
            estimateId: "est_mock001",
            teachMinutes: 24,
            playMinutes: 66,
            totalMinutes: 90,
            confidence: "medium",
            playerCountFit: [
                PlayerCountFitDto(playerCount: 3, fit: "good"),
                PlayerCountFitDto(playerCount: 4, fit: "best")
            ],
            explanation: "Catan plays best at 4 players. Mixed familiarity means some rules review, adding 24 minutes.",
            riskNotes: ["Rules reminders may increase downtime"]
        )
    }

    func submitFeedback(_ request: FeedbackRequest) async throws -> FeedbackResponse {
        try await Task.sleep(for: scanDelay)
        if shouldFailNextCall {
            shouldFailNextCall = false
            throw ServiceError.networkError("Connection failed")
        }
        return FeedbackResponse(
            feedbackId: "fb_mock001",
            estimateId: request.estimateId,
            accepted: true)
    }
}

enum ServiceError: LocalizedError {
    case networkError(String)
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .networkError(let msg): msg
        case .serverError(let msg): msg
        }
    }
}

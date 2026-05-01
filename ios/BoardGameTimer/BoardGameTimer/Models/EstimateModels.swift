import Foundation

struct EstimateRequest: Codable {
    let gameId: String
    let groupProfile: GroupProfileDto
}

struct EstimateResponse: Codable, Equatable {
    let estimateId: String
    let teachMinutes: Int
    let playMinutes: Int
    let totalMinutes: Int
    let confidence: String
    let playerCountFit: [PlayerCountFitDto]
    let explanation: String
    let riskNotes: [String]
}

struct PlayerCountFitDto: Codable, Equatable {
    let playerCount: Int
    let fit: String
}

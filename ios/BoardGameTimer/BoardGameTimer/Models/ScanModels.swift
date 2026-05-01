import Foundation

struct ScanRequest: Codable {
    let barcode: String
}

struct ScanResponse: Codable {
    let gameId: String?
    let name: String?
    let officialPlayTimeMinutes: Int
    let minPlayers: Int
    let maxPlayers: Int
    let supported: Bool
}

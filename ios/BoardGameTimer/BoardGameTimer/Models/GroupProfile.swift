import Foundation

struct GroupProfileDto: Codable, Equatable {
    var playerCount: Int
    var groupFamiliarity: String
    var turnPace: String
    var analysisStyle: String
    var childrenIncluded: Bool
    var notes: String?

    static let `default` = GroupProfileDto(
        playerCount: 3,
        groupFamiliarity: "mixed",
        turnPace: "average",
        analysisStyle: "moderate",
        childrenIncluded: false,
        notes: nil
    )
}

enum GroupFamiliarity: String, CaseIterable, Identifiable {
    case new, mixed, experienced
    var id: String { rawValue }

    var label: String {
        switch self {
        case .new: "New to this game"
        case .mixed: "Mixed experience"
        case .experienced: "Experienced"
        }
    }
}

enum TurnPace: String, CaseIterable, Identifiable {
    case fast, average, slow
    var id: String { rawValue }

    var label: String {
        switch self {
        case .fast: "Fast"
        case .average: "Average"
        case .slow: "Slow"
        }
    }
}

enum AnalysisStyle: String, CaseIterable, Identifiable {
    case low, moderate, high
    var id: String { rawValue }

    var label: String {
        switch self {
        case .low: "Low (casual)"
        case .moderate: "Moderate"
        case .high: "High (competitive)"
        }
    }
}

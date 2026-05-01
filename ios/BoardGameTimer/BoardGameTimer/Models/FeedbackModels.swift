import Foundation

struct FeedbackRequest: Codable {
    let estimateId: String
    let actualTeachMinutes: Int
    let actualPlayMinutes: Int
    let notes: String?
}

struct FeedbackResponse: Codable {
    let feedbackId: String
    let estimateId: String
    let accepted: Bool
}

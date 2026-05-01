import Foundation

struct ErrorResponse: Codable {
    let message: String

    enum CodingKeys: String, CodingKey {
        case message
        case error // Support both "message" and "error" fields
    }

    init(message: String) {
        self.message = message
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        // Try "message" first, then fall back to "error"
        if let msg = try? container.decode(String.self, forKey: .message) {
            self.message = msg
        } else if let err = try? container.decode(String.self, forKey: .error) {
            self.message = err
        } else {
            throw DecodingError.dataCorrupted(
                DecodingError.Context(
                    codingPath: decoder.codingPath,
                    debugDescription: "ErrorResponse must contain either 'message' or 'error' field"
                )
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(message, forKey: .message)
    }
}

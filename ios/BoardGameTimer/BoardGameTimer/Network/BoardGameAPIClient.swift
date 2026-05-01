import Foundation

// MARK: - API Client

final class BoardGameAPIClient: BoardGameService {
    private let transport: HTTPTransport
    private let encoder: JSONEncoder

    init(transport: HTTPTransport, encoder: JSONEncoder = JSONEncoder()) {
        self.transport = transport
        self.encoder = encoder
    }

    // MARK: - Barcode Resolution

    func resolveBarcode(_ barcode: String) async throws -> ScanResponse {
        let request = ScanRequest(barcode: barcode)

        let body = try encoder.encode(request)

        let httpRequest = HTTPRequest(
            method: .post,
            path: "/api/v1/scan/resolve",
            body: body
        )

        do {
            return try await transport.send(httpRequest)
        } catch let httpError as HTTPError {
            throw mapToServiceError(httpError)
        } catch {
            throw ServiceError.networkError(error.localizedDescription)
        }
    }

    // MARK: - Estimate Creation

    func createEstimate(_ request: EstimateRequest) async throws -> EstimateResponse {
        let body = try encoder.encode(request)

        let httpRequest = HTTPRequest(
            method: .post,
            path: "/api/v1/estimates",
            body: body
        )

        do {
            return try await transport.send(httpRequest)
        } catch let httpError as HTTPError {
            throw mapToServiceError(httpError)
        } catch {
            throw ServiceError.networkError(error.localizedDescription)
        }
    }

    // MARK: - Feedback Submission

    func submitFeedback(_ request: FeedbackRequest) async throws -> FeedbackResponse {
        let body = try encoder.encode(request)

        let httpRequest = HTTPRequest(
            method: .post,
            path: "/api/v1/feedback",
            body: body
        )

        do {
            return try await transport.send(httpRequest)
        } catch let httpError as HTTPError {
            throw mapToServiceError(httpError)
        } catch {
            throw ServiceError.networkError(error.localizedDescription)
        }
    }

    // MARK: - Error Mapping

    private func mapToServiceError(_ httpError: HTTPError) -> ServiceError {
        switch httpError {
        case .invalidURL, .invalidResponse, .decodingFailed:
            return .networkError(httpError.localizedDescription ?? "Network error")
        case .badRequest(let message), .notFound(let message):
            return .serverError(message)
        case .serverError(let message), .httpError(_, let message):
            return .serverError(message)
        case .networkError(let error):
            return .networkError(error.localizedDescription)
        }
    }
}

// MARK: - Convenience Initializer

extension BoardGameAPIClient {
    /// Create an API client with a base URL
    static func create(baseURL: String) -> BoardGameAPIClient? {
        guard let url = URL(string: baseURL) else {
            return nil
        }

        let transport = URLSessionHTTPTransport(baseURL: url)
        return BoardGameAPIClient(transport: transport)
    }
}

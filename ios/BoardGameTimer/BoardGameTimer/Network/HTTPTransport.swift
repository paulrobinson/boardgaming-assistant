import Foundation

// MARK: - HTTP Transport Protocol

protocol HTTPTransport {
    func send<T: Decodable>(_ request: HTTPRequest) async throws -> T
}

// MARK: - HTTP Request

struct HTTPRequest {
    let method: HTTPMethod
    let path: String
    let queryItems: [URLQueryItem]?
    let body: Data?
    let headers: [String: String]

    init(
        method: HTTPMethod,
        path: String,
        queryItems: [URLQueryItem]? = nil,
        body: Data? = nil,
        headers: [String: String] = [:]
    ) {
        self.method = method
        self.path = path
        self.queryItems = queryItems
        self.body = body
        self.headers = headers
    }
}

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case delete = "DELETE"
}

// MARK: - URLSession Implementation

final class URLSessionHTTPTransport: HTTPTransport {
    private let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder

    init(
        baseURL: URL,
        session: URLSession = .shared,
        decoder: JSONDecoder = JSONDecoder()
    ) {
        self.baseURL = baseURL
        self.session = session
        self.decoder = decoder
    }

    func send<T: Decodable>(_ request: HTTPRequest) async throws -> T {
        let urlRequest = try buildURLRequest(from: request)

        let (data, response) = try await session.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw HTTPError.invalidResponse
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            throw mapHTTPError(statusCode: httpResponse.statusCode, data: data)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw HTTPError.decodingFailed(error)
        }
    }

    private func buildURLRequest(from request: HTTPRequest) throws -> URLRequest {
        var components = URLComponents(url: baseURL.appendingPathComponent(request.path), resolvingAgainstBaseURL: true)
        components?.queryItems = request.queryItems

        guard let url = components?.url else {
            throw HTTPError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = request.method.rawValue
        urlRequest.httpBody = request.body

        // Default headers
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")

        // Custom headers
        for (key, value) in request.headers {
            urlRequest.setValue(value, forHTTPHeaderField: key)
        }

        return urlRequest
    }

    private func mapHTTPError(statusCode: Int, data: Data) -> HTTPError {
        // Try to decode error response
        if let errorResponse = try? decoder.decode(ErrorResponse.self, from: data) {
            switch statusCode {
            case 400: return .badRequest(errorResponse.message)
            case 404: return .notFound(errorResponse.message)
            case 500...599: return .serverError(errorResponse.message)
            default: return .httpError(statusCode, errorResponse.message)
            }
        }

        // Generic error
        switch statusCode {
        case 400: return .badRequest("Invalid request")
        case 404: return .notFound("Resource not found")
        case 500...599: return .serverError("Server error")
        default: return .httpError(statusCode, "HTTP error \(statusCode)")
        }
    }
}

// MARK: - Mock Implementation

final class MockHTTPTransport: HTTPTransport {
    var responses: [String: Any] = [:]
    var errors: [String: Error] = [:]
    var requestDelay: Duration = .milliseconds(100)

    // Recorded requests for verification
    var recordedRequests: [HTTPRequest] = []

    func send<T: Decodable>(_ request: HTTPRequest) async throws -> T {
        recordedRequests.append(request)

        try await Task.sleep(for: requestDelay)

        let key = requestKey(request)

        // Check for configured error
        if let error = errors[key] {
            throw error
        }

        // Check for configured response
        guard let response = responses[key] else {
            throw HTTPError.notFound("No mock response configured for \(key)")
        }

        // If response is already the right type, return it
        if let typedResponse = response as? T {
            return typedResponse
        }

        // If response is Data, try to decode it
        if let data = response as? Data {
            do {
                return try JSONDecoder().decode(T.self, from: data)
            } catch {
                throw HTTPError.decodingFailed(error)
            }
        }

        // If response is Encodable, encode then decode
        if let encodable = response as? Encodable {
            do {
                let data = try JSONEncoder().encode(encodable)
                return try JSONDecoder().decode(T.self, from: data)
            } catch {
                throw HTTPError.decodingFailed(error)
            }
        }

        throw HTTPError.invalidResponse
    }

    func setResponse<T: Encodable>(_ response: T, for method: HTTPMethod, path: String) {
        let key = "\(method.rawValue):\(path)"
        responses[key] = response
    }

    func setError(_ error: Error, for method: HTTPMethod, path: String) {
        let key = "\(method.rawValue):\(path)"
        errors[key] = error
    }

    func reset() {
        responses.removeAll()
        errors.removeAll()
        recordedRequests.removeAll()
    }

    private func requestKey(_ request: HTTPRequest) -> String {
        "\(request.method.rawValue):\(request.path)"
    }
}

// MARK: - Errors

enum HTTPError: LocalizedError {
    case invalidURL
    case invalidResponse
    case badRequest(String)
    case notFound(String)
    case serverError(String)
    case httpError(Int, String)
    case decodingFailed(Error)
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            "Invalid URL"
        case .invalidResponse:
            "Invalid response from server"
        case .badRequest(let message):
            "Bad request: \(message)"
        case .notFound(let message):
            "Not found: \(message)"
        case .serverError(let message):
            "Server error: \(message)"
        case .httpError(let code, let message):
            "HTTP error \(code): \(message)"
        case .decodingFailed(let error):
            "Failed to decode response: \(error.localizedDescription)"
        case .networkError(let error):
            "Network error: \(error.localizedDescription)"
        }
    }
}

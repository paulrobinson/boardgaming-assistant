# Networking Architecture

## Overview
The BoardGameTimer iOS app uses a ports-and-adapters architecture for networking, keeping the network layer behind protocols to enable testing and flexibility.

## Architecture Layers

### 1. Service Protocol Layer
**Location:** `BoardGameTimer/Services/BoardGameService.swift`

The `BoardGameService` protocol defines the domain interface:
```swift
protocol BoardGameService {
    func resolveBarcode(_ barcode: String) async throws -> ScanResponse
    func createEstimate(_ request: EstimateRequest) async throws -> EstimateResponse
    func submitFeedback(_ request: FeedbackRequest) async throws -> FeedbackResponse
}
```

**Purpose:** Domain-level abstraction that ViewModels depend on. Keeps UI layer independent of networking details.

### 2. HTTP Transport Layer
**Location:** `BoardGameTimer/Network/HTTPTransport.swift`

The `HTTPTransport` protocol abstracts HTTP communication:
```swift
protocol HTTPTransport {
    func send<T: Decodable>(_ request: HTTPRequest) async throws -> T
}
```

**Implementations:**
- **URLSessionHTTPTransport**: Production implementation using URLSession
- **MockHTTPTransport**: Test implementation with configurable responses

**Purpose:** Isolates HTTP mechanics from business logic. Makes testing possible without network calls.

### 3. API Client Layer
**Location:** `BoardGameTimer/Network/BoardGameAPIClient.swift`

The `BoardGameAPIClient` implements `BoardGameService` using `HTTPTransport`:
- Constructs API requests
- Encodes request bodies
- Decodes responses
- Maps HTTP errors to domain errors

**Purpose:** Translates domain operations to HTTP API calls.

## Request/Response Flow

### Example: Barcode Scan
```
ViewController
    ↓
ScanViewModel
    ↓
BoardGameService (protocol)
    ↓
BoardGameAPIClient
    ↓
HTTPTransport (protocol)
    ↓
URLSessionHTTPTransport
    ↓
HTTP: POST /api/v1/scan/resolve
```

## API Endpoints

### 1. POST /api/v1/scan/resolve
**Purpose:** Resolve barcode to game information

**Request:**
```json
{
  "barcode": "0029877030712"
}
```

**Response (Supported Game):**
```json
{
  "gameId": "catan",
  "name": "Catan",
  "officialPlayTimeMinutes": 60,
  "minPlayers": 3,
  "maxPlayers": 4,
  "supported": true
}
```

**Response (Unsupported Game):**
```json
{
  "gameId": null,
  "name": null,
  "officialPlayTimeMinutes": 0,
  "minPlayers": 0,
  "maxPlayers": 0,
  "supported": false
}
```

### 2. POST /api/v1/estimates
**Purpose:** Generate session time estimate

**Request:**
```json
{
  "gameId": "catan",
  "groupProfile": {
    "playerCount": 4,
    "groupFamiliarity": "mixed",
    "turnPace": "medium",
    "analysisStyle": "moderate",
    "childrenIncluded": false
  }
}
```

**Response:**
```json
{
  "estimateId": "est_catan_001",
  "teachMinutes": 24,
  "playMinutes": 66,
  "totalMinutes": 90,
  "confidence": "medium",
  "playerCountFit": [
    {"playerCount": 3, "fit": "good"},
    {"playerCount": 4, "fit": "best"}
  ],
  "explanation": "Catan plays best at 4 players. Mixed familiarity means some rules review, adding 24 minutes.",
  "riskNotes": ["Rules reminders may increase downtime"]
}
```

**Important:** All time values are in **minutes**. `totalMinutes` must equal `teachMinutes + playMinutes`.

### 3. POST /api/v1/feedback
**Purpose:** Submit actual session timing feedback

**Request:**
```json
{
  "estimateId": "est_catan_001",
  "actualTeachMinutes": 30,
  "actualPlayMinutes": 75,
  "notes": "Took longer than expected due to rule questions"
}
```

**Response:**
```json
{
  "feedbackId": "fb_001",
  "estimateId": "est_catan_001",
  "accepted": true
}
```

**Important:** Time values are in **minutes**.

## Error Handling

### HTTP Error Mapping

The API client maps HTTP errors to domain errors:

| HTTP Status | HTTPError | ServiceError | User Message |
|-------------|-----------|--------------|--------------|
| 400 | badRequest | serverError | "Invalid request" |
| 404 | notFound | serverError | "Resource not found" |
| 500-599 | serverError | serverError | "Server error" |
| Network failure | networkError | networkError | "Connection failed" |
| Decode failure | decodingFailed | networkError | "Invalid response" |

### Error Response Format

Server errors should return:
```json
{
  "message": "Descriptive error message"
}
```

Or (legacy format):
```json
{
  "error": "Descriptive error message"
}
```

Both formats are supported via `ErrorResponse` model.

## Test Strategy

### 1. Mock Transport Tests
**Location:** `BoardGameTimerTests/NetworkTests.swift`

Test the mock transport itself:
- Returns configured responses
- Throws configured errors
- Records requests for verification

### 2. Request Encoding Tests

Verify API client constructs correct HTTP requests:
- Correct HTTP method (POST)
- Correct endpoint path
- Valid JSON body
- All required fields present

**Example:**
```swift
func testResolveBarcodeEncodesRequestCorrectly() async throws {
    let transport = MockHTTPTransport()
    transport.setResponse(scanResponse, for: .post, path: "/api/v1/scan/resolve")
    
    let client = BoardGameAPIClient(transport: transport)
    _ = try await client.resolveBarcode("0029877030712")
    
    let request = transport.recordedRequests[0]
    XCTAssertEqual(request.method, .post)
    XCTAssertEqual(request.path, "/api/v1/scan/resolve")
    
    let decoded = try JSONDecoder().decode(ScanRequest.self, from: request.body!)
    XCTAssertEqual(decoded.barcode, "0029877030712")
}
```

### 3. Response Decoding Tests

Verify API client correctly decodes responses:
- All fields parsed correctly
- Types match expectations
- Time values in minutes
- totalMinutes = teachMinutes + playMinutes

**Example:**
```swift
func testDecodesValidEstimateResponse() async throws {
    let transport = MockHTTPTransport()
    transport.setResponse(estimateResponse, for: .post, path: "/api/v1/estimates")
    
    let client = BoardGameAPIClient(transport: transport)
    let response = try await client.createEstimate(request)
    
    XCTAssertEqual(response.teachMinutes, 24)
    XCTAssertEqual(response.playMinutes, 66)
    XCTAssertEqual(response.totalMinutes, 90)
    XCTAssertEqual(response.teachMinutes + response.playMinutes, response.totalMinutes)
}
```

### 4. Unsupported Game Tests

Verify handling of unsupported games:
- `supported: false`
- `gameId: null`
- No error thrown (valid response)

**Example:**
```swift
func testHandlesUnsupportedGameResponse() async throws {
    let unsupportedResponse = ScanResponse(
        gameId: nil,
        name: nil,
        officialPlayTimeMinutes: 0,
        minPlayers: 0,
        maxPlayers: 0,
        supported: false
    )
    transport.setResponse(unsupportedResponse, for: .post, path: "/api/v1/scan/resolve")
    
    let response = try await client.resolveBarcode("9999999999999")
    
    XCTAssertNil(response.gameId)
    XCTAssertFalse(response.supported)
}
```

### 5. Server Error Tests

Verify error handling:
- 400 Bad Request
- 404 Not Found
- 500 Server Error
- Network errors
- Decode errors

**Example:**
```swift
func testHandles500ServerError() async {
    transport.setError(HTTPError.serverError("Internal server error"), 
                      for: .post, path: "/api/v1/estimates")
    
    do {
        _ = try await client.createEstimate(request)
        XCTFail("Expected error")
    } catch let error as ServiceError {
        if case .serverError(let message) = error {
            XCTAssertTrue(message.contains("server error"))
        }
    }
}
```

### 6. Integration Tests

Test complete flows end-to-end:
- Scan → Estimate → Feedback
- Verify request sequencing
- Validate data flow between calls

## Usage Examples

### Production Setup

```swift
// In AppCoordinator or dependency container
let baseURL = URL(string: "https://api.boardgametimer.example.com")!
let transport = URLSessionHTTPTransport(baseURL: baseURL)
let service: BoardGameService = BoardGameAPIClient(transport: transport)

// Or use convenience method
let service = BoardGameAPIClient.create(baseURL: "https://api.boardgametimer.example.com")
```

### Test Setup

```swift
// In test
let transport = MockHTTPTransport()
transport.setResponse(expectedResponse, for: .post, path: "/api/v1/scan/resolve")
let service: BoardGameService = BoardGameAPIClient(transport: transport)
let viewModel = ScanViewModel(scanner: fakeScanner, service: service)
```

### Preview Setup

```swift
#Preview {
    let service = MockBoardGameService() // Uses existing mock
    ScanView(
        viewModel: ScanViewModel(scanner: FakeBarcodeScanner(), service: service),
        path: .constant(NavigationPath())
    )
}
```

## Configuration

### Base URL Configuration

The base URL should be:
- **Development:** Environment variable or config file
- **Production:** Hardcoded or from app configuration
- **Testing:** Not needed (uses MockHTTPTransport)

**Example:**
```swift
extension BoardGameAPIClient {
    static var production: BoardGameAPIClient {
        let baseURL = ProcessInfo.processInfo.environment["API_BASE_URL"] 
            ?? "https://api.boardgametimer.com"
        return create(baseURL: baseURL)!
    }
}
```

### Request Headers

Default headers set by URLSessionHTTPTransport:
- `Content-Type: application/json`
- `Accept: application/json`

Additional headers can be added via `HTTPRequest.headers`.

### Timeouts

URLSession default timeouts apply:
- Request timeout: 60 seconds
- Resource timeout: 7 days

Custom timeouts can be set via URLSession configuration.

## Adding New Endpoints

To add a new API endpoint:

1. **Add models** (if needed):
   ```swift
   struct NewRequest: Codable { ... }
   struct NewResponse: Codable { ... }
   ```

2. **Add method to protocol**:
   ```swift
   protocol BoardGameService {
       func newOperation(_ request: NewRequest) async throws -> NewResponse
   }
   ```

3. **Implement in API client**:
   ```swift
   func newOperation(_ request: NewRequest) async throws -> NewResponse {
       let body = try encoder.encode(request)
       let httpRequest = HTTPRequest(method: .post, path: "/api/v1/new-endpoint", body: body)
       return try await transport.send(httpRequest)
   }
   ```

4. **Implement in mock service**:
   ```swift
   func newOperation(_ request: NewRequest) async throws -> NewResponse {
       // Return canned response
   }
   ```

5. **Add tests**:
   - Request encoding test
   - Response decoding test
   - Error handling tests

## Testing Best Practices

1. **Use MockHTTPTransport for unit tests**
   - Fast (no network)
   - Deterministic
   - Test-specific responses

2. **Test request construction**
   - Verify method, path, body
   - Check JSON encoding

3. **Test response parsing**
   - Verify all fields
   - Check type conversions
   - Validate business rules (e.g., totalMinutes = teach + play)

4. **Test error paths**
   - HTTP errors (400, 404, 500)
   - Network failures
   - Decode errors
   - Domain-specific errors

5. **Test edge cases**
   - Unsupported games (null fields)
   - Empty arrays
   - Optional fields
   - Extreme values

6. **Integration tests**
   - Multi-step flows
   - State transitions
   - Error recovery

## Future Enhancements

Potential improvements:
- **Retry logic** for transient failures
- **Request/response logging** for debugging
- **Analytics** integration for API metrics
- **Caching** for repeated requests
- **Authentication** headers (if needed)
- **Rate limiting** handling
- **Pagination** support (for lists)
- **WebSocket** support for real-time updates

## Dependencies

- **Foundation** framework (URLSession, Codable)
- No third-party networking libraries required
- Async/await for concurrency (iOS 15+)

## Performance Considerations

- **Network calls** are asynchronous (non-blocking UI)
- **JSON encoding/decoding** is fast for small payloads
- **MockHTTPTransport** has configurable delay for realistic testing
- **URLSession** handles connection pooling automatically

## Security Notes

- Uses **HTTPS** in production (enforced by URLSession ATS)
- **No credentials** stored in code
- **Error messages** don't leak sensitive information
- **Input validation** on server side (not client responsibility)

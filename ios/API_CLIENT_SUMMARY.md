# API Client Structure & Test Strategy

## Quick Reference

### File Structure
```
BoardGameTimer/
├── Network/
│   ├── HTTPTransport.swift       # Transport protocol + implementations
│   └── BoardGameAPIClient.swift  # API client (implements BoardGameService)
├── Services/
│   └── BoardGameService.swift    # Service protocol + mock
└── Models/
    ├── ScanModels.swift          # ScanRequest, ScanResponse
    ├── EstimateModels.swift      # EstimateRequest, EstimateResponse
    ├── FeedbackModels.swift      # FeedbackRequest, FeedbackResponse
    └── ErrorResponse.swift       # Error response model

BoardGameTimerTests/
└── NetworkTests.swift            # Comprehensive network tests
```

## API Client Structure

### Layer 1: Service Protocol (Domain Interface)
```swift
protocol BoardGameService {
    func resolveBarcode(_ barcode: String) async throws -> ScanResponse
    func createEstimate(_ request: EstimateRequest) async throws -> EstimateResponse
    func submitFeedback(_ request: FeedbackRequest) async throws -> FeedbackResponse
}
```

### Layer 2: HTTP Transport (Network Abstraction)
```swift
protocol HTTPTransport {
    func send<T: Decodable>(_ request: HTTPRequest) async throws -> T
}

// Production
class URLSessionHTTPTransport: HTTPTransport { ... }

// Testing
class MockHTTPTransport: HTTPTransport {
    var responses: [String: Any]
    var errors: [String: Error]
    var recordedRequests: [HTTPRequest]
}
```

### Layer 3: API Client (Bridge)
```swift
class BoardGameAPIClient: BoardGameService {
    private let transport: HTTPTransport
    
    func resolveBarcode(_ barcode: String) async throws -> ScanResponse {
        // 1. Encode request
        // 2. Build HTTPRequest
        // 3. Send via transport
        // 4. Handle errors
    }
}
```

## API Endpoints

| Endpoint | Method | Purpose | Time Unit |
|----------|--------|---------|-----------|
| `/api/v1/scan/resolve` | POST | Barcode → Game info | minutes |
| `/api/v1/estimates` | POST | Game + Profile → Estimate | minutes |
| `/api/v1/feedback` | POST | Submit actual times | minutes |

**Important:** All time values (teach, play, total) are in **minutes**.

## Test Strategy

### Test Categories

#### 1. Transport Tests
**Goal:** Verify mock infrastructure works

```swift
func testMockTransportReturnsConfiguredResponse()
func testMockTransportThrowsConfiguredError()
func testMockTransportRecordsRequests()
```

#### 2. Request Encoding Tests
**Goal:** Verify API client builds correct HTTP requests

```swift
func testResolveBarcodeEncodesRequestCorrectly() {
    // Verify: method, path, body content
    XCTAssertEqual(request.method, .post)
    XCTAssertEqual(request.path, "/api/v1/scan/resolve")
    let decoded = try JSONDecoder().decode(ScanRequest.self, from: body)
    XCTAssertEqual(decoded.barcode, "0029877030712")
}
```

#### 3. Response Decoding Tests
**Goal:** Verify API client parses responses correctly

```swift
func testDecodesValidEstimateResponse() {
    // Verify: all fields, types, business rules
    XCTAssertEqual(response.teachMinutes, 24)
    XCTAssertEqual(response.playMinutes, 66)
    XCTAssertEqual(response.totalMinutes, 90)
    XCTAssertEqual(response.teachMinutes + response.playMinutes, 
                   response.totalMinutes) // Business rule!
}
```

#### 4. Unsupported Game Tests
**Goal:** Verify handling of games not in catalog

```swift
func testHandlesUnsupportedGameResponse() {
    // Unsupported games return valid response, not error
    XCTAssertNil(response.gameId)
    XCTAssertNil(response.name)
    XCTAssertFalse(response.supported)
    // No exception thrown!
}
```

#### 5. Server Error Tests
**Goal:** Verify error handling for all HTTP errors

```swift
func testHandles400BadRequest()
func testHandles404NotFound()
func testHandles500ServerError()
func testHandlesNetworkError()
func testHandlesDecodingError()
```

#### 6. Time Validation Tests
**Goal:** Ensure time values are in minutes

```swift
func testEstimateTimesAreInMinutes() {
    // All times in minutes
    XCTAssertGreaterThanOrEqual(response.teachMinutes, 0)
    XCTAssertGreaterThanOrEqual(response.playMinutes, 0)
    XCTAssertEqual(response.totalMinutes, 
                   response.teachMinutes + response.playMinutes)
}
```

#### 7. Integration Tests
**Goal:** Test complete flows

```swift
func testFullScanToEstimateFlow() {
    // 1. Scan barcode
    let scan = try await client.resolveBarcode("0029877030712")
    
    // 2. Create estimate
    let estimate = try await client.createEstimate(...)
    
    // Verify both requests made
    XCTAssertEqual(transport.recordedRequests.count, 2)
}
```

## Test Setup Pattern

```swift
// 1. Create mock transport
let transport = MockHTTPTransport()

// 2. Configure response
let response = ScanResponse(gameId: "catan", name: "Catan", ...)
transport.setResponse(response, for: .post, path: "/api/v1/scan/resolve")

// 3. Create client
let client = BoardGameAPIClient(transport: transport)

// 4. Make request
let result = try await client.resolveBarcode("0029877030712")

// 5. Verify result
XCTAssertEqual(result.gameId, "catan")

// 6. Verify request was made correctly
let request = transport.recordedRequests[0]
XCTAssertEqual(request.method, .post)
```

## Error Handling Map

```
HTTP Error          → HTTPError              → ServiceError
──────────────────────────────────────────────────────────
400 Bad Request     → .badRequest(msg)       → .serverError(msg)
404 Not Found       → .notFound(msg)         → .serverError(msg)
500 Server Error    → .serverError(msg)      → .serverError(msg)
Network Failure     → .networkError(err)     → .networkError(msg)
Decode Failure      → .decodingFailed(err)   → .networkError(msg)
```

## Usage Examples

### Production
```swift
let transport = URLSessionHTTPTransport(
    baseURL: URL(string: "https://api.example.com")!
)
let service: BoardGameService = BoardGameAPIClient(transport: transport)
```

### Testing
```swift
let transport = MockHTTPTransport()
transport.setResponse(scanResponse, for: .post, path: "/api/v1/scan/resolve")
let service: BoardGameService = BoardGameAPIClient(transport: transport)
```

### Mock Service (For UI Previews)
```swift
let service: BoardGameService = MockBoardGameService()
// No network, instant responses, configurable delays
```

## Key Design Principles

1. **Protocol-based:** All layers use protocols for testability
2. **Async/await:** Modern concurrency throughout
3. **No side effects:** Pure request/response (no caching, no state)
4. **Error mapping:** HTTP errors → Domain errors
5. **Type-safe:** Codable models, no `Any` types
6. **Time in minutes:** Consistent unit throughout
7. **Testable:** Mock transport for fast, deterministic tests

## Test Coverage Summary

| Category | Tests | What's Tested |
|----------|-------|---------------|
| Transport | 3 | Mock infrastructure |
| Request Encoding | 3 | POST body, path, method |
| Response Decoding | 3 | All endpoints, all fields |
| Unsupported Games | 2 | Null handling, supported flag |
| Server Errors | 5 | 400, 404, 500, network, decode |
| Time Validation | 2 | Minutes unit, math rules |
| Integration | 1 | Multi-step flow |
| **Total** | **19** | **All network paths** |

## Adding the Test File to Xcode

**IMPORTANT:** The new test file needs to be added to the Xcode project:

1. Open `BoardGameTimer.xcodeproj` in Xcode
2. Right-click `BoardGameTimerTests` folder
3. Add Files → `NetworkTests.swift`
4. Check "Add to targets: BoardGameTimerTests"
5. Click "Add"

## Running Tests

```bash
# Command line
xcodebuild test -scheme BoardGameTimer -destination 'platform=iOS Simulator,name=iPhone 15'

# Xcode
Cmd+U (runs all tests)
Cmd+Control+Option+U (runs tests for current file)
```

## Next Steps

1. **Add network files to Xcode project**
   - HTTPTransport.swift
   - BoardGameAPIClient.swift
   - NetworkTests.swift

2. **Configure base URL**
   - Add to app configuration
   - Use environment variable for dev

3. **Update AppCoordinator**
   - Replace MockBoardGameService with BoardGameAPIClient (when backend ready)
   - Or keep mock for development

4. **Run tests**
   - All 19 network tests should pass
   - No network required (uses mocks)

5. **Test with real backend** (when available)
   - Create integration test target
   - Point to staging API
   - Verify real network calls

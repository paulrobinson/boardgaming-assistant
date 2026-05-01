import XCTest
@testable import BoardGameTimer

@MainActor
final class NetworkTests: XCTestCase {

    // MARK: - HTTP Transport Tests

    func testMockTransportReturnsConfiguredResponse() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = ScanResponse(
            gameId: "catan",
            name: "Catan",
            officialPlayTimeMinutes: 60,
            minPlayers: 3,
            maxPlayers: 4,
            supported: true
        )

        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/scan/resolve")

        let request = HTTPRequest(method: .post, path: "/api/v1/scan/resolve")
        let response: ScanResponse = try await transport.send(request)

        XCTAssertEqual(response.gameId, "catan")
        XCTAssertEqual(response.name, "Catan")
        XCTAssertEqual(response.officialPlayTimeMinutes, 60)
        XCTAssertTrue(response.supported)
    }

    func testMockTransportThrowsConfiguredError() async {
        let transport = MockHTTPTransport()
        transport.setError(HTTPError.serverError("Test error"), for: .post, path: "/api/v1/scan/resolve")

        let request = HTTPRequest(method: .post, path: "/api/v1/scan/resolve")

        do {
            let _: ScanResponse = try await transport.send(request)
            XCTFail("Expected error to be thrown")
        } catch let error as HTTPError {
            if case .serverError(let message) = error {
                XCTAssertEqual(message, "Test error")
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        } catch {
            XCTFail("Expected HTTPError, got \(error)")
        }
    }

    func testMockTransportRecordsRequests() async throws {
        let transport = MockHTTPTransport()
        transport.setResponse(ScanResponse(gameId: nil, name: nil, officialPlayTimeMinutes: 0, minPlayers: 0, maxPlayers: 0, supported: false), for: .post, path: "/test")

        let request = HTTPRequest(method: .post, path: "/test")
        let _: ScanResponse = try await transport.send(request)

        XCTAssertEqual(transport.recordedRequests.count, 1)
        XCTAssertEqual(transport.recordedRequests[0].method, .post)
        XCTAssertEqual(transport.recordedRequests[0].path, "/test")
    }

    // MARK: - API Client Request Encoding Tests

    func testResolveBarcodeEncodesRequestCorrectly() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = ScanResponse(
            gameId: "catan",
            name: "Catan",
            officialPlayTimeMinutes: 60,
            minPlayers: 3,
            maxPlayers: 4,
            supported: true
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)

        _ = try await client.resolveBarcode("0029877030712")

        XCTAssertEqual(transport.recordedRequests.count, 1)
        let request = transport.recordedRequests[0]

        XCTAssertEqual(request.method, .post)
        XCTAssertEqual(request.path, "/api/v1/scan/resolve")
        XCTAssertNotNil(request.body)

        // Verify request body
        let decoded = try JSONDecoder().decode(ScanRequest.self, from: request.body!)
        XCTAssertEqual(decoded.barcode, "0029877030712")
    }

    func testCreateEstimateEncodesRequestCorrectly() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = EstimateResponse(
            estimateId: "est_001",
            teachMinutes: 20,
            playMinutes: 60,
            totalMinutes: 80,
            confidence: "high",
            playerCountFit: [],
            explanation: "Standard session",
            riskNotes: []
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/estimates")

        let client = BoardGameAPIClient(transport: transport)

        let profile = GroupProfileDto(
            playerCount: 4,
            groupFamiliarity: "mixed",
            turnPace: "medium",
            analysisStyle: "moderate",
            childrenIncluded: false
        )
        let estimateRequest = EstimateRequest(gameId: "catan", groupProfile: profile)

        _ = try await client.createEstimate(estimateRequest)

        XCTAssertEqual(transport.recordedRequests.count, 1)
        let request = transport.recordedRequests[0]

        XCTAssertEqual(request.method, .post)
        XCTAssertEqual(request.path, "/api/v1/estimates")
        XCTAssertNotNil(request.body)

        // Verify request body
        let decoded = try JSONDecoder().decode(EstimateRequest.self, from: request.body!)
        XCTAssertEqual(decoded.gameId, "catan")
        XCTAssertEqual(decoded.groupProfile.playerCount, 4)
        XCTAssertEqual(decoded.groupProfile.groupFamiliarity, "mixed")
    }

    func testSubmitFeedbackEncodesRequestCorrectly() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = FeedbackResponse(
            feedbackId: "fb_001",
            estimateId: "est_001",
            accepted: true
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/feedback")

        let client = BoardGameAPIClient(transport: transport)

        let feedbackRequest = FeedbackRequest(
            estimateId: "est_001",
            actualTeachMinutes: 25,
            actualPlayMinutes: 65,
            notes: "Took longer than expected"
        )

        _ = try await client.submitFeedback(feedbackRequest)

        XCTAssertEqual(transport.recordedRequests.count, 1)
        let request = transport.recordedRequests[0]

        XCTAssertEqual(request.method, .post)
        XCTAssertEqual(request.path, "/api/v1/feedback")
        XCTAssertNotNil(request.body)

        // Verify request body
        let decoded = try JSONDecoder().decode(FeedbackRequest.self, from: request.body!)
        XCTAssertEqual(decoded.estimateId, "est_001")
        XCTAssertEqual(decoded.actualTeachMinutes, 25)
        XCTAssertEqual(decoded.actualPlayMinutes, 65)
        XCTAssertEqual(decoded.notes, "Took longer than expected")
    }

    // MARK: - Response Decoding Tests

    func testDecodesValidScanResponse() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = ScanResponse(
            gameId: "ticket-to-ride",
            name: "Ticket to Ride",
            officialPlayTimeMinutes: 45,
            minPlayers: 2,
            maxPlayers: 5,
            supported: true
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)
        let response = try await client.resolveBarcode("0824968717912")

        XCTAssertEqual(response.gameId, "ticket-to-ride")
        XCTAssertEqual(response.name, "Ticket to Ride")
        XCTAssertEqual(response.officialPlayTimeMinutes, 45)
        XCTAssertEqual(response.minPlayers, 2)
        XCTAssertEqual(response.maxPlayers, 5)
        XCTAssertTrue(response.supported)
    }

    func testDecodesValidEstimateResponse() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = EstimateResponse(
            estimateId: "est_catan_001",
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
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/estimates")

        let client = BoardGameAPIClient(transport: transport)
        let profile = GroupProfileDto(playerCount: 4, groupFamiliarity: "mixed", turnPace: "medium", analysisStyle: "moderate", childrenIncluded: false)
        let response = try await client.createEstimate(EstimateRequest(gameId: "catan", groupProfile: profile))

        XCTAssertEqual(response.estimateId, "est_catan_001")
        XCTAssertEqual(response.teachMinutes, 24)
        XCTAssertEqual(response.playMinutes, 66)
        XCTAssertEqual(response.totalMinutes, 90)
        XCTAssertEqual(response.teachMinutes + response.playMinutes, response.totalMinutes)
        XCTAssertEqual(response.confidence, "medium")
        XCTAssertEqual(response.playerCountFit.count, 2)
        XCTAssertEqual(response.playerCountFit[0].playerCount, 3)
        XCTAssertEqual(response.playerCountFit[1].fit, "best")
        XCTAssertTrue(response.explanation.contains("minutes"))
    }

    func testDecodesValidFeedbackResponse() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = FeedbackResponse(
            feedbackId: "fb_test_001",
            estimateId: "est_001",
            accepted: true
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/feedback")

        let client = BoardGameAPIClient(transport: transport)
        let feedbackRequest = FeedbackRequest(estimateId: "est_001", actualTeachMinutes: 20, actualPlayMinutes: 60, notes: nil)
        let response = try await client.submitFeedback(feedbackRequest)

        XCTAssertEqual(response.feedbackId, "fb_test_001")
        XCTAssertEqual(response.estimateId, "est_001")
        XCTAssertTrue(response.accepted)
    }

    // MARK: - Unsupported Game Handling Tests

    func testHandlesUnsupportedGameResponse() async throws {
        let transport = MockHTTPTransport()
        let unsupportedResponse = ScanResponse(
            gameId: nil,
            name: nil,
            officialPlayTimeMinutes: 0,
            minPlayers: 0,
            maxPlayers: 0,
            supported: false
        )
        transport.setResponse(unsupportedResponse, for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)
        let response = try await client.resolveBarcode("9999999999999")

        XCTAssertNil(response.gameId)
        XCTAssertNil(response.name)
        XCTAssertFalse(response.supported)
        XCTAssertEqual(response.officialPlayTimeMinutes, 0)
    }

    func testUnsupportedGameHasNoGameId() async throws {
        let transport = MockHTTPTransport()
        let unsupportedResponse = ScanResponse(
            gameId: nil,
            name: nil,
            officialPlayTimeMinutes: 0,
            minPlayers: 0,
            maxPlayers: 0,
            supported: false
        )
        transport.setResponse(unsupportedResponse, for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)
        let response = try await client.resolveBarcode("0000000000000")

        XCTAssertNil(response.gameId)
        XCTAssertFalse(response.supported)
    }

    // MARK: - Server Error Handling Tests

    func testHandles400BadRequest() async {
        let transport = MockHTTPTransport()
        transport.setError(HTTPError.badRequest("Invalid barcode format"), for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)

        do {
            _ = try await client.resolveBarcode("invalid")
            XCTFail("Expected error to be thrown")
        } catch let error as ServiceError {
            if case .serverError(let message) = error {
                XCTAssertTrue(message.contains("Invalid barcode"))
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        } catch {
            XCTFail("Expected ServiceError, got \(error)")
        }
    }

    func testHandles404NotFound() async {
        let transport = MockHTTPTransport()
        transport.setError(HTTPError.notFound("Game not found"), for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)

        do {
            _ = try await client.resolveBarcode("0000000000000")
            XCTFail("Expected error to be thrown")
        } catch let error as ServiceError {
            if case .serverError(let message) = error {
                XCTAssertTrue(message.contains("not found"))
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        } catch {
            XCTFail("Expected ServiceError, got \(error)")
        }
    }

    func testHandles500ServerError() async {
        let transport = MockHTTPTransport()
        transport.setError(HTTPError.serverError("Internal server error"), for: .post, path: "/api/v1/estimates")

        let client = BoardGameAPIClient(transport: transport)
        let profile = GroupProfileDto(playerCount: 4, groupFamiliarity: "mixed", turnPace: "medium", analysisStyle: "moderate", childrenIncluded: false)

        do {
            _ = try await client.createEstimate(EstimateRequest(gameId: "catan", groupProfile: profile))
            XCTFail("Expected error to be thrown")
        } catch let error as ServiceError {
            if case .serverError(let message) = error {
                XCTAssertTrue(message.contains("server error"))
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        } catch {
            XCTFail("Expected ServiceError, got \(error)")
        }
    }

    func testHandlesNetworkError() async {
        let transport = MockHTTPTransport()
        transport.setError(HTTPError.networkError(URLError(.notConnectedToInternet)), for: .post, path: "/api/v1/feedback")

        let client = BoardGameAPIClient(transport: transport)
        let feedbackRequest = FeedbackRequest(estimateId: "est_001", actualTeachMinutes: 20, actualPlayMinutes: 60, notes: nil)

        do {
            _ = try await client.submitFeedback(feedbackRequest)
            XCTFail("Expected error to be thrown")
        } catch let error as ServiceError {
            if case .networkError = error {
                // Expected
            } else {
                XCTFail("Expected networkError, got \(error)")
            }
        } catch {
            XCTFail("Expected ServiceError, got \(error)")
        }
    }

    func testHandlesDecodingError() async {
        let transport = MockHTTPTransport()
        // Set invalid data that can't be decoded
        transport.setResponse(Data("invalid json".utf8), for: .post, path: "/api/v1/scan/resolve")

        let client = BoardGameAPIClient(transport: transport)

        do {
            _ = try await client.resolveBarcode("0029877030712")
            XCTFail("Expected error to be thrown")
        } catch let error as ServiceError {
            if case .networkError = error {
                // Expected - decoding errors are mapped to network errors
            } else {
                XCTFail("Expected networkError, got \(error)")
            }
        } catch {
            XCTFail("Expected ServiceError, got \(error)")
        }
    }

    // MARK: - Time Estimate Validation Tests

    func testEstimateTimesAreInMinutes() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = EstimateResponse(
            estimateId: "est_001",
            teachMinutes: 15,
            playMinutes: 45,
            totalMinutes: 60,
            confidence: "high",
            playerCountFit: [],
            explanation: "Session estimate in minutes",
            riskNotes: []
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/estimates")

        let client = BoardGameAPIClient(transport: transport)
        let profile = GroupProfileDto(playerCount: 3, groupFamiliarity: "all-experienced", turnPace: "fast", analysisStyle: "quick", childrenIncluded: false)
        let response = try await client.createEstimate(EstimateRequest(gameId: "catan", groupProfile: profile))

        // All time values should be in minutes
        XCTAssertGreaterThanOrEqual(response.teachMinutes, 0)
        XCTAssertGreaterThanOrEqual(response.playMinutes, 0)
        XCTAssertGreaterThanOrEqual(response.totalMinutes, 0)

        // Total should equal teach + play
        XCTAssertEqual(response.totalMinutes, response.teachMinutes + response.playMinutes)
    }

    func testFeedbackTimesAreInMinutes() async throws {
        let transport = MockHTTPTransport()
        let expectedResponse = FeedbackResponse(
            feedbackId: "fb_001",
            estimateId: "est_001",
            accepted: true
        )
        transport.setResponse(expectedResponse, for: .post, path: "/api/v1/feedback")

        let client = BoardGameAPIClient(transport: transport)

        // Feedback times are in minutes
        let feedbackRequest = FeedbackRequest(
            estimateId: "est_001",
            actualTeachMinutes: 30,  // minutes
            actualPlayMinutes: 75,    // minutes
            notes: "Session took 105 minutes total"
        )

        let response = try await client.submitFeedback(feedbackRequest)

        XCTAssertTrue(response.accepted)
        XCTAssertEqual(feedbackRequest.actualTeachMinutes + feedbackRequest.actualPlayMinutes, 105)
    }

    // MARK: - Integration Tests

    func testFullScanToEstimateFlow() async throws {
        let transport = MockHTTPTransport()

        // Setup scan response
        let scanResponse = ScanResponse(
            gameId: "catan",
            name: "Catan",
            officialPlayTimeMinutes: 60,
            minPlayers: 3,
            maxPlayers: 4,
            supported: true
        )
        transport.setResponse(scanResponse, for: .post, path: "/api/v1/scan/resolve")

        // Setup estimate response
        let estimateResponse = EstimateResponse(
            estimateId: "est_001",
            teachMinutes: 20,
            playMinutes: 70,
            totalMinutes: 90,
            confidence: "high",
            playerCountFit: [PlayerCountFitDto(playerCount: 4, fit: "best")],
            explanation: "Optimal session time estimate",
            riskNotes: []
        )
        transport.setResponse(estimateResponse, for: .post, path: "/api/v1/estimates")

        let client = BoardGameAPIClient(transport: transport)

        // 1. Scan barcode
        let scan = try await client.resolveBarcode("0029877030712")
        XCTAssertEqual(scan.gameId, "catan")
        XCTAssertTrue(scan.supported)

        // 2. Create estimate
        let profile = GroupProfileDto(playerCount: 4, groupFamiliarity: "mixed", turnPace: "medium", analysisStyle: "moderate", childrenIncluded: false)
        let estimate = try await client.createEstimate(EstimateRequest(gameId: scan.gameId!, groupProfile: profile))

        XCTAssertEqual(estimate.teachMinutes, 20)
        XCTAssertEqual(estimate.playMinutes, 70)
        XCTAssertEqual(estimate.totalMinutes, 90)

        // Verify both requests were made
        XCTAssertEqual(transport.recordedRequests.count, 2)
    }
}

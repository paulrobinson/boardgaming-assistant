import XCTest
@testable import BoardGameTimer

@MainActor
final class EstimateViewModelTests: XCTestCase {

    func testStartsInIdleState() {
        let vm = EstimateViewModel(service: MockBoardGameService())
        XCTAssertEqual(vm.state, .idle)
    }

    func testSuccessfulEstimateTransitionsToLoaded() async {
        let vm = EstimateViewModel(service: MockBoardGameService())

        await vm.createEstimate(gameId: "catan", profile: .default)

        if case .loaded(let estimate) = vm.state {
            XCTAssertEqual(estimate.estimateId, "est_mock001")
            XCTAssertEqual(estimate.totalMinutes, estimate.teachMinutes + estimate.playMinutes)
            XCTAssertFalse(estimate.explanation.isEmpty)
        } else {
            XCTFail("Expected loaded state, got \(vm.state)")
        }
    }

    func testServiceFailureTransitionsToError() async {
        let service = MockBoardGameService()
        service.shouldFailNextCall = true
        let vm = EstimateViewModel(service: service)

        await vm.createEstimate(gameId: "catan", profile: .default)

        if case .error = vm.state {
            // expected
        } else {
            XCTFail("Expected error state, got \(vm.state)")
        }
    }

    func testTotalMinutesEqualsTeachPlusPlay() async {
        let vm = EstimateViewModel(service: MockBoardGameService())

        await vm.createEstimate(gameId: "catan", profile: .default)

        if case .loaded(let estimate) = vm.state {
            XCTAssertEqual(estimate.totalMinutes, estimate.teachMinutes + estimate.playMinutes)
        } else {
            XCTFail("Expected loaded state")
        }
    }

    func testResetGoesBackToIdle() async {
        let vm = EstimateViewModel(service: MockBoardGameService())
        await vm.createEstimate(gameId: "catan", profile: .default)
        vm.reset()
        XCTAssertEqual(vm.state, .idle)
    }
}

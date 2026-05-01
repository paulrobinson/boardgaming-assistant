import XCTest
@testable import BoardGameTimer

@MainActor
final class FeedbackViewModelTests: XCTestCase {

    func testStartsInEditingState() {
        let vm = FeedbackViewModel(service: MockBoardGameService())
        XCTAssertEqual(vm.state, .editing)
    }

    func testSuccessfulSubmitTransitionsToSubmitted() async {
        let vm = FeedbackViewModel(service: MockBoardGameService())
        vm.actualTeachMinutes = 25
        vm.actualPlayMinutes = 80

        await vm.submit(estimateId: "est_mock001")

        XCTAssertEqual(vm.state, .submitted)
    }

    func testServiceFailureTransitionsToError() async {
        let service = MockBoardGameService()
        service.shouldFailNextCall = true
        let vm = FeedbackViewModel(service: service)

        await vm.submit(estimateId: "est_mock001")

        if case .error = vm.state {
            // expected
        } else {
            XCTFail("Expected error state, got \(vm.state)")
        }
    }

    func testDefaultMinuteValues() {
        let vm = FeedbackViewModel(service: MockBoardGameService())
        XCTAssertEqual(vm.actualTeachMinutes, 15)
        XCTAssertEqual(vm.actualPlayMinutes, 60)
    }
}

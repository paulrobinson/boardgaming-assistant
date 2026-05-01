import SwiftUI

struct EstimateResultView: View {
    @ObservedObject var viewModel: EstimateViewModel
    let gameId: String
    let profile: GroupProfileDto
    @Binding var path: NavigationPath

    var body: some View {
        Group {
            switch viewModel.state {
            case .idle, .loading:
                loadingView

            case .loaded(let estimate):
                resultView(estimate)

            case .error(let message):
                errorView(message)
            }
        }
        .navigationTitle("Session Estimate")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if viewModel.state == .idle {
                await viewModel.createEstimate(gameId: gameId, profile: profile)
            }
        }
    }

    // MARK: - Loading State

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)

            Text("Calculating session time...")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Result View

    private func resultView(_ estimate: EstimateResponse) -> some View {
        ScrollView(.vertical, showsIndicators: true) {  // Explicitly show scroll indicators
            VStack(spacing: 16) {  // REDUCED from 24 to 16 to be more compact
                // Hero time card - MORE COMPACT
                TimeEstimateCard(
                    totalMinutes: estimate.totalMinutes,
                    teachMinutes: estimate.teachMinutes,
                    playMinutes: estimate.playMinutes
                )

                // Confidence badge and player count - COMBINED IN ONE ROW
                VStack(spacing: 12) {
                    HStack {
                        ConfidenceBadge(confidence: estimate.confidence)
                        Spacer()
                    }

                    if !estimate.playerCountFit.isEmpty {
                        PlayerCountFitView(playerCountFit: estimate.playerCountFit)
                    }
                }
                .padding(.vertical, -8)  // Reduce spacing

                // Explanation section
                VStack(alignment: .leading, spacing: 12) {
                    Text("What to Expect")
                        .font(.headline)

                    Text(estimate.explanation)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(.quaternary.opacity(0.3))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                // Risk notes (if any)
                if !estimate.riskNotes.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Things to Watch For")
                            .font(.headline)

                        ForEach(estimate.riskNotes, id: \.self) { note in
                            RiskNoteItem(note: note)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Actions - COSMETIC: Changed "Report" to "Submit" to verify latest code
                VStack(spacing: 12) {
                    // Feedback action
                    Button {
                        path.append(Route.feedback(estimateId: estimate.estimateId))
                    } label: {
                        HStack {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                            Text("Submit Actual Times")  // CHANGED FROM "Report"
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                    }
                    .buttonStyle(.borderedProminent)

                    // Done button
                    Button {
                        path = NavigationPath()
                    } label: {
                        Text("Done")
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(.horizontal)
            .padding(.top, 8)
            .padding(.bottom, 120)  // MASSIVE bottom padding - should definitely work
        }
        .scrollContentBackground(.hidden)
    }

    // MARK: - Error View

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 56))
                .foregroundStyle(.red)

            Text("Estimation Failed")
                .font(.title2.bold())

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button {
                Task {
                    await viewModel.createEstimate(gameId: gameId, profile: profile)
                }
            } label: {
                HStack {
                    Image(systemName: "arrow.clockwise")
                    Text("Retry")
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

// MARK: - Preview Support

extension EstimateResponse {
    static var previewData: EstimateResponse {
        EstimateResponse(
            estimateId: "est_preview_001",
            teachMinutes: 24,
            playMinutes: 66,
            totalMinutes: 90,
            confidence: "medium",
            playerCountFit: [
                PlayerCountFitDto(playerCount: 2, fit: "okay"),
                PlayerCountFitDto(playerCount: 3, fit: "good"),
                PlayerCountFitDto(playerCount: 4, fit: "best"),
                PlayerCountFitDto(playerCount: 5, fit: "good")
            ],
            explanation: "Catan plays best with 4 players. Your group has mixed familiarity, so plan for about 24 minutes of rules review and reminders during the first round.",
            riskNotes: [
                "Rules reminders may increase downtime between turns",
                "Resource trading explanations could add 5-10 minutes"
            ]
        )
    }

    static var previewHighConfidence: EstimateResponse {
        EstimateResponse(
            estimateId: "est_preview_002",
            teachMinutes: 10,
            playMinutes: 50,
            totalMinutes: 60,
            confidence: "high",
            playerCountFit: [
                PlayerCountFitDto(playerCount: 3, fit: "best"),
                PlayerCountFitDto(playerCount: 4, fit: "great")
            ],
            explanation: "Ticket to Ride is straightforward for experienced players. Fast turn pace should keep things moving smoothly.",
            riskNotes: []
        )
    }

    static var previewLowConfidence: EstimateResponse {
        EstimateResponse(
            estimateId: "est_preview_003",
            teachMinutes: 45,
            playMinutes: 120,
            totalMinutes: 165,
            confidence: "low",
            playerCountFit: [
                PlayerCountFitDto(playerCount: 4, fit: "okay"),
                PlayerCountFitDto(playerCount: 5, fit: "poor")
            ],
            explanation: "Complex strategy game with children included. Teaching time is uncertain and could vary significantly based on attention span.",
            riskNotes: [
                "Children may need frequent breaks",
                "Complex rules may require repeated explanations",
                "Analysis paralysis could significantly extend play time"
            ]
        )
    }
}

// MARK: - Previews

#Preview("Loaded - Medium Confidence") {
    NavigationStack {
        EstimateResultView(
            viewModel: {
                let vm = EstimateViewModel(service: MockBoardGameService())
                vm.state = .loaded(.previewData)
                return vm
            }(),
            gameId: "catan",
            profile: GroupProfileDto(
                playerCount: 4,
                groupFamiliarity: "mixed",
                turnPace: "medium",
                analysisStyle: "moderate",
                childrenIncluded: false
            ),
            path: .constant(NavigationPath())
        )
    }
}

#Preview("Loaded - High Confidence") {
    NavigationStack {
        EstimateResultView(
            viewModel: {
                let vm = EstimateViewModel(service: MockBoardGameService())
                vm.state = .loaded(.previewHighConfidence)
                return vm
            }(),
            gameId: "ticket-to-ride",
            profile: GroupProfileDto(
                playerCount: 3,
                groupFamiliarity: "all-experienced",
                turnPace: "fast",
                analysisStyle: "quick",
                childrenIncluded: false
            ),
            path: .constant(NavigationPath())
        )
    }
}

#Preview("Loaded - Low Confidence") {
    NavigationStack {
        EstimateResultView(
            viewModel: {
                let vm = EstimateViewModel(service: MockBoardGameService())
                vm.state = .loaded(.previewLowConfidence)
                return vm
            }(),
            gameId: "complex-game",
            profile: GroupProfileDto(
                playerCount: 4,
                groupFamiliarity: "mixed",
                turnPace: "slow",
                analysisStyle: "deep",
                childrenIncluded: true
            ),
            path: .constant(NavigationPath())
        )
    }
}

#Preview("Loading") {
    NavigationStack {
        EstimateResultView(
            viewModel: EstimateViewModel(service: MockBoardGameService()),
            gameId: "catan",
            profile: GroupProfileDto(
                playerCount: 4,
                groupFamiliarity: "mixed",
                turnPace: "medium",
                analysisStyle: "moderate",
                childrenIncluded: false
            ),
            path: .constant(NavigationPath())
        )
    }
}

#Preview("Error") {
    NavigationStack {
        EstimateResultView(
            viewModel: {
                let vm = EstimateViewModel(service: MockBoardGameService())
                vm.state = .error("Unable to connect to server. Please check your internet connection and try again.")
                return vm
            }(),
            gameId: "catan",
            profile: GroupProfileDto(
                playerCount: 4,
                groupFamiliarity: "mixed",
                turnPace: "medium",
                analysisStyle: "moderate",
                childrenIncluded: false
            ),
            path: .constant(NavigationPath())
        )
    }
}

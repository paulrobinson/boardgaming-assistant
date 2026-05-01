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
                ProgressView("Estimating session time...")

            case .loaded(let estimate):
                resultView(estimate)

            case .error(let message):
                errorView(message)
            }
        }
        .navigationTitle("Estimate")
        .task {
            if viewModel.state == .idle {
                await viewModel.createEstimate(gameId: gameId, profile: profile)
            }
        }
    }

    private func resultView(_ estimate: EstimateResponse) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                // Total time
                VStack(spacing: 4) {
                    Text("\(estimate.totalMinutes)")
                        .font(.system(size: 64, weight: .bold, design: .rounded))
                    Text("minutes total")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.top)

                // Breakdown
                HStack(spacing: 32) {
                    timeBlock("Teach", estimate.teachMinutes)
                    timeBlock("Play", estimate.playMinutes)
                }

                // Confidence
                HStack {
                    Text("Confidence")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(estimate.confidence.capitalized)
                        .fontWeight(.medium)
                        .foregroundStyle(confidenceColor(estimate.confidence))
                }
                .padding()
                .background(.quaternary.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                // Explanation
                VStack(alignment: .leading, spacing: 8) {
                    Text("Details")
                        .font(.headline)
                    Text(estimate.explanation)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Risk notes
                if !estimate.riskNotes.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Heads Up")
                            .font(.headline)
                        ForEach(estimate.riskNotes, id: \.self) { note in
                            Label(note, systemImage: "exclamationmark.circle")
                                .font(.subheadline)
                                .foregroundStyle(.orange)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Feedback button
                Button {
                    path.append(Route.feedback(estimateId: estimate.estimateId))
                } label: {
                    Text("How did it go?")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
                .buttonStyle(.bordered)
                .padding(.top)

                // Done
                Button {
                    path = NavigationPath()
                } label: {
                    Text("Done")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
    }

    private func timeBlock(_ label: String, _ minutes: Int) -> some View {
        VStack(spacing: 4) {
            Text("\(minutes)")
                .font(.system(size: 32, weight: .semibold, design: .rounded))
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private func confidenceColor(_ level: String) -> Color {
        switch level {
        case "high": .green
        case "medium": .orange
        default: .red
        }
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.red)
            Text("Estimation failed")
                .font(.title2.bold())
            Text(message)
                .foregroundStyle(.secondary)
            Button("Retry") {
                Task {
                    await viewModel.createEstimate(gameId: gameId, profile: profile)
                }
            }
            .buttonStyle(.bordered)
        }
    }
}

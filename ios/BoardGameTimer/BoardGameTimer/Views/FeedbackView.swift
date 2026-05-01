import SwiftUI

struct FeedbackView: View {
    @ObservedObject var viewModel: FeedbackViewModel
    let estimateId: String
    @Binding var path: NavigationPath

    var body: some View {
        Group {
            switch viewModel.state {
            case .editing, .submitting:
                formView

            case .submitted:
                submittedView

            case .error(let message):
                errorView(message)
            }
        }
        .navigationTitle("Feedback")
    }

    private var formView: some View {
        Form {
            Section("How long did it actually take?") {
                Stepper("Teach time: \(viewModel.actualTeachMinutes) min",
                        value: $viewModel.actualTeachMinutes, in: 1...120)
                Stepper("Play time: \(viewModel.actualPlayMinutes) min",
                        value: $viewModel.actualPlayMinutes, in: 1...480)
            }

            Section {
                HStack {
                    Text("Total")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(viewModel.actualTeachMinutes + viewModel.actualPlayMinutes) min")
                        .fontWeight(.medium)
                }
            }

            Section("Notes (optional)") {
                TextField("How did the session go?", text: $viewModel.notes, axis: .vertical)
                    .lineLimit(3)
            }

            Section {
                Button {
                    Task { await viewModel.submit(estimateId: estimateId) }
                } label: {
                    if viewModel.state == .submitting {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else {
                        Text("Submit Feedback")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(viewModel.state == .submitting)
            }
        }
    }

    private var submittedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(.green)
            Text("Thanks!")
                .font(.title.bold())
            Text("Your feedback helps improve future estimates.")
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button("Done") {
                path = NavigationPath()
            }
            .buttonStyle(.borderedProminent)
            .padding(.top)
        }
        .padding()
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.red)
            Text("Submission failed")
                .font(.title2.bold())
            Text(message)
                .foregroundStyle(.secondary)
            Button("Retry") {
                Task { await viewModel.submit(estimateId: estimateId) }
            }
            .buttonStyle(.bordered)
        }
    }
}

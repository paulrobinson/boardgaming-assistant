import SwiftUI

struct ScanView: View {
    @ObservedObject var viewModel: ScanViewModel
    @Binding var path: NavigationPath

    var body: some View {
        VStack(spacing: 24) {
            switch viewModel.state {
            case .idle:
                scanPrompt

            case .scanning:
                ProgressView("Scanning barcode...")

            case .resolving:
                ProgressView("Looking up game...")

            case .resolved(let scan):
                resolvedView(scan)

            case .unsupported:
                unsupportedView

            case .error(let message):
                errorView(message)
            }
        }
        .padding()
        .navigationTitle("Scan")
        .task {
            if viewModel.state == .idle {
                await viewModel.startScan()
            }
        }
    }

    private var scanPrompt: some View {
        VStack(spacing: 16) {
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Point your camera at the game's barcode")
                .foregroundStyle(.secondary)
        }
    }

    private func resolvedView(_ scan: ScanResponse) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.green)
            Text("Game found!")
                .font(.title2.bold())
            Button("Continue") {
                path.append(Route.gameConfirmation(scan))
            }
            .buttonStyle(.borderedProminent)
        }
        .onAppear {
            path.append(Route.gameConfirmation(scan))
        }
    }

    private var unsupportedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "xmark.circle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.orange)
            Text("Game Not Supported")
                .font(.title2.bold())
            Text("This game isn't in our catalog yet.")
                .foregroundStyle(.secondary)
            Button("Try Another") {
                viewModel.reset()
                Task { await viewModel.startScan() }
            }
            .buttonStyle(.bordered)
        }
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48))
                .foregroundStyle(.red)
            Text("Something went wrong")
                .font(.title2.bold())
            Text(message)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.reset()
                Task { await viewModel.startScan() }
            }
            .buttonStyle(.bordered)
        }
    }
}

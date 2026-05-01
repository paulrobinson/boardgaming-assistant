import SwiftUI
import VisionKit

struct ScanView: View {
    @ObservedObject var viewModel: ScanViewModel
    @Binding var path: NavigationPath

    var body: some View {
        ZStack {
            switch viewModel.state {
            case .idle:
                scanPrompt

            case .scanning:
                if #available(iOS 16.0, *),
                   let visionScanner = viewModel.visionKitScanner {
                    cameraView(scanner: visionScanner)
                } else {
                    ProgressView("Scanning barcode...")
                }

            case .resolving:
                Color.black.ignoresSafeArea()
                ProgressView("Looking up game...")
                    .tint(.white)

            case .resolved(let scan):
                resolvedView(scan)

            case .unsupported:
                unsupportedView

            case .error(let message):
                errorView(message)
            }
        }
        .navigationTitle("Scan")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if viewModel.state == .idle {
                await viewModel.startScan()
            }
        }
    }

    @available(iOS 16.0, *)
    private func cameraView(scanner: VisionKitBarcodeScanner) -> some View {
        ZStack {
            BarcodeScannerView(scanner: scanner)
                .ignoresSafeArea()

            VStack {
                Spacer()

                VStack(spacing: 12) {
                    Text("Point camera at barcode")
                        .font(.headline)
                        .foregroundStyle(.white)

                    Text("UPC, EAN, or Code 128 barcodes")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.8))
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.bottom, 48)
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

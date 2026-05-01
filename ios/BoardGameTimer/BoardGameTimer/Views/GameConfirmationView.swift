import SwiftUI

struct GameConfirmationView: View {
    let scan: ScanResponse
    @Binding var path: NavigationPath

    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "puzzlepiece.fill")
                .font(.system(size: 56))
                .foregroundStyle(.tint)

            Text(scan.name ?? "Unknown Game")
                .font(.title.bold())

            VStack(spacing: 8) {
                detailRow("Official play time", "\(scan.officialPlayTimeMinutes) min")
                detailRow("Players", "\(scan.minPlayers)–\(scan.maxPlayers)")
            }
            .padding()
            .background(.quaternary.opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            Spacer()

            Button {
                if let gameId = scan.gameId {
                    path.append(Route.groupProfile(gameId: gameId))
                }
            } label: {
                Text("Set Up Group")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, 32)
        }
        .padding()
        .navigationTitle("Game Found")
    }

    private func detailRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}

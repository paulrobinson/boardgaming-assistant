import SwiftUI

struct HomeView: View {
    @Binding var path: NavigationPath

    var body: some View {
        VStack(spacing: 32) {
            Spacer()

            Image(systemName: "dice.fill")
                .font(.system(size: 72))
                .foregroundStyle(.accent)

            Text("Board Game\nSession Timer")
                .font(.largeTitle.bold())
                .multilineTextAlignment(.center)

            Text("Scan a game's barcode to estimate\nhow long your session will take.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Spacer()

            Button {
                path.append(Route.scan)
            } label: {
                Label("Scan a Game", systemImage: "barcode.viewfinder")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .padding(.horizontal, 32)
            .padding(.bottom, 48)
        }
        .navigationTitle("")
    }
}

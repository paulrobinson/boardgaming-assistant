import SwiftUI

struct GroupProfileFormView: View {
    let gameId: String
    @Binding var path: NavigationPath

    @State private var playerCount = 3
    @State private var familiarity: GroupFamiliarity = .mixed
    @State private var pace: TurnPace = .average
    @State private var analysis: AnalysisStyle = .moderate
    @State private var childrenIncluded = false
    @State private var notes = ""

    var body: some View {
        Form {
            Section("Players") {
                Stepper("Player count: \(playerCount)", value: $playerCount, in: 1...10)
                Toggle("Children playing", isOn: $childrenIncluded)
            }

            Section("Group Experience") {
                Picker("Familiarity", selection: $familiarity) {
                    ForEach(GroupFamiliarity.allCases) { f in
                        Text(f.label).tag(f)
                    }
                }
            }

            Section("Play Style") {
                Picker("Turn pace", selection: $pace) {
                    ForEach(TurnPace.allCases) { p in
                        Text(p.label).tag(p)
                    }
                }
                Picker("Analysis style", selection: $analysis) {
                    ForEach(AnalysisStyle.allCases) { a in
                        Text(a.label).tag(a)
                    }
                }
            }

            Section("Notes (optional)") {
                TextField("Anything else?", text: $notes, axis: .vertical)
                    .lineLimit(3)
            }

            Section {
                Button {
                    let profile = GroupProfileDto(
                        playerCount: playerCount,
                        groupFamiliarity: familiarity.rawValue,
                        turnPace: pace.rawValue,
                        analysisStyle: analysis.rawValue,
                        childrenIncluded: childrenIncluded,
                        notes: notes.isEmpty ? nil : notes)
                    path.append(Route.estimate(gameId: gameId, profile: profile))
                } label: {
                    Text("Get Estimate")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
            }
        }
        .navigationTitle("Group Profile")
    }
}

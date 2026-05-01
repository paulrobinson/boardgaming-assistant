import SwiftUI

/// Large card displaying total session time estimate
struct TimeEstimateCard: View {
    let totalMinutes: Int
    let teachMinutes: Int
    let playMinutes: Int

    var body: some View {
        VStack(spacing: 16) {
            // Total time - hero number
            VStack(spacing: 4) {
                Text("\(totalMinutes)")
                    .font(.system(size: 72, weight: .bold, design: .rounded))
                    .foregroundStyle(.primary)
                Text("minutes total")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            // Breakdown
            HStack(spacing: 24) {
                TimeBreakdownItem(label: "Teach", minutes: teachMinutes, icon: "person.2.fill")
                Divider()
                    .frame(height: 40)
                TimeBreakdownItem(label: "Play", minutes: playMinutes, icon: "gamecontroller.fill")
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 24)
        .frame(maxWidth: .infinity)
        .background(.quaternary.opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

/// Individual time breakdown item (teach or play)
struct TimeBreakdownItem: View {
    let label: String
    let minutes: Int
    let icon: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(.secondary)

            Text("\(minutes) min")
                .font(.title2.bold())
                .foregroundStyle(.primary)

            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Confidence level badge
struct ConfidenceBadge: View {
    let confidence: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.body)

            Text("Confidence:")
                .foregroundStyle(.secondary)

            Text(confidence.capitalized)
                .fontWeight(.semibold)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(backgroundColor)
        .foregroundStyle(foregroundColor)
        .clipShape(Capsule())
    }

    private var icon: String {
        switch confidence.lowercased() {
        case "high": "checkmark.circle.fill"
        case "medium": "circle.fill"
        default: "questionmark.circle.fill"
        }
    }

    private var backgroundColor: Color {
        switch confidence.lowercased() {
        case "high": .green.opacity(0.15)
        case "medium": .orange.opacity(0.15)
        default: .red.opacity(0.15)
        }
    }

    private var foregroundColor: Color {
        switch confidence.lowercased() {
        case "high": .green
        case "medium": .orange
        default: .red
        }
    }
}

/// Risk note item with warning icon
struct RiskNoteItem: View {
    let note: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.body)
                .foregroundStyle(.orange)

            Text(note)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            Spacer(minLength: 0)
        }
        .padding(12)
        .background(.orange.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

// MARK: - Previews

#Preview("Time Estimate Card") {
    TimeEstimateCard(totalMinutes: 90, teachMinutes: 24, playMinutes: 66)
        .padding()
}

#Preview("Confidence Badges") {
    VStack(spacing: 12) {
        ConfidenceBadge(confidence: "high")
        ConfidenceBadge(confidence: "medium")
        ConfidenceBadge(confidence: "low")
    }
    .padding()
}

#Preview("Risk Notes") {
    VStack(spacing: 8) {
        RiskNoteItem(note: "Rules reminders may increase downtime")
        RiskNoteItem(note: "First-time players will need extra explanation for trading mechanics")
    }
    .padding()
}

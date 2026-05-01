import SwiftUI

/// Displays player count fit information as colored chips
struct PlayerCountFitView: View {
    let playerCountFit: [PlayerCountFitDto]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Player Count Fit")
                .font(.headline)

            FlowLayout(spacing: 8) {
                ForEach(playerCountFit, id: \.playerCount) { fit in
                    PlayerCountChip(fit: fit)
                }
            }
        }
    }
}

/// Individual chip showing player count and fit level
struct PlayerCountChip: View {
    let fit: PlayerCountFitDto

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: personIcon)
                .font(.caption)
            Text("\(fit.playerCount)")
                .font(.subheadline.bold())
            Text(fit.fit.capitalized)
                .font(.caption)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(backgroundColor)
        .foregroundStyle(foregroundColor)
        .clipShape(Capsule())
    }

    private var personIcon: String {
        switch fit.playerCount {
        case 1: "person.fill"
        case 2: "person.2.fill"
        default: "person.3.fill"
        }
    }

    private var backgroundColor: Color {
        switch fit.fit.lowercased() {
        case "best": .green.opacity(0.2)
        case "great", "good": .blue.opacity(0.2)
        case "ok", "okay": .orange.opacity(0.2)
        default: .gray.opacity(0.2)
        }
    }

    private var foregroundColor: Color {
        switch fit.fit.lowercased() {
        case "best": .green
        case "great", "good": .blue
        case "ok", "okay": .orange
        default: .gray
        }
    }
}

/// Simple flow layout for wrapping chips
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            subview.place(at: CGPoint(x: bounds.minX + result.frames[index].minX,
                                     y: bounds.minY + result.frames[index].minY),
                         proposal: .unspecified)
        }
    }

    struct FlowResult {
        var frames: [CGRect] = []
        var size: CGSize = .zero

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > maxWidth && currentX > 0 {
                    // Move to next line
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                frames.append(CGRect(x: currentX, y: currentY, width: size.width, height: size.height))
                lineHeight = max(lineHeight, size.height)
                currentX += size.width + spacing
            }

            self.size = CGSize(
                width: maxWidth,
                height: currentY + lineHeight
            )
        }
    }
}

// MARK: - Previews

#Preview("Player Count Fit - Best") {
    PlayerCountFitView(playerCountFit: [
        PlayerCountFitDto(playerCount: 3, fit: "good"),
        PlayerCountFitDto(playerCount: 4, fit: "best"),
        PlayerCountFitDto(playerCount: 5, fit: "okay")
    ])
    .padding()
}

#Preview("Player Count Chips") {
    VStack(spacing: 16) {
        PlayerCountChip(fit: PlayerCountFitDto(playerCount: 4, fit: "best"))
        PlayerCountChip(fit: PlayerCountFitDto(playerCount: 3, fit: "good"))
        PlayerCountChip(fit: PlayerCountFitDto(playerCount: 2, fit: "okay"))
        PlayerCountChip(fit: PlayerCountFitDto(playerCount: 5, fit: "poor"))
    }
    .padding()
}

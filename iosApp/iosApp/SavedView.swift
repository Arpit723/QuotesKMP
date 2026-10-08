import SwiftUI
import SharedLogic

struct SavedView: View {

    @StateObject private var observable = SavedObservable()

    var body: some View {
        SavedScreenContent(
            uiState: observable.state,
            onDelete: observable.delete,
            onRetry: observable.retry
        )
        .task {
            observable.startObserving()
        }
        .onDisappear {
            observable.stopObserving()
        }
    }
}

struct SavedScreenContent: View {

    let uiState: SavedUiState
    let onDelete: (Int64) -> Void
    let onRetry: () -> Void

    var body: some View {
        Group {
            switch onEnum(of: uiState) {
            case .loading:
                ProgressView()
            case .empty:
                emptyView()
            case .content(let content):
                contentView(quotes: content.quotes)
            case .error(let failure):
                errorView(message: failure.message)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func emptyView() -> some View {
        VStack(spacing: 20) {
            Text("No saved quotes yet")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
    }

    private func contentView(quotes: [Quote]) -> some View {
        List {
            ForEach(quotes, id: \.id) { quote in
                VStack(alignment: .leading, spacing: 12) {
                    Text(quote.text)
                        .font(.title3)
                        .multilineTextAlignment(.leading)
                    Text(quote.author)
                        .font(.headline)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 8)
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        onDelete(quote.id)
                    } label: {
                        Label("Delete quote", systemImage: "trash")
                    }
                    .accessibilityLabel("Delete quote")
                }
            }
        }
        .listStyle(.plain)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 20) {
            Text(message)
                .font(.body)
                .multilineTextAlignment(.center)
            Button("Retry") {
                onRetry()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
    }
}

#Preview {
    SavedScreenContent(uiState: SavedUiStateLoading.shared, onDelete: { _ in }, onRetry: {})
}

#Preview {
    SavedScreenContent(uiState: SavedUiStateEmpty.shared, onDelete: { _ in }, onRetry: {})
}

#Preview {
    SavedScreenContent(
        uiState: SavedUiStateContent(quotes: [
            Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            Quote(id: 2, text: "The only way to do great work is to love what you do.", author: "Steve Jobs")
        ]),
        onDelete: { _ in },
        onRetry: {}
    )
}

#Preview {
    SavedScreenContent(uiState: SavedUiStateError(message: "Failed to load saved quotes"), onDelete: { _ in }, onRetry: {})
}
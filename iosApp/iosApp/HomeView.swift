import SwiftUI
import SharedLogic

struct HomeView: View {

    @StateObject private var observable = HomeObservable()

    var body: some View {
        HomeScreenContent(
            uiState: observable.state,
            onNewQuote: observable.loadNewQuote,
            onToggleSave: observable.toggleSave
        )
        .task {
            observable.startObserving()
        }
        .onDisappear {
            observable.stopObserving()
        }
    }
}

struct HomeScreenContent: View {

    let uiState: HomeUiState
    let onNewQuote: () -> Void
    let onToggleSave: () -> Void

    var body: some View {
        Group {
            switch onEnum(of: uiState) {
            case .loading:
                ProgressView()
            case .success(let success):
                successView(quote: success.quote, isSaved: success.isSaved, isOffline: success.isOffline)
            case .error(let failure):
                errorView(message: failure.message)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func successView(quote: Quote, isSaved: Bool, isOffline: Bool) -> some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 12) {
                Text(quote.text)
                    .font(.title3)
                    .multilineTextAlignment(.center)
                Text(quote.author)
                    .font(.headline)
                    .foregroundColor(.secondary)
            }
            .padding(20)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.secondarySystemBackground))
            )

            if isOffline {
                Text("Offline - showing a saved quote")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 16) {
                Button(action: onToggleSave) {
                    Image(systemName: isSaved ? "heart.fill" : "heart")
                        .font(.title2)
                        .foregroundColor(isSaved ? .red : .primary)
                }
                .accessibilityLabel(isSaved ? "Remove from saved" : "Save quote")

                Button("New Quote") {
                    onNewQuote()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(24)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 20) {
            Text(message)
                .font(.body)
                .multilineTextAlignment(.center)
            Button("Retry") {
                onNewQuote()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
    }
}

#Preview {
    HomeScreenContent(uiState: HomeUiStateLoading.shared, onNewQuote: {}, onToggleSave: {})
}

#Preview {
    HomeScreenContent(
        uiState: HomeUiStateSuccess(
            quote: Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            isSaved: false,
            isOffline: false
        ),
        onNewQuote: {},
        onToggleSave: {}
    )
}

#Preview {
    HomeScreenContent(
        uiState: HomeUiStateSuccess(
            quote: Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            isSaved: true,
            isOffline: false
        ),
        onNewQuote: {},
        onToggleSave: {}
    )
}

#Preview {
    HomeScreenContent(
        uiState: HomeUiStateSuccess(
            quote: Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            isSaved: false,
            isOffline: true
        ),
        onNewQuote: {},
        onToggleSave: {}
    )
}

#Preview {
    HomeScreenContent(
        uiState: HomeUiStateSuccess(
            quote: Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            isSaved: true,
            isOffline: true
        ),
        onNewQuote: {},
        onToggleSave: {}
    )
}

#Preview {
    HomeScreenContent(uiState: HomeUiStateError(message: "Failed to load quote"), onNewQuote: {}, onToggleSave: {})
}
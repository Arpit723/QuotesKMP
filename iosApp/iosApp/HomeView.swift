import SwiftUI
import SharedLogic

struct HomeView: View {

    @StateObject private var observable = HomeObservable()

    var body: some View {
        HomeScreenContent(
            uiState: observable.state,
            onNewQuote: observable.loadNewQuote
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

    var body: some View {
        Group {
            switch onEnum(of: uiState) {
            case .loading:
                ProgressView()
            case .success(let success):
                successView(quote: success.quote)
            case .error(let failure):
                errorView(message: failure.message)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func successView(quote: Quote) -> some View {
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

            Button("New Quote") {
                onNewQuote()
            }
            .buttonStyle(.borderedProminent)
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
    HomeScreenContent(uiState: HomeUiStateLoading.shared, onNewQuote: {})
}

#Preview {
    HomeScreenContent(
        uiState: HomeUiStateSuccess(
            quote: Quote(id: 1, text: "Stay hungry, stay foolish.", author: "Steve Jobs"),
            isSaved: false,
            isOffline: false
        ),
        onNewQuote: {}
    )
}

#Preview {
    HomeScreenContent(uiState: HomeUiStateError(message: "Failed to load quote"), onNewQuote: {})
}

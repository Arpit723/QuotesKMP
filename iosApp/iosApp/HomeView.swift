import SwiftUI
import SharedLogic

struct HomeView: View {

    @StateObject private var observable = HomeObservable()

    var body: some View {
        Group {
            switch onEnum(of: observable.state) {
            case .loading:
                ProgressView()
            case .success(let success):
                successView(quote: success.quote)
            case .error(let failure):
                errorView(message: failure.message)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task {
            observable.startObserving()
        }
        .onDisappear {
            observable.stopObserving()
        }
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
                observable.loadNewQuote()
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
                observable.loadNewQuote()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
    }
}

import SwiftUI
import SharedLogic

@MainActor
final class HomeObservable: ObservableObject {

    @Published var state: HomeUiState

    private let viewModel: HomeViewModel
    private var observingTask: Task<Void, Never>?

    init() {
        let viewModel = KoinHelper.shared.homeViewModel()
        self.viewModel = viewModel
        self.state = viewModel.uiState.value
    }

    func startObserving() {
        observingTask = Task { [weak self] in
            guard let self else { return }
            for await state in self.viewModel.uiState {
                self.state = state
            }
        }
    }

    func stopObserving() {
        observingTask?.cancel()
        observingTask = nil
    }

    func loadNewQuote() {
        viewModel.loadNewQuote()
    }
}

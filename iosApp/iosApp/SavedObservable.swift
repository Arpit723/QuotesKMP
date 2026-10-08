import SwiftUI
import SharedLogic

@MainActor
final class SavedObservable: ObservableObject {

    @Published var state: SavedUiState

    private let viewModel: SavedViewModel
    private var observingTask: Task<Void, Never>?

    init() {
        let viewModel = KoinHelper.shared.savedViewModel()
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

    func delete(id: Int64) {
        viewModel.delete(id: id)
    }

    func retry() {
        viewModel.retry()
    }
}
import SwiftUI
import SharedLogic

@main
struct iOSApp: App {

    init() {
        KoinHelper.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            HomeView()
        }
    }
}

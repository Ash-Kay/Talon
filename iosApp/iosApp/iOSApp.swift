import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            KoinInitKt.doInitKoin()
            MainViewControllerKt.setupLogger()
            ContentView()
        }
    }
}

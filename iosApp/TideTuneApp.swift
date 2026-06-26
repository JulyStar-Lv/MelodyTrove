import SwiftUI
import TideTuneShared

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

@main
struct TideTuneApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .onOpenURL { url in
                    guard
                        let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                        let code = components.queryItems?.first(where: { $0.name == "code" })?.value,
                        let state = components.queryItems?.first(where: { $0.name == "state" })?.value
                    else {
                        return
                    }
                    MainViewControllerKt.handleOneDriveOAuthRedirect(code: code, state: state)
                }
        }
    }
}

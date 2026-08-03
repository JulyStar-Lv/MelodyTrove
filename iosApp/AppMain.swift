import AVFoundation
import MediaPlayer
import SwiftUI
import SharedKit

private final class AppDelegate: NSObject, UIApplicationDelegate {
    private var remoteCommandTargets: [(command: MPRemoteCommand, target: Any)] = []

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        configureAudioSession()
        configureRemoteCommands()
        return true
    }

    func applicationWillTerminate(_ application: UIApplication) {
        removeRemoteCommands()
        try? AVAudioSession.sharedInstance().setActive(
            false,
            options: .notifyOthersOnDeactivation
        )
        MainViewControllerKt.shutdownApplication()
    }

    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping @Sendable () -> Void
    ) {
        MainViewControllerKt.handleEventsForBackgroundURLSession(
            identifier: identifier,
            completionHandler: completionHandler
        )
    }

    private func configureAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(
                .playback,
                mode: .default,
                options: [.allowAirPlay, .allowBluetoothA2DP]
            )
            try session.setActive(true)
        } catch {
            // AVPlayer can still surface its own playback error through the shared controller.
        }
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.isEnabled = true
        center.pauseCommand.isEnabled = true
        center.stopCommand.isEnabled = true
        center.togglePlayPauseCommand.isEnabled = true
        center.nextTrackCommand.isEnabled = true
        center.previousTrackCommand.isEnabled = true
        center.skipForwardCommand.isEnabled = true
        center.skipBackwardCommand.isEnabled = true
        center.changePlaybackPositionCommand.isEnabled = true
        center.skipForwardCommand.preferredIntervals = [10]
        center.skipBackwardCommand.preferredIntervals = [10]

        register(center.playCommand) { _ in
            commandStatus(MainViewControllerKt.handlePlaybackPlayCommand())
        }
        register(center.pauseCommand) { _ in
            commandStatus(MainViewControllerKt.handlePlaybackPauseCommand())
        }
        register(center.stopCommand) { _ in
            // Keep the current item, queue, and position so playback can resume.
            commandStatus(MainViewControllerKt.handlePlaybackStopCommand())
        }
        register(center.togglePlayPauseCommand) { _ in
            commandStatus(MainViewControllerKt.handlePlaybackToggleCommand())
        }
        register(center.nextTrackCommand) { _ in
            commandStatus(MainViewControllerKt.handlePlaybackNextCommand())
        }
        register(center.previousTrackCommand) { _ in
            commandStatus(MainViewControllerKt.handlePlaybackPreviousCommand())
        }
        register(center.skipForwardCommand) { event in
            let seconds = (event as? MPSkipIntervalCommandEvent)?.interval ?? 10
            return commandStatus(
                MainViewControllerKt.handlePlaybackSeekByCommand(
                    deltaMs: Int64(seconds * 1_000)
                )
            )
        }
        register(center.skipBackwardCommand) { event in
            let seconds = (event as? MPSkipIntervalCommandEvent)?.interval ?? 10
            return commandStatus(
                MainViewControllerKt.handlePlaybackSeekByCommand(
                    deltaMs: -Int64(seconds * 1_000)
                )
            )
        }
        register(center.changePlaybackPositionCommand) { event in
            guard let positionEvent = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            return commandStatus(
                MainViewControllerKt.handlePlaybackSeekToCommand(
                    positionMs: Int64(positionEvent.positionTime * 1_000)
                )
            )
        }
    }

    private func register(
        _ command: MPRemoteCommand,
        handler: @escaping (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus
    ) {
        let target = command.addTarget(handler: handler)
        remoteCommandTargets.append((command, target))
    }

    private func removeRemoteCommands() {
        remoteCommandTargets.forEach { entry in
            entry.command.removeTarget(entry.target)
        }
        remoteCommandTargets.removeAll()
    }
}

private func commandStatus(_ handled: Bool) -> MPRemoteCommandHandlerStatus {
    handled ? .success : .noSuchContent
}

private final class KeyboardShortcutHostController: UIViewController {
    private let contentController: UIViewController

    init(contentController: UIViewController) {
        self.contentController = contentController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(contentController)
        contentController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(contentController.view)
        NSLayoutConstraint.activate([
            contentController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentController.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        contentController.didMove(toParent: self)
    }

    override var canBecomeFirstResponder: Bool { true }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        becomeFirstResponder()
    }

    override var keyCommands: [UIKeyCommand]? {
        [
            shortcut(" ", [], #selector(togglePlayPause), "Play or pause"),
            shortcut(UIKeyCommand.inputLeftArrow, .alternate, #selector(previousTrack), "Previous track"),
            shortcut(UIKeyCommand.inputRightArrow, .alternate, #selector(nextTrack), "Next track"),
            shortcut(UIKeyCommand.inputLeftArrow, .command, #selector(seekBackward), "Seek backward 10 seconds"),
            shortcut(UIKeyCommand.inputRightArrow, .command, #selector(seekForward), "Seek forward 10 seconds"),
            shortcut(UIKeyCommand.inputLeftArrow, [.command, .shift], #selector(seekBackwardLong), "Seek backward 30 seconds"),
            shortcut(UIKeyCommand.inputRightArrow, [.command, .shift], #selector(seekForwardLong), "Seek forward 30 seconds"),
            shortcut(UIKeyCommand.inputEscape, [], #selector(navigateBack), "Back"),
        ]
    }

    private func shortcut(
        _ input: String,
        _ modifiers: UIKeyModifierFlags,
        _ action: Selector,
        _ title: String
    ) -> UIKeyCommand {
        let command = UIKeyCommand(input: input, modifierFlags: modifiers, action: action)
        command.discoverabilityTitle = title
        return command
    }

    private var isEditingText: Bool {
        guard let responder = view.window?.firstResponderView else { return false }
        return responder is UITextField || responder is UITextView
    }

    @objc private func togglePlayPause() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackToggleCommand()
    }

    @objc private func previousTrack() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackPreviousCommand()
    }

    @objc private func nextTrack() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackNextCommand()
    }

    @objc private func seekBackward() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackSeekByCommand(deltaMs: -10_000)
    }

    @objc private func seekForward() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackSeekByCommand(deltaMs: 10_000)
    }

    @objc private func seekBackwardLong() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackSeekByCommand(deltaMs: -30_000)
    }

    @objc private func seekForwardLong() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackSeekByCommand(deltaMs: 30_000)
    }

    @objc private func navigateBack() {
        guard !isEditingText else { return }
        _ = MainViewControllerKt.handlePlaybackBackCommand()
    }
}

private extension UIView {
    var firstResponderView: UIView? {
        if isFirstResponder {
            return self
        }
        for subview in subviews {
            if let responder = subview.firstResponderView {
                return responder
            }
        }
        return nil
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        KeyboardShortcutHostController(
            contentController: MainViewControllerKt.MainViewController()
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

@main
struct AppMain: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

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

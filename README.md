This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

## MIDI Roadmap / TODO

Current status:
- [x] Android USB MIDI transport works.
- [x] Notes from connected MIDI device are displayed on Home screen.

### Cross-platform MIDI support

Android:
- [x] USB MIDI transport (`midi-transport-usb`) with note events.
- [ ] Add robust reconnect flow for USB device detach/attach.
- [ ] Add user-facing error UI (not only logs/toasts) for connect/scan failures.

iOS:
- [ ] Implement iOS transport module (CoreMIDI) with device discovery.
- [ ] Add connect/disconnect flow and stream incoming MIDI messages.
- [ ] Convert incoming MIDI messages to shared `NoteEvent` format.
- [ ] Integrate transport into DI as `MidiInputService` for iOS target.
- [ ] Add iOS permissions and runtime handling (Bluetooth/USB where needed).

Web (JS/Wasm):
- [ ] Implement Web transport using Web MIDI API (`navigator.requestMIDIAccess`).
- [ ] Add device selection and input subscription in browser.
- [ ] Map Web MIDI messages to shared `NoteEvent`.
- [ ] Add browser capability detection and unsupported-state UX.
- [ ] Define HTTPS/secure-context requirements in docs.

Desktop (JVM):
- [ ] Implement Desktop transport via Java MIDI API (`javax.sound.midi`) or selected library.
- [ ] Add input device discovery and connection management.
- [ ] Map incoming messages to shared `NoteEvent`.
- [ ] Add fallback/unsupported behavior per OS if required.

Architecture/cleanup:
- [ ] Finish migration of `bluetooth-midi` to `midi-core` contracts (remove remaining duplicated service/error contracts).
- [ ] Add transport selector (`USB` / `Bluetooth` / `Auto`) in Settings.
- [ ] Add integration tests for parser + note-state reducer (`midi-core`).

### Piano Roll screen (visual note rendering)

Functional tasks:
- [x] Create separate KMP feature module `feature-piano-roll-ui`.
- [x] Add new screen route (`Node Visualizer`) and navigation from Home screen.
- [x] Add basic live keyboard rendering from `MidiInputService.noteEvents`.
- [x] Keep active notes state (currently pressed notes).
- [ ] Keep short event history for falling notes/timeline visualization.

UI tasks:
- [x] Draw keyboard lanes (12 semitones repeated by octave).
- [x] Highlight currently pressed keys in real time.
- [ ] Add finalized visual state/color for correct pressed key.
- [ ] Add finalized visual state/color for wrong pressed key.
- [ ] Add finalized visual state/color for target key (must press).
- [ ] Set default visible keyboard range to exactly `F3..E5` (2 octaves).
- [ ] Add left arrow control under keyboard (shift visible range by 1 octave left).
- [ ] Add right arrow control under keyboard (shift visible range by 1 octave right).
- [ ] Render note blocks on a scrolling timeline (piano-roll style).
- [ ] Add velocity-based color/intensity for note blocks.
- [ ] Add device/channel filters and legend in UI.

Performance/UX tasks:
- [ ] Throttle/reduce recompositions for high-frequency MIDI streams.
- [ ] Use canvas-based rendering for smooth animation.
- [ ] Add frame-time profiling and optimize for low-end devices.
- [ ] Add pause/clear/history controls for debugging sessions.

<p align="center">
  <!-- TODO: Add app logo here -->
  <!-- <img src="readme/logo.png" width="120" alt="Talon Logo" /> -->
  <h1 align="center">🦅 Talon</h1>
  <p align="center"><b>An open-source, AI-powered autonomous agent that operates your Android device.</b></p>
  <p align="center">Tell it what to do in plain English — Talon navigates apps, clicks buttons, types text, and completes tasks on your behalf.</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.3.0-purple?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Compose_Multiplatform-1.10.1-blue?logo=jetpackcompose" alt="Compose Multiplatform" />
</p>

---

## ✨ What is Talon?

Talon is an **autonomous AI agent** that lives on your phone. Instead of using screenshots or screen
recording, Talon leverages Android's **Accessibility Service** to read the device's live UI tree — a
structured representation of every element on screen. It feeds this tree to an LLM (Large Language
Model) which decides what action to take next: click a button, type text, scroll, launch an app, or
navigate back.

**Think of it as an AI assistant that can actually _use_ your phone.**

### How it works — The Agent Loop

```
┌─────────────────────────────────────────────────────┐
│                    User gives a goal                │
│              "Order coffee on Starbucks app"         │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
        ┌─────────────────┐
        │    OBSERVE       │  ← Capture UI tree via Accessibility Service
        │  (get_screen)    │    Returns structured JSON of all visible elements
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │     THINK        │  ← LLM analyzes the screen
        │   (AI decides)   │    Identifies the next micro-step
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │      ACT         │  ← Execute one action
        │  (click/type/    │    click(3), type_text(5, "latte"),
        │   scroll/back)   │    scroll(7, DOWN), go_back()
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │     VERIFY       │  ← Capture screen again
        │  (get_screen)    │    Confirm action succeeded
        └────────┬────────┘
                 │
                 ▼
           ┌───────────┐
           │ Goal met?  │──── Yes ──→ ✅ Task Complete
           └─────┬─────┘
                 │ No
                 └──────────→ 🔄 Loop back to OBSERVE
```

### Key Design Decisions

- **No screenshots / screen recording** — Talon uses the Accessibility Service to read a parsed UI
  tree. This is faster, cheaper (no vision API calls), and more reliable than sending screenshots to
  an LLM.
- **Self-filtering overlay** — Talon's own overlay UI is automatically excluded from the UI tree
  capture (`node.packageName == OWN_PACKAGE` → skip), so the agent never tries to interact with
  Talon's own interface.
- **Chat-based UX** — Conversations with the agent look and feel like a messaging app. Each tool
  use, status update, and AI reply appears as a chat bubble.
- **Continue completed tasks** — You can send follow-up messages in a completed session to give the
  agent new instructions with prior context.

---

## 🏗 Architecture

Talon follows **Clean Architecture** with **MVI (Model-View-Intent)** state management
via [Orbit-MVI](https://github.com/orbit-mvi/orbit-mvi).

```
┌──────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │ TasksScreen  │  │SessionDetailScreen│  │ SettingsScreen  │ │
│  │ (Chat list)  │  │  (Chat interface) │  │  (Config)       │ │
│  └──────┬──────┘  └────────┬─────────┘  └────────┬────────┘ │
│         │                  │                      │          │
│         │    ┌─────────────┴──────────────┐       │          │
│         │    │ SessionDetailViewModel      │       │          │
│         │    │ (Orbit ContainerHost)       │       │          │
│         │    └─────────────┬──────────────┘       │          │
├─────────┼──────────────────┼──────────────────────┼──────────┤
│         │           Domain Layer                  │          │
│         │    ┌─────────────┴──────────────┐       │          │
│         │    │     RunAgentUseCase         │       │          │
│         │    │ (orchestrates agent run)    │       │          │
│         │    └─────────────┬──────────────┘       │          │
├─────────┼──────────────────┼──────────────────────┼──────────┤
│         │            Data Layer                   │          │
│  ┌──────┴──────┐ ┌────────┴────────┐ ┌───────────┴────────┐ │
│  │SessionRepo  │ │SettingsRepo     │ │  DeviceController   │ │
│  │(Room DB)    │ │(Multiplatform   │ │  (Accessibility     │ │
│  │             │ │ Settings)       │ │   Service bridge)   │ │
│  └─────────────┘ └─────────────────┘ └─────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Project Structure

```
composeApp/
├── src/
│   ├── commonMain/kotlin/io/ashkay/talon/
│   │   ├── agent/                    # AI agent core
│   │   │   ├── RunAgentUseCase.kt    # Orchestrates agent execution
│   │   │   ├── TalonAgentStrategy.kt # Koog state machine graph
│   │   │   ├── LlmProvider.kt       # Supported LLM providers enum
│   │   │   ├── AgentRunStatus.kt     # Agent lifecycle states
│   │   │   └── tools/               # LLM-callable tools
│   │   │       ├── GetScreenTool.kt      # Captures UI tree
│   │   │       ├── ClickTool.kt          # Clicks a UI node
│   │   │       ├── TypeTextTool.kt       # Types text into a node
│   │   │       ├── ScrollTool.kt         # Scrolls a node
│   │   │       ├── GoBackTool.kt         # Android back button
│   │   │       ├── LaunchAppTool.kt      # Opens an app by package
│   │   │       ├── GetInstalledAppsTool.kt # Lists installed apps
│   │   │       └── TalonToolRegistry.kt  # Registers all tools
│   │   ├── data/
│   │   │   ├── SettingsRepository.kt     # API keys, provider, onboarding
│   │   │   └── db/
│   │   │       ├── Entities.kt           # Room entities (sessions, logs)
│   │   │       ├── Dao.kt               # Room DAOs
│   │   │       ├── SessionRepository.kt  # Session & log operations
│   │   │       └── TalonDatabase.kt     # Room database definition
│   │   ├── di/
│   │   │   └── Koin.kt                  # Koin DI modules
│   │   ├── model/
│   │   │   ├── UiNode.kt                # UI tree data model
│   │   │   ├── AgentCommand.kt          # Click, Type, Scroll, etc.
│   │   │   └── AppInfo.kt               # Installed app info
│   │   ├── navigation/
│   │   │   └── Destinations.kt          # Type-safe navigation routes
│   │   ├── platform/
│   │   │   ├── DeviceController.kt      # Platform interface
│   │   │   └── OverlayUiController.kt   # Overlay interface
│   │   ├── ui/
│   │   │   ├── tasks/
│   │   │   │   ├── TasksScreen.kt           # Session list (home)
│   │   │   │   ├── SessionDetailScreen.kt   # Chat interface
│   │   │   │   └── SessionDetailViewModel.kt
│   │   │   ├── settings/
│   │   │   │   └── SettingsScreen.kt        # LLM & permissions config
│   │   │   └── onboarding/
│   │   │       ├── OnboardingScreen.kt      # Setup wizard
│   │   │       └── steps/                   # Individual onboarding steps
│   │   ├── theme/
│   │   │   ├── Color.kt
│   │   │   └── Theme.kt
│   │   └── App.kt                    # Root composable & navigation
│   │
│   ├── androidMain/kotlin/io/ashkay/talon/
│   │   ├── MainActivity.kt              # Android entry point
│   │   ├── TalonApp.kt                  # Application class (Koin init)
│   │   ├── accessibility/
│   │   │   ├── TalonAccessibilityService.kt  # Core accessibility service
│   │   │   ├── UiTreeBuilder.kt              # AccessibilityNodeInfo → UiNode
│   │   │   ├── NodeFinder.kt                # Finds nodes by index
│   │   │   └── commands/                    # Click, Scroll, Type handlers
│   │   ├── platform/
│   │   │   ├── AndroidDeviceController.kt    # DeviceController impl
│   │   │   └── AndroidOverlayUiController.kt # Floating overlay UI
│   │   └── service/
│   │       └── AgentForegroundService.kt     # Keeps agent alive
│   │
│   └── iosMain/                        # iOS platform (WIP)
│
├── iosApp/                             # iOS app entry point
└── gradle/
    └── libs.versions.toml              # Centralized dependency versions
```

---

## 🤖 The AI Agent

### Powered by Koog

Talon uses [**Koog**](https://github.com/JetBrains/koog) — JetBrains' open-source Kotlin AI Agent
framework. Koog provides a type-safe DSL for defining agent strategies as state machine graphs.

The agent strategy is defined as a graph:

```
START → Set LLM Params → LLM Request ──→ FINISH (on assistant message)
                              │
                              └──→ Execute Tool → Send Tool Result ──→ FINISH (on assistant message)
                                                       │
                                                       └──→ Execute Tool (loop)
```

### Supported LLM Providers

| Provider          | Model             | Setup                                         |
|-------------------|-------------------|-----------------------------------------------|
| **Google Gemini** | Gemini 2.5 Flash  | [Get API key](https://aistudio.google.com/)   |
| **OpenAI**        | GPT-4o            | [Get API key](https://platform.openai.com/)   |
| **Anthropic**     | Claude 3.5 Sonnet | [Get API key](https://console.anthropic.com/) |
| **OpenRouter**    | Gemini 2.5 Flash  | [Get API key](https://openrouter.ai/)         |

### Agent Tools

The LLM can call these tools to interact with the device:

| Tool                 | Description                                                      |
|----------------------|------------------------------------------------------------------|
| `get_screen`         | Captures the current UI tree as a structured text representation |
| `click`              | Clicks a UI element by its node index                            |
| `type_text`          | Types text into an editable field by node index                  |
| `scroll`             | Scrolls a scrollable element (up/down/left/right)                |
| `go_back`            | Presses the Android back button                                  |
| `launch_app`         | Opens an app by its package name                                 |
| `get_installed_apps` | Lists all installed apps on the device                           |

### System Prompt & Edge Case Handling

The agent follows strict protocols for common edge cases:

- **Popup Ambush** — If an unexpected dialog appears (ads, "Rate Us", permissions), the agent
  dismisses it before continuing.
- **Keyboard Trap** — After typing, if the keyboard obscures buttons, the agent uses `go_back` to
  dismiss it first.
- **Anti-Looping** — If the screen doesn't change after an action, the agent tries a different
  approach instead of repeating.
- **No Hallucination** — The agent is forbidden from interacting with node indices that don't exist
  in the current UI tree.

---

## 📱 UI / UX

### Chat-Based Interface

Talon uses a **WhatsApp-style chat interface** for interacting with the agent:

<!-- TODO: Add chat UI screenshot -->
<!-- <img src="readme/chat_detail.png" width="300" /> -->

- **User messages** appear as right-aligned bubbles
- **Agent logs** (tool usage, status updates) appear as left-aligned bubbles with distinct colors
  per type:
    - 🔧 **Tool use** — shows what the agent did (clicked, typed, scrolled)
    - 🤖 **AI reply** — agent's final response
    - ℹ️ **Info** — status updates (started, provider info)
    - ❌ **Error** — error messages
    - 📋 **Summary** — task completion summary
- **Typing indicator** — animated dots show when the agent is actively working
- **Input disabled** while agent is running, re-enabled after completion for follow-up tasks

### Floating Overlay

When the agent is running, Talon minimizes and shows a **floating overlay** on top of other apps:

<!-- TODO: Add overlay screenshot -->
<!-- <img src="readme/overlay.png" width="300" /> -->

- Shows real-time agent logs
- **Stop button** to cancel the agent at any time
- Automatically excluded from UI tree capture (the agent never sees its own overlay)
- Requires `SYSTEM_ALERT_WINDOW` permission

### Onboarding

First-time users go through a guided setup wizard:

1. **Welcome** — Introduction to Talon
2. **LLM Provider** — Choose your AI provider
3. **API Key** — Enter your API key
4. **Accessibility** — Enable the Accessibility Service
5. **Overlay** — Grant "Display over other apps" permission
6. **Notifications** — Allow foreground service notifications

---

## 🛠 Tech Stack

| Layer                  | Technology                                                                          |
|------------------------|-------------------------------------------------------------------------------------|
| **UI**                 | [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) 1.10.1    |
| **State Management**   | [Orbit-MVI](https://github.com/orbit-mvi/orbit-mvi) 11.0.0                          |
| **AI Agent Framework** | [Koog](https://github.com/JetBrains/koog) 0.6.2 (JetBrains)                         |
| **Navigation**         | Jetpack Compose Navigation 2.9.2                                                    |
| **DI**                 | [Koin](https://insert-koin.io/) 4.1.1                                               |
| **Database**           | [Room](https://developer.android.com/training/data-storage/room) 2.8.4              |
| **Settings**           | [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings) 1.3.0 |
| **Logging**            | [Napier](https://github.com/AAkira/Napier) 2.7.1                                    |
| **Language**           | Kotlin 2.3.0 (Multiplatform)                                                        |
| **Targets**            | Android (primary), iOS (scaffolded)                                                 |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or newer (or IntelliJ IDEA with Android plugin)
- **JDK 11+**
- **Android device or emulator** running API 24+ (Android 7.0+)
- An **API key** from one of the supported LLM providers

### Clone & Build

```bash
git clone https://github.com/AshishKayastha/Talon.git
cd Talon
```

#### Android

```bash
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / IntelliJ IDEA.

#### iOS (WIP)

Open `iosApp/` in Xcode and run from there. Note: the iOS target is scaffolded but the agent
functionality (Accessibility Service) is Android-only.

### Setup on Device

1. **Install & launch** the app
2. **Complete onboarding**:
    - Select your LLM provider
    - Enter your API key
    - Enable the Accessibility Service (`Settings → Accessibility → Talon`)
    - Grant overlay ("Display over other apps") permission
    - Allow notifications
3. **Tap ➕** to create a new task
4. **Type your goal** in plain English (e.g., "Open Chrome and search for Kotlin Multiplatform")
5. **Watch the agent work** — Talon will minimize and show a floating overlay while it operates your
   device

---

## 🧪 Development Mode

Talon includes a **fake agent mode** for development and testing without consuming LLM API credits.

In `RunAgentUseCase.kt`:

```kotlin
const val USE_FAKE_AGENT = false  // Set to `true` to enable
```

When enabled, the fake agent simulates a run by:

1. Fetching installed apps
2. Launching Chrome
3. Capturing the screen
4. Simulating clicks and text input
5. Completing with a fake summary

This is useful for testing UI changes, overlay behavior, and the chat interface without making real
API calls.

---

## 🔒 Permissions

Talon requires the following Android permissions:

| Permission                       | Purpose                                  |
|----------------------------------|------------------------------------------|
| `INTERNET`                       | Communicate with LLM APIs                |
| `BIND_ACCESSIBILITY_SERVICE`     | Read UI tree & perform actions on screen |
| `SYSTEM_ALERT_WINDOW`            | Show floating overlay while agent runs   |
| `FOREGROUND_SERVICE`             | Keep agent alive in background           |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for Android 14+                 |
| `POST_NOTIFICATIONS`             | Show foreground service notification     |
| `QUERY_ALL_PACKAGES`             | List installed apps for the agent        |

> ⚠️ **Privacy Note**: The Accessibility Service can read all content on your screen. Talon **only**
> uses it when the agent is actively running a task. The UI tree is sent to the LLM provider you
> configured. No data is collected or sent anywhere else.

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create a branch** for your feature (`git checkout -b feature/my-feature`)
3. **Make your changes** following the project's architecture guidelines
4. **Test** with the fake agent mode (`USE_FAKE_AGENT = true`)
5. **Submit a pull request**

### Code Style

- The project uses [Spotless](https://github.com/diffplug/spotless)
  with [ktfmt](https://github.com/facebook/ktfmt) for formatting
- Run `./gradlew spotlessApply` before committing
- Follow the Orbit-MVI pattern for state management
- Use Koin for dependency injection
- All strings must be in `composeResources/values/strings.xml` (no hardcoded strings)

---

## 🙏 Acknowledgements

- [**Koog**](https://github.com/JetBrains/koog) by JetBrains — the Kotlin AI Agent framework
  powering Talon's brain
- [**Compose Multiplatform**](https://www.jetbrains.com/compose-multiplatform/) — declarative UI
  across platforms
- [**Orbit-MVI**](https://github.com/orbit-mvi/orbit-mvi) — predictable state management
- Inspired by [OpenClaw](https://github.com/nicepkg/OpenClaw) and similar autonomous agent projects

---

<p align="center">
  <b>⭐ Star this repo if you find it interesting!</b>
</p>

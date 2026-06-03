<div align="center">

# JavaVisualizer

**Live JavaFX UI preview inside VS Code**

*See and interact with your Java desktop app in real-time — no window switching needed.*

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![VS Code Marketplace](https://img.shields.io/badge/VS%20Code-Marketplace-007ACC?logo=visual-studio-code)](https://github.com/SourisCG/JavaVisualizer)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)

[Installation](#-installation) • [Usage](#-usage) • [Configuration](#-configuration) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## What is JavaVisualizer?

JavaVisualizer is a VS Code extension that lets you **see and interact with your JavaFX application** directly inside a VS Code tab. Your app runs with its full logic — databases, events, business code — completely intact.

No mocking. No simulation. Your app, running live inside your editor.

## Features

### Live Preview
Open your JavaFX project and watch it run in real-time inside VS Code. Click buttons, type in fields, navigate menus — everything works exactly as it would natively.

### Manual Refresh
Hit the **Refresh** button in the preview toolbar to instantly kill, recompile, and relaunch your application.

### Auto-Reload
Toggle the **Auto-Reload** switch to automatically recompile and refresh the preview every time you save a `.java` file.

### Run on Desktop
Need the real native window? The **Run Desktop** button launches your app as a standalone desktop application — perfect for final testing.

### Smart Error Panel
Compilation errors, missing JDK, or missing main class — JavaVisualizer shows clear error messages with clickable links to open the problematic files.

### Zero External Dependencies
No extra software to install. JavaVisualizer uses your existing JDK and bundles everything else internally.

## Requirements

- **VS Code** 1.85+
- **JDK 17+** installed and `JAVA_HOME` set (or `java` in your PATH)
- A JavaFX project (plain Java, Maven, or Gradle)

## Installation

### From VS Code Marketplace
1. Open VS Code
2. Go to Extensions (`Ctrl+Shift+X`)
3. Search for **JavaVisualizer**
4. Click **Install**

### From Source
```bash
git clone https://github.com/SourisCG/JavaVisualizer.git
cd JavaVisualizer
npm install
npm run compile
npx @vscode/vsce package
code --install-extension javavisualizer-0.0.1.vsix
```

## Usage

### Quick Start
1. Open a JavaFX project folder in VS Code
2. Open the Command Palette (`Ctrl+Shift+P`)
3. Run: **JavaVisualizer: Open Preview**
4. The preview panel opens — your app compiles and launches inside VS Code

### Toolbar Controls

| Button | Action |
|--------|--------|
| **Refresh** | Kill, recompile, and relaunch the preview |
| **Auto-Reload** | Toggle automatic recompilation on `.java` file save |
| **Run Desktop** | Launch the app as a native desktop window |

### Keyboard Shortcuts
- `Ctrl+Shift+P` → **JavaVisualizer: Refresh Preview** — Manual refresh
- `Ctrl+Shift+P` → **JavaVisualizer: Run on Desktop** — Launch native window

## Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `javavisualizer.autoReload` | `false` | Auto-recompile on `.java` file save |
| `javavisualizer.frameRate` | `30` | Target FPS for the preview (5-60) |
| `javavisualizer.jpegQuality` | `75` | JPEG quality for frame compression (10-100) |
| `javavisualizer.javaHome` | `""` | Custom JDK path (empty = auto-detect) |
| `javavisualizer.mainClass` | `""` | Main class to run (empty = auto-detect) |

## Architecture

```
┌──────────────────────────────────────────────────┐
│            VS Code Extension (TypeScript)          │
│                                                    │
│  ┌──────────────┐       ┌───────────────────────┐ │
│  │   Webview     │       │   Process Manager     │ │
│  │               │       │                       │ │
│  │  ┌─────────┐  │       │  - javac / mvn / gradle│ │
│  │  │ Toolbar │  │       │  - java -javaagent:.. │ │
│  │  │ [⟳][⚡] │  │       │  - kill / restart     │ │
│  │  └─────────┘  │       │  - FileWatcher (.java)│ │
│  │  ┌─────────┐  │       └───────────┬───────────┘ │
│  │  │ <canvas>│  │                   │              │
│  │  │ (frames)│  │                   │              │
│  │  └─────────┘  │                   │              │
│  └───────┬───────┘                   │              │
│          │ WebSocket (binary+JSON)   │ spawn/kill   │
└──────────┼───────────────────────────┼──────────────┘
           │                           │
           ▼                           ▼
┌──────────────────────────────────────────────────────┐
│              Java Process (User's App)                │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │          JavaVisualizer Agent (fat JAR)         │  │
│  │                                                │  │
│  │  1. Intercepta Stage.show() con ByteBuddy      │  │
│  │  2. Mueve ventana off-screen (x:-10000)        │  │
│  │  3. Captura frames con Node.snapshot()         │  │
│  │  4. Envía JPEG por WebSocket (binario)         │  │
│  │  5. Recibe eventos JSON del Webview            │  │
│  │  6. Inyecta eventos en JavaFX Application Thread│ │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │          User's JavaFX Application              │  │
│  │          (lógica, BD, eventos: intactos)        │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### How It Works

1. **Compilation** — The extension compiles your Java project using your JDK (supports plain Java, Maven, and Gradle).
2. **Launch with Agent** — It launches your app with a bundled **Java Agent** that intercepts the JavaFX window.
3. **Frame Streaming** — The Agent captures frames from your app's SceneGraph and streams them to the Webview via WebSocket.
4. **Event Injection** — Your clicks and keystrokes in the Webview are sent back to the Agent, which injects them into your running application.

Your app runs **exactly** as it would natively — same JVM, same threads, same databases. Nothing is simulated or mocked.

## Building from Source

```bash
git clone https://github.com/SourisCG/JavaVisualizer.git
cd JavaVisualizer

npm install

cd agent && ./gradlew shadowJar && cd ..

npm run compile

npx @vscode/vsce package
```

## Troubleshooting

### "JDK Not Found"
- Ensure `java` is in your PATH or set `javavisualizer.javaHome` in settings
- Run `java -version` in terminal to verify your JDK is 17+

### "No Main Class Found"
- Set `javavisualizer.mainClass` in settings to your main class (e.g., `com.example.MainApp`)
- Or ensure your main class has a `public static void main(String[] args)` method

### Preview is laggy
- Lower `javavisualizer.frameRate` (e.g., 15-20 FPS)
- Lower `javavisualizer.jpegQuality` (e.g., 50-60)

### Blank preview
- Ensure your app creates and shows a JavaFX `Stage`
- Check the Output panel for JavaVisualizer logs

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [SourisCG](https://github.com/SourisCG)

[Report Bug](https://github.com/SourisCG/JavaVisualizer/issues) · [Request Feature](https://github.com/SourisCG/JavaVisualizer/issues)

</div>

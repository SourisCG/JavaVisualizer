# 🎨 JavaFX Live Preview

> Edit your FXML and CSSFX — see changes **instantly**.  
> No compilation, no waiting. Just save and watch.

## ✨ What it does

- Open any `.fxml` file
- **Hot-reload FXML** — save → reloads instantly
- **Hot-reload CSSFX** — save → styles update without reloading the scene
- **Switch between FXML files** — dropdown lists all views in your project
- **Responsive viewport presets** — Phone (375×667), Tablet (768×1024), Desktop, HD, Custom
- **Auto-reload toggle** — one click to pause/resume watching
- **Error overlay** — shows load errors with a clear red banner

## 🚀 Quick Start

```bash
# Open a specific FXML
java -jar javafx-live-preview.jar /path/to/login.fxml

# Or just pick one from the file chooser
java -jar javafx-live-preview.jar
```

Requires **JDK 17 or newer**. JavaFX is bundled in the JAR.

## 📥 Download

| Platform | |
|----------|-|
| **Windows** | [![.exe](https://img.shields.io/badge/Download-.exe-blue?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-windows-x64.exe) [![.msi](https://img.shields.io/badge/Download-.msi-blue?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-windows-x64.msi) |
| **macOS (Apple Silicon)** | [![.dmg](https://img.shields.io/badge/Download-.dmg-lightgrey?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-macos-arm64.dmg) |
| **macOS (Intel)** | [![.dmg](https://img.shields.io/badge/Download-.dmg-lightgrey?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-macos-x64.dmg) |
| **Linux (Debian/Ubuntu)** | [![.deb](https://img.shields.io/badge/Download-.deb-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-amd64.deb) |
| **Linux (Fedora/RHEL)** | [![.rpm](https://img.shields.io/badge/Download-.rpm-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x86_64.rpm) |
| **Linux (any distro)** | [![.tar.gz](https://img.shields.io/badge/Download-.tar.gz-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x64.tar.gz) |

### Installation Notes

**Windows**  
Double-click the `.exe` or `.msi` installer. No admin rights required.

**macOS**  
Open the `.dmg` and drag the app to Applications.  
⚠️ **First launch**: macOS will show "App is damaged" or "Cannot be opened". This is normal for unsigned apps.  
**Fix**: Right-click the app → Open → Click "Open" in the dialog. You only need to do this once.

**Linux (Debian/Ubuntu)**  
```bash
sudo apt install ./JavaFXLivePreview-linux-amd64.deb
```

**Linux (Fedora/RHEL)**  
```bash
sudo dnf install ./JavaFXLivePreview-linux-x86_64.rpm
```

**Linux (any distro)**  
```bash
tar -xzf JavaFXLivePreview-linux-x64.tar.gz
./JavaFXLivePreview/bin/javafx-live-preview
```

**Note**: All Linux packages require Java 17+ (installed automatically on Debian/Ubuntu/Fedora).

## 🔧 Build from source

```bash
git clone https://github.com/SourisCG/JavaVisualizer.git
cd JavaVisualizer
./gradlew shadowJar
# Output: build/libs/javafx-live-preview.jar
```

## 🎮 Usage

```
┌──────────────────────────────────────────────────────────────────┐
│ [Open...] [login.fxml ▼] [Auto ◎] [ Native ▼ ]          ● OK   │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    Your JavaFX scene                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

1. **Open...** — browse for an FXML file (or pass it as CLI argument)
2. **FXML selector** — switch between all `.fxml` files in the project
3. **Auto toggle** — enable/disable hot-reload
4. **Viewport** — simulate different screen sizes
5. **Status** — green dot = OK, red = error

## 📁 Supported project types

Works with any JavaFX project structure:

| Type | Detected by |
|------|------------|
| Maven | `pom.xml` in root |
| Gradle | `build.gradle` in root |
| Plain Java | `src/` directory |

The app auto-discovers:
- All `.fxml` files in the project
- All `.css` files (applied to the scene)
- Classpath (`target/classes`, `build/classes`, `bin`) for controllers

## 🏗️ How it works

```
User saves file ──► Polling watcher (every 500ms)
                        │
              ┌─────────┴─────────┐
              ▼                   ▼
        .fxml changed        .css changed
              │                   │
    FXMLLoader.load()    scene.getStylesheets().setAll()
              │                   │
         setRoot()           applyCss() + requestLayout()
              │                   │
              └─────────┬─────────┘
                        ▼
                  JavaFX renders
```

- **No compilation** — only reloads FXML and CSS
- **No WebSocket / agent** — it's a single JavaFX app
- **No inotify** — uses polling (works everywhere)

## 🛠️ Packaging

```bash
# Fat JAR (any OS)
./gradlew shadowJar

# RPM (Fedora/RHEL)
./packaging/linux/build-rpm.sh

# macOS DMG (generates .icns automatically)
./packaging/macos/build-dmg.sh

# Windows MSI + EXE (requires WIX Toolset + ImageMagick for .ico)
powershell -File packaging/windows/build-exe.ps1
```

See `packaging/` for detailed instructions per platform.

## 📄 License

MIT — do whatever you want with it.

---

Made with ☕ in JavaFX

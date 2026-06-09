# JavaFX Live Preview

**Edit your FXML and CSS — see changes instantly.**

No compilation. No waiting. Just save and watch your UI update in real time.

---

## What you get

Open any `.fxml` file and start editing. Every time you save:

- **FXML changes** reload immediately
- **CSS changes** apply without reloading the scene
- Switch between multiple FXML files from a dropdown
- Preview your UI at different sizes (phone, tablet, desktop, HD, or custom)
- Toggle auto-reload on/off with one click
- Clear error messages when something goes wrong

---

## Get started

### Option 1: Download an installer (recommended)

Pick your platform:

| Platform | Download |
|----------|----------|
| **Windows** | [![.exe](https://img.shields.io/badge/Download-.exe-blue?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-windows-x64.exe) [![.msi](https://img.shields.io/badge/Download-.msi-blue?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-windows-x64.msi) |
| **macOS (Apple Silicon)** | [![.dmg](https://img.shields.io/badge/Download-.dmg-lightgrey?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-macos-arm64.dmg) |
| **macOS (Intel)** | [![.dmg](https://img.shields.io/badge/Download-.dmg-lightgrey?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-macos-x64.dmg) |
| **Linux (Debian/Ubuntu)** | [![.deb](https://img.shields.io/badge/Download-.deb-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-amd64.deb) |
| **Linux (Fedora/RHEL)** | [![.rpm](https://img.shields.io/badge/Download-.rpm-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x86_64.rpm) |
| **Linux (any distro)** | [![.tar.gz](https://img.shields.io/badge/Download-.tar.gz-orange?style=flat-square)](https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x64.tar.gz) |

Then install it:

**Windows**  
Double-click the installer. No admin rights needed.

**macOS**  
Open the `.dmg` and drag the app to Applications.  
*First time opening it?* Right-click the app, select "Open", then click "Open" again. You'll only need to do this once — it's just macOS being cautious about unsigned apps.

**Linux (Debian/Ubuntu)**  
Double-click the downloaded `.deb` file. Your package manager will open and guide you through the installation.

**Linux (Fedora/RHEL)**  
Double-click the downloaded `.rpm` file. Your package manager will open and guide you through the installation.

**Linux (any distro)**  
Extract the `.tar.gz` and run `./JavaFXLivePreview/bin/javafx-live-preview` from the extracted folder.

> **Note:** Linux packages automatically install Java 17+ if you don't have it.

### Option 2: Install with one command

If you prefer the terminal, here's a one-liner for each platform:

**Windows (PowerShell)**
```powershell
irm https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-windows-x64.exe | iex
```

**macOS**
```bash
curl -L https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-macos-arm64.dmg -o ~/Downloads/JavaFXLivePreview.dmg && open ~/Downloads/JavaFXLivePreview.dmg
```

**Linux (Debian/Ubuntu)**
```bash
curl -L https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-amd64.deb -o /tmp/javafx.deb && sudo apt install -y /tmp/javafx.deb
```

**Linux (Fedora/RHEL)**
```bash
curl -L https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x86_64.rpm -o /tmp/javafx.rpm && sudo dnf install -y /tmp/javafx.rpm
```

**Linux (any distro)**
```bash
curl -L https://github.com/SourisCG/JavaVisualizer/releases/latest/download/JavaFXLivePreview-linux-x64.tar.gz | tar xz && ./JavaFXLivePreview/bin/javafx-live-preview
```

### Option 3: Run the JAR directly

If you already have Java 17+ installed, you can skip the installer:

```bash
# Open a specific FXML file
java -jar javafx-live-preview.jar /path/to/login.fxml

# Or let the app show you a file picker
java -jar javafx-live-preview.jar
```

---

## How to use it

```
┌──────────────────────────────────────────────────────────────────┐
│ [Open...] [login.fxml ▼] [Auto ◎] [ Native ▼ ]          ● OK   │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    Your JavaFX scene                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

The toolbar at the top gives you everything you need:

1. **Open...** — Browse for an FXML file (or pass it as a command-line argument)
2. **FXML selector** — Switch between all `.fxml` files in your project
3. **Auto toggle** — Pause or resume hot-reload
4. **Viewport** — Simulate different screen sizes
5. **Status indicator** — Green means everything's working, red means there's an error

---

## Project compatibility

This works with any JavaFX project structure. The app automatically detects:

| Project type | How it's detected |
|--------------|-------------------|
| Maven | `pom.xml` in the root |
| Gradle | `build.gradle` in the root |
| Plain Java | `src/` directory |

It also auto-discovers:
- All `.fxml` files in your project
- All `.css` files (automatically applied to the scene)
- Your classpath (`target/classes`, `build/classes`, `bin`) so controllers work

---

## How it works under the hood

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

**No compilation** — it only reloads FXML and CSS, not your Java code.  
**No WebSocket or agent** — it's just a standalone JavaFX app.  
**No inotify** — uses polling, so it works on every platform.

---

## Build from source

Want to build it yourself?

```bash
git clone https://github.com/SourisCG/JavaVisualizer.git
cd JavaVisualizer
./gradlew shadowJar
```

The JAR will be at `build/libs/javafx-live-preview.jar`.

---

## Packaging for distribution

If you want to create installers for your platform:

```bash
# Fat JAR (works on any OS)
./gradlew shadowJar

# RPM (Fedora/RHEL)
./packaging/linux/build-rpm.sh

# macOS DMG (generates .icns automatically)
./packaging/macos/build-dmg.sh

# Windows MSI + EXE (requires WIX Toolset + ImageMagick)
powershell -File packaging/windows/build-exe.ps1
```

Check the `packaging/` folder for platform-specific details.

---

## License

MIT — do whatever you want with it.

---

Built with JavaFX.

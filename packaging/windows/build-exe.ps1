# Windows packaging instructions
# 
# Prerequisites:
# 1. Install JDK 17+ from https://adoptium.net
# 2. Install WIX Toolset from https://wixtoolset.org
# 3. Add WIX to PATH
#
# Build steps:
# 1. cd javafx-live-preview
# 2. gradlew.bat shadowJar
# 3. jpackage --type exe --name "JavaFX Live Preview" --app-version 1.0.0 --input build\libs --main-jar javafx-live-preview.jar --main-class com.javafxpreview.Main --icon src\main\resources\icons\icon.ico --dest build\dist --win-shortcut --win-menu
#
# OR for MSI:
# 3. jpackage --type msi --name "JavaFX Live Preview" --app-version 1.0.0 --input build\libs --main-jar javafx-live-preview.jar --main-class com.javafxpreview.Main --icon src\main\resources\icons\icon.ico --dest build\dist --win-shortcut --win-menu
#
# Output: build/dist/*.exe or build/dist/*.msi
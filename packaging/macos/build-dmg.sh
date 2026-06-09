#!/bin/bash
set -e
echo "=== Building JavaFX Live Preview DMG ==="
cd "$(dirname "$0")/../.."
./gradlew shadowJar

ICON="src/main/resources/icons/icon.icns"
if [ ! -f "$ICON" ]; then
    echo "=== Generating .icns from .png ==="
    mkdir -p icon.iconset
    sips -z 128 128 src/main/resources/icons/icon.png --out icon.iconset/icon_128x128.png
    sips -z 256 256 src/main/resources/icons/icon.png --out icon.iconset/icon_128x128@2x.png
    sips -z 256 256 src/main/resources/icons/icon.png --out icon.iconset/icon_256x256.png
    sips -z 512 512 src/main/resources/icons/icon.png --out icon.iconset/icon_256x256@2x.png
    iconutil -c icns icon.iconset -o "$ICON"
    rm -rf icon.iconset
fi

echo "=== Running jpackage ==="
jpackage \
    --type dmg \
    --name "JavaFX Live Preview" \
    --app-version 1.0.0 \
    --input build/libs \
    --main-jar javafx-live-preview.jar \
    --main-class com.javafxpreview.Main \
    --icon "$ICON" \
    --dest build/dist
echo "=== DMG built ==="
ls -la build/dist/

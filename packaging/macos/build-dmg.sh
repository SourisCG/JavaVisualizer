#!/bin/bash
set -e
echo "=== Building JavaFX Live Preview DMG ==="
cd "$(dirname "$0")/../.."
./gradlew shadowJar
echo "=== Running jpackage ==="
jpackage \
    --type dmg \
    --name "JavaFX Live Preview" \
    --app-version 1.0.0 \
    --input build/libs \
    --main-jar javafx-live-preview.jar \
    --main-class com.javafxpreview.Main \
    --icon src/main/resources/icons/icon.icns \
    --dest build/dist
echo "=== DMG built in build/dist ==="
ls -la build/dist/

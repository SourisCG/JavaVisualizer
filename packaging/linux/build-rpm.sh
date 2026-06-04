#!/bin/bash
set -e
echo "=== Building JavaFX Live Preview RPM ==="
cd "$(dirname "$0")/../.."
./gradlew shadowJar
echo "=== Running jpackage ==="
jpackage \
    --type rpm \
    --name "javafx-live-preview" \
    --app-version 1.0.0 \
    --input build/libs \
    --main-jar javafx-live-preview.jar \
    --main-class com.javafxpreview.Main \
    --icon src/main/resources/icons/icon.png \
    --linux-package-name javafx-live-preview \
    --linux-shortcut \
    --linux-app-category Development \
    --dest build/dist
echo "=== RPM built in build/dist ==="
ls -la build/dist/

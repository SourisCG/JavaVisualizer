Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "=== Building JavaFX Live Preview for Windows ==="
Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent | Split-Path -Parent)

& .\gradlew.bat shadowJar
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$icon = "src\main\resources\icons\icon.ico"
if (-not (Test-Path $icon)) {
    Write-Host "=== Generating .ico from .png ==="
    magick src\main\resources\icons\icon.png -define icon:auto-resize=256,128,64,48,32,16 $icon
}

Write-Host "=== Building MSI ==="
jpackage --type msi --name "JavaFX Live Preview" --app-version 1.0.0 --input build\libs --main-jar javafx-live-preview.jar --main-class com.javafxpreview.Main --icon $icon --dest build\dist --win-shortcut --win-menu

Write-Host "=== Building EXE ==="
jpackage --type exe --name "JavaFX Live Preview" --app-version 1.0.0 --input build\libs --main-jar javafx-live-preview.jar --main-class com.javafxpreview.Main --icon $icon --dest build\dist --win-shortcut --win-menu

Write-Host "=== Done ==="
Get-ChildItem build\dist\*

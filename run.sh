#!/usr/bin/env bash
# Build the debug APK, install it on the connected phone / running emulator and launch it.
# Usage: ./run.sh            -> build + install + launch
#        ./run.sh --logs     -> same, then follow the app's logcat output
set -euo pipefail
cd "$(dirname "$0")"
PKG="com.vislupus.dream_records"

# Android SDK: write local.properties if missing.
if [[ ! -f local.properties ]]; then
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  [[ -z "$SDK" && -d "$HOME/Android/Sdk" ]] && SDK="$HOME/Android/Sdk"
  [[ -z "$SDK" && -d "$HOME/Library/Android/sdk" ]] && SDK="$HOME/Library/Android/sdk"
  [[ -n "$SDK" ]] && echo "sdk.dir=$SDK" > local.properties
fi

./gradlew --quiet installDebug
adb shell am start -n "$PKG/.MainActivity" >/dev/null
echo "✓ Dream Records installed and started"

if [[ "${1:-}" == "--logs" ]]; then
  adb logcat --pid="$(adb shell pidof -s "$PKG")"
fi

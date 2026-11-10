#!/usr/bin/env bash
# Build an installable APK.
# Usage: ./build.sh            -> debug APK   (app/build/outputs/apk/debug/app-debug.apk)
#        ./build.sh release    -> release APK (app/build/outputs/apk/release/app-release.apk)
set -euo pipefail
cd "$(dirname "$0")"
TYPE="${1:-debug}"
if [[ "$TYPE" == "release" ]]; then
  ./gradlew --quiet assembleRelease
  echo "✓ app/build/outputs/apk/release/app-release.apk"
else
  ./gradlew --quiet assembleDebug
  echo "✓ app/build/outputs/apk/debug/app-debug.apk"
fi

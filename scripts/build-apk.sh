#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

export ANDROID_USER_HOME="$PROJECT_DIR/.tools/android-user-home"
mkdir -p "$ANDROID_USER_HOME"
export PATH="$PROJECT_DIR/.tools/android-sdk/platform-tools:$PROJECT_DIR/.tools/android-sdk/cmdline-tools/latest/bin:$PATH"

echo "🔨 Building Khata Debug APK..."
cd "$PROJECT_DIR"
./gradlew assembleDebug

echo ""
echo "✅ Build Successful! APK generated at:"
echo "   $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

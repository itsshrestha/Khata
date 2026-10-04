#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

export HOME="$PROJECT_DIR/.tools"
export ANDROID_USER_HOME="$PROJECT_DIR/.tools/android-user-home"
mkdir -p "$ANDROID_USER_HOME" "$HOME/.android"
export PATH="$PROJECT_DIR/.tools/android-sdk/platform-tools:$PROJECT_DIR/.tools/android-sdk/cmdline-tools/latest/bin:$PATH"

ADB="$PROJECT_DIR/.tools/android-sdk/platform-tools/adb"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "📱 Checking connected Android devices via ADB..."
DEVICES=$("$ADB" devices | grep -v "List of devices" | grep "device$" || true)

if [ -z "$DEVICES" ]; then
    echo "⚠️  No connected Android device found."
    echo ""
    echo "Steps to connect your phone:"
    echo "1. Connect your Android phone to PC with a USB cable."
    echo "2. On your phone, go to Settings -> About Phone -> tap 'Build Number' 7 times to unlock Developer Options."
    echo "3. Go to Settings -> Developer Options -> turn ON 'USB Debugging'."
    echo "4. Accept the 'Allow USB Debugging' prompt on your phone screen."
    echo "5. Re-run this script: ./scripts/run-on-phone.sh"
    exit 1
fi

echo "Found device(s):"
echo "$DEVICES"
echo ""

if [ ! -f "$APK" ]; then
    echo "🔨 APK not found, building now..."
    "$SCRIPT_DIR/build-apk.sh"
fi

echo "📲 Installing APK to phone..."
if ! "$ADB" install -r "$APK"; then
    echo "⚠️  Signature mismatch or update failed. Uninstalling previous version and retrying..."
    "$ADB" uninstall com.khata.app || true
    "$ADB" install "$APK"
fi

echo "🚀 Launching Khata App on your phone..."
"$ADB" shell am start -n com.khata.app/.MainActivity

echo "✅ App launched successfully on your phone!"

#!/usr/bin/env bash
# Compiles and runs the pure-Kotlin tests (domain, utils) WITHOUT Gradle or the Android SDK.
# Android-dependent code (data/local, ui, ...) is verified through Gradle / Android Studio instead.
# Requires the local toolchain in .tools/ (JDK 17, Kotlin compiler, JUnit jars).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/.tools"
JAVA_HOME="$(echo "$TOOLS"/jdk-17*)"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$TOOLS/kotlinc/bin:$PATH"
OUT="$ROOT/.test-out"
CP="$TOOLS/libs/junit-4.13.2.jar:$TOOLS/libs/hamcrest-core-1.3.jar"
STDLIB="$TOOLS/kotlinc/lib/kotlin-stdlib.jar"

rm -rf "$OUT" && mkdir -p "$OUT"

MAIN_SRC=$(find "$ROOT/app/src/main/java/com/khata/app/domain" "$ROOT/app/src/main/java/com/khata/app/utils" -name '*.kt')
TEST_SRC=$(find "$ROOT/app/src/test" -name '*.kt')

kotlinc -jvm-target 17 -cp "$CP" -d "$OUT" $MAIN_SRC $TEST_SRC

TEST_CLASSES=$(cd "$OUT" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g')
java -cp "$OUT:$CP:$STDLIB" org.junit.runner.JUnitCore $TEST_CLASSES

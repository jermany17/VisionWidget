#!/usr/bin/env bash
# Watches the app sources and rebuilds, reinstalls and relaunches on every change,
# so the emulator always shows the current code.
#
#   bash tools/dev-watch.sh
#
# Needs a running emulator or a connected device. While this is running, avoid
# invoking ./gradlew yourself — two Gradle builds at once fight over the lock.
set -u

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR" || exit 1

ADB="${ANDROID_HOME:-$HOME/AppData/Local/Android/Sdk}/platform-tools/adb.exe"
APP_ID="com.example.visionwidget"
ACTIVITY="$APP_ID/.MainActivity"
POLL_SECONDS=2

# Paths whose contents decide whether a rebuild is needed.
WATCHED=(app/src app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml)

fingerprint() {
    find "${WATCHED[@]}" -type f -printf '%p %T@\n' 2>/dev/null | sort | md5sum
}

device_online() {
    "$ADB" devices 2>/dev/null | grep -qE '\bdevice$'
}

build_and_run() {
    local log status
    log="$(mktemp)"
    ./gradlew installDebug --console=plain >"$log" 2>&1
    status=$?

    if [ $status -eq 0 ]; then
        "$ADB" shell am start -n "$ACTIVITY" >/dev/null 2>&1
        echo "[$(date +%H:%M:%S)] reloaded"
    else
        echo "[$(date +%H:%M:%S)] BUILD FAILED"
        grep -E '^e: |error:|FAILURE:' "$log" | head -8
    fi
    rm -f "$log"
}

echo "[$(date +%H:%M:%S)] watching ${WATCHED[*]}"
last="$(fingerprint)"
build_and_run

while true; do
    sleep "$POLL_SECONDS"

    current="$(fingerprint)"
    [ "$current" = "$last" ] && continue
    last="$current"

    if ! device_online; then
        echo "[$(date +%H:%M:%S)] change detected but no device attached — skipped"
        continue
    fi
    build_and_run
done

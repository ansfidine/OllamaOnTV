#!/usr/bin/env bash
set -euo pipefail
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
TV="${1:?Usage: tools/connect.sh <TV-IP>}"
"$ADB" connect "$TV:5555"
"$ADB" -s "$TV:5555" reverse tcp:11436 tcp:11436
"$ADB" -s "$TV:5555" reverse tcp:11437 tcp:11437
"$ADB" -s "$TV:5555" reverse tcp:11438 tcp:11438
"$ADB" -s "$TV:5555" reverse --list

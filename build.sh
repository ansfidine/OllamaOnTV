#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
  for d in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" /opt/homebrew/share/android-commandlinetools \
           /usr/local/share/android-commandlinetools /usr/lib/android-sdk; do
    if [ -d "$d/build-tools" ]; then SDK="$d"; break; fi
  done
fi
[ -n "$SDK" ] && [ -d "$SDK/build-tools" ] || { echo "Android SDK not found. Set ANDROID_HOME."; exit 1; }
BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)"
[ -f "${AJ:-}" ] || { echo "Android platform not found. Install platforms;android-34."; exit 1; }
JAVAC=javac
KEYTOOL=keytool

rm -rf out
mkdir -p out/gen out/classes

"$BT/aapt2" compile --dir res -o out/res.zip
"$BT/aapt2" link -o out/base.apk -I "$AJ" --manifest AndroidManifest.xml --java out/gen out/res.zip \
  --min-sdk-version 26 --target-sdk-version 33 -A assets

"$JAVAC" -nowarn -Xlint:-options -source 8 -target 8 -bootclasspath "$AJ:$BT/core-lambda-stubs.jar" -classpath "$AJ" \
  -encoding UTF-8 -d out/classes $(find src out/gen -name '*.java')

"$BT/d8" --release --min-api 26 --lib "$AJ" --output out $(find out/classes -name '*.class')
(cd out && zip -qj base.apk classes.dex)
"$BT/zipalign" -f -p 4 out/base.apk out/aligned.apk

if [ ! -f ollamaontv.keystore ]; then
  "$KEYTOOL" -genkeypair -keystore ollamaontv.keystore -alias ollamaontv -keyalg RSA -keysize 2048 \
    -validity 20000 -storepass ollamaontv -keypass ollamaontv -dname "CN=OllamaOnTV" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks ollamaontv.keystore --ks-pass pass:ollamaontv --key-pass pass:ollamaontv \
  --out OllamaOnTV.apk out/aligned.apk

echo "Built OllamaOnTV.apk ($(du -h OllamaOnTV.apk | cut -f1))"

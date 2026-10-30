# OllamaOnTV v0.1.0

A cinematic, voice-first AI console for Android TV, developed by **Ansfidine Youssouf** as a fun
project for his TV.

## Features

- Cinematic dark home with animated aurora, hero, glass top bar and focus-scale cards
- Ollama Cloud chat, 23+ models, model dropdown and live web search
- Voice button: local Vosk speech-to-text → Ollama → spoken reply
- Neural Edge TTS voice (Aria), with the TV voice as fallback
- TV control through the TV's own ADB: open apps, keys, volume, settings, YouTube search
- Search, details, quick-settings and saved/watchlist overlays
- VPN-aware connection errors: switch VPN server / disable VPN, or start the local relay
- Settings **Test connection** and a retrying Models error card
- Fixed the home carousel rows not rendering (clear cards without removing the row container)
- Official OllamaOnTV logo: launcher icon, top bar mark, About screen, README
- Models and Saved are dedicated filtered tabs; Home stays Home
- Provider logos on model cards and saved model cards
- Compact circular back buttons; no duplicate back where a Close exists
- Removed the TV Apps launcher tab (the assistant still opens apps by voice)
- Model cards now show provider logos (DeepSeek, Mistral, NVIDIA, Kimi, GLM, Gemma, MiniMax, Meta, OpenAI)
- Developer credit: every model is told the app was developed by Ansfidine Youssouf
- Boot service, assistant role and accessibility fallback
- Inter typography, navigation sounds and reduced-motion support

## Screenshots

![Home](https://raw.githubusercontent.com/ansfidine/OllamaOnTV/main/docs/screenshots/home-v3.png)
![About](https://raw.githubusercontent.com/ansfidine/OllamaOnTV/main/docs/screenshots/about-v2.png)
![Settings](https://raw.githubusercontent.com/ansfidine/OllamaOnTV/main/docs/screenshots/settings-v2.png)

## Install

1. Download `OllamaOnTV.apk` from this release.
2. On the TV, enable Developer options → USB debugging.
3. `adb connect <TV-IP>:5555 && adb install -r OllamaOnTV.apk`
4. Grant microphone permission and open **Settings** to choose the endpoint/model.
5. For the assistant button: give OllamaOnTV the Assistant role or enable the button listener.

## No API key included

The APK and repository contain **no Ollama API key**. Add your own key from
[ollama.com/settings/keys](https://ollama.com/settings/keys) in the app, or run the included relay
with your key in `~/.ollama_cloud_key`. Never commit the key.

## Built with

Ollama Cloud, deepseek-v4.1-flash, Microsoft Edge TTS, Vosk, Free My TV's ADB client (MIT), Inter
(SIL OFL 1.1), Material Symbols (Apache 2.0), Android SDK/JDK and macOS LaunchAgents. UI/UX and
app code by Ansfidine Youssouf (MIT).

- About pill in the top bar replaces the AY avatar
- TV banner app icon with the full OllamaOnTV logo and readable wordmark
- Transparent logo inside the app (top bar and About)

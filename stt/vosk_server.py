#!/usr/bin/env python3
"""Local Vosk speech-to-text for OllamaOnTV."""
import io
import json
import os
import sys
import threading
import wave
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from vosk import KaldiRecognizer, Model, SetLogLevel

SetLogLevel(-1)
MODEL = os.environ.get(
    "VOSK_MODEL",
    os.path.expanduser("~/Library/Application Support/OllamaOnTV/vosk-model-small-en-us"),
)
model = Model(MODEL)
lock = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def do_GET(self):
        if self.path.rstrip("/") in ("/health", ""):
            self.respond(200, {"ok": True})
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path.rstrip("/") != "/stt":
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0") or 0)
        except ValueError:
            length = 0
        data = self.rfile.read(length) if length > 0 else b""
        print("STT %s bytes=%d" % (self.address_string(), len(data)), flush=True)
        try:
            with wave.open(io.BytesIO(data), "rb") as wav:
                rate = wav.getframerate()
                channels = wav.getnchannels()
                width = wav.getsampwidth()
                frames = wav.readframes(wav.getnframes())
            if rate != 16000 or channels != 1 or width != 2:
                raise ValueError("need 16 kHz mono 16-bit WAV")
            with lock:
                recognizer = KaldiRecognizer(model, rate)
                recognizer.AcceptWaveform(frames)
                text = json.loads(recognizer.FinalResult()).get("text", "").strip()
            self.respond(200, {"text": text})
        except Exception as error:
            self.respond(400, {"error": str(error)})

    def respond(self, code, obj):
        data = json.dumps(obj).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 11437
    print("Vosk STT http://0.0.0.0:%d/stt model=%s" % (port, MODEL), flush=True)
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()

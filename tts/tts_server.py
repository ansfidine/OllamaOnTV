#!/usr/bin/env python3
"""Neural Edge TTS for OllamaOnTV (free Microsoft Edge voices)."""
import asyncio
import json
import os
import sys
import tempfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import edge_tts

VOICE = os.environ.get("EDGE_VOICE", "en-US-AriaNeural")
RATE = os.environ.get("EDGE_RATE", "+0%")
PITCH = os.environ.get("EDGE_PITCH", "+0Hz")


async def synthesize(text, voice, path):
    communicate = edge_tts.Communicate(text, voice, rate=RATE, pitch=PITCH)
    await communicate.save(path)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def do_GET(self):
        path = self.path.rstrip("/")
        if path in ("/health", ""):
            self.respond(200, b'{"ok":true}', "application/json")
        elif path == "/voices":
            try:
                voices = asyncio.run(edge_tts.list_voices())
                data = json.dumps({"voices": voices}).encode("utf-8")
                self.respond(200, data, "application/json")
            except Exception as error:
                data = json.dumps({"error": "tts voices: " + str(error)}).encode("utf-8")
                self.respond(500, data, "application/json")
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path.rstrip("/") != "/tts":
            self.send_error(404)
            return
        path = None
        try:
            length = int(self.headers.get("Content-Length", "0") or 0)
            data = self.rfile.read(length) if length > 0 else b"{}"
            body = json.loads(data.decode("utf-8"))
            text = (body.get("text") or "").strip()
            if not text:
                raise ValueError("no text")
            voice = (body.get("voice") or VOICE).strip()
            fd, path = tempfile.mkstemp(suffix=".mp3")
            os.close(fd)
            asyncio.run(synthesize(text, voice, path))
            with open(path, "rb") as handle:
                audio = handle.read()
            print("TTS %s text=%d bytes_out=%d voice=%s" % (
                self.address_string(), len(text), len(audio), voice), flush=True)
            self.respond(200, audio, "audio/mpeg")
        except Exception as error:
            message = json.dumps({"error": "tts: " + str(error)}).encode("utf-8")
            self.respond(500, message, "application/json")
        finally:
            if path:
                try:
                    os.remove(path)
                except Exception:
                    pass

    def respond(self, code, data, content_type):
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 11438
    print("Edge TTS http://0.0.0.0:%d/tts voice=%s" % (port, VOICE), flush=True)
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()

#!/usr/bin/env python3
"""Local Ollama Cloud relay for OllamaOnTV.

The TV may not be able to reach ollama.com (network filtering). Run this on a computer
on the same Wi-Fi; it forwards /api/* to https://ollama.com and injects the API key.
The TV app then uses http://<computer-ip>:11436/api as its endpoint.

Key: set OLLAMA_API_KEY or OLLAMA_API_KEY_FILE (default ~/.ollama_cloud_key).
Only /api/ paths are proxied. Keep it on your own network.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

UPSTREAM = "https://ollama.com"
KEY = os.environ.get("OLLAMA_API_KEY", "").strip()
KEY_FILE = os.environ.get("OLLAMA_API_KEY_FILE", os.path.expanduser("~/.ollama_cloud_key"))
if not KEY and os.path.isfile(KEY_FILE):
    with open(KEY_FILE, "r", encoding="utf-8") as handle:
        KEY = handle.read().strip()
if not KEY:
    print("Set OLLAMA_API_KEY or put the key in " + KEY_FILE, file=sys.stderr)
    sys.exit(2)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def do_GET(self):
        self.proxy("GET")

    def do_POST(self):
        self.proxy("POST")

    def do_DELETE(self):
        self.proxy("DELETE")

    def proxy(self, method):
        if not self.path.startswith("/api/"):
            self.send_error(404)
            return
        print("REQ %s %s len=%s expect=%s ua=%s" % (
            method, self.path, self.headers.get("Content-Length"),
            self.headers.get("Expect"), self.headers.get("User-Agent")), flush=True)
        if (self.headers.get("Expect", "") or "").lower() == "100-continue":
            self.wfile.write(b"HTTP/1.1 100 Continue\r\n\r\n")
            self.wfile.flush()
        try:
            length = int(self.headers.get("Content-Length", "0") or 0)
        except ValueError:
            length = 0
        body = self.rfile.read(length) if length > 0 else None
        last = None
        for attempt in range(3):
            request = urllib.request.Request(UPSTREAM + self.path, data=body, method=method)
            request.add_header("Authorization", "Bearer " + KEY)
            request.add_header("Content-Type", self.headers.get("Content-Type", "application/json"))
            request.add_header("Accept", self.headers.get("Accept", "application/json"))
            try:
                with urllib.request.urlopen(request, timeout=180) as response:
                    data = response.read()
                    self.send_response(response.status)
                    self.send_header("Content-Type", response.headers.get("Content-Type", "application/json"))
                    self.send_header("Content-Length", str(len(data)))
                    self.end_headers()
                    self.wfile.write(data)
                    return
            except urllib.error.HTTPError as error:
                data = error.read()
                if error.code in (429, 500, 502, 503, 504) and attempt < 2:
                    last = (error.code, error.headers.get("Content-Type", "application/json"), data)
                    print("upstream HTTP %d, retry %d" % (error.code, attempt + 1), flush=True)
                    time.sleep(1.2 * (attempt + 1))
                    continue
                self.send_response(error.code)
                self.send_header("Content-Type", error.headers.get("Content-Type", "application/json"))
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
            except Exception as error:
                last = error
                print("upstream error %s, retry %d" % (error, attempt + 1), flush=True)
                if attempt < 2:
                    time.sleep(1.2 * (attempt + 1))
                    continue
                break
        if isinstance(last, tuple):
            code, content_type, data = last
            self.send_response(code)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        else:
            data = json.dumps({"error": "relay: " + str(last)}).encode("utf-8")
            self.send_response(502)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 11436
    print("OllamaOnTV relay http://0.0.0.0:%d/api -> %s/api" % (port, UPSTREAM), flush=True)
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()

#!/usr/bin/env python3
"""Minimal April-compatible algorithm mock for local AICO development.

Exposes:
  POST /mal/get_ai_reply
  POST /mal/get_parent_reply_basis
  POST /all_instruction
  POST /save_tree
"""

from __future__ import annotations

import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse


HOST = "0.0.0.0"
PORT = 22500


def ok(result):
    return {"code": 200, "msg": "ok", "result": result}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        print(f"[algorithm-mock] {self.address_string()} - {fmt % args}")

    def _read_json(self):
        length = int(self.headers.get("Content-Length", "0") or 0)
        if length <= 0:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            return {"_raw": raw.decode("utf-8", errors="replace")}

    def _write_json(self, payload, status: int = 200):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        trace = self.headers.get("X-Trace-Id") or self.headers.get("x-trace-id")
        if trace:
            self.send_header("X-Trace-Id", trace)
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):  # noqa: N802
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, X-Trace-Id")
        self.end_headers()

    def do_GET(self):  # noqa: N802
        path = urlparse(self.path).path
        if path in ("/", "/health"):
            self._write_json({"status": "ok", "service": "aico-algorithm-mock"})
            return
        self._write_json({"code": 404, "msg": f"unknown path {path}"}, status=404)

    def do_POST(self):  # noqa: N802
        path = urlparse(self.path).path.rstrip("/") or "/"
        data = self._read_json()
        trace = self.headers.get("X-Trace-Id") or self.headers.get("x-trace-id") or "-"
        print(f"[algorithm-mock] traceId={trace} path={path}")

        if path.endswith("/get_ai_reply") or path == "/mal/get_ai_reply":
            current = ""
            if isinstance(data, dict):
                current = data.get("current_content") or ""
            reply = (
                "（算法 Mock）我听到了你的困扰。"
                f"{' 你可以再说得更具体一些：' + current[:80] if current else ' 请继续描述当前场景。'}"
            )
            self._write_json(
                ok(
                    {
                        "all_reply": reply,
                        "score": 0.88,
                        "knowledge_uuids": [],
                        "chat_title": (current[:24] or "新会话"),
                    }
                )
            )
            return

        if path.endswith("/get_parent_reply_basis") or path == "/mal/get_parent_reply_basis":
            self._write_json(
                ok(
                    {
                        "profile": "local-demo client profile",
                        "reply_strategy": "validate first, then ask one concrete question",
                        "event_summary": "demo session",
                        "tag": "demo",
                        "current_need": "support",
                        "presenting_problem": "demo",
                        "emotion_state": "anxious",
                        "risk_signals": [],
                        "objective_background": {},
                        "subjective_perception": {},
                        "relationship_context": "personal",
                        "communication_style": "concise",
                        "cognitive_style": "practical",
                        "avoidance_pattern": "",
                        "sensitivity_points": [],
                        "preferred_tone": "warm",
                        "questioning_strategy": "open_then_specific",
                        "avoidance_guidelines": [],
                        "next_best_question": "Can you share one recent concrete scene?",
                        "permission_boundary": {},
                    }
                )
            )
            return

        if path.endswith("/all_instruction") or path == "/all_instruction":
            tree = {
                "id": "root",
                "label": "AICO Demo Strategy Tree",
                "children": [
                    {"id": "n1", "label": "Understand context", "children": []},
                    {"id": "n2", "label": "Build rapport", "children": []},
                    {"id": "n3", "label": "Address core need", "children": []},
                ],
            }
            self._write_json(tree)
            return

        if path.endswith("/save_tree") or path == "/save_tree":
            self._write_json(ok({"saved": True}))
            return

        self._write_json({"code": 404, "msg": f"unknown path {path}", "got": data}, status=404)


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"AICO algorithm mock listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()

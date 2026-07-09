"""Small JSON persistence layer for AICO long-term alignment state."""

from __future__ import annotations

import json
from copy import deepcopy
from pathlib import Path
from threading import RLock
from typing import Any


class JsonStateStore:
    def __init__(self, path: str | Path | None = None) -> None:
        default_path = Path(__file__).resolve().parent / "data" / "aico_state.json"
        self.memory_only = path == ":memory:"
        self.path = Path(path) if path and not self.memory_only else default_path
        self._lock = RLock()
        self._state: dict[str, Any] | None = None

    def load(self) -> dict[str, Any]:
        with self._lock:
            if self._state is not None:
                return deepcopy(self._state)
            if self.memory_only:
                self._state = self._empty_state()
                return deepcopy(self._state)
            if not self.path.exists():
                self._state = self._empty_state()
                return deepcopy(self._state)
            try:
                self._state = json.loads(self.path.read_text(encoding="utf-8"))
            except (json.JSONDecodeError, OSError):
                self._state = self._empty_state()
            return deepcopy(self._state)

    def save(self, state: dict[str, Any]) -> None:
        with self._lock:
            if self.memory_only:
                self._state = deepcopy(state)
                return
            self.path.parent.mkdir(parents=True, exist_ok=True)
            self.path.write_text(json.dumps(state, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
            self._state = deepcopy(state)

    def section(self, name: str) -> dict[str, Any]:
        state = self.load()
        section = state.setdefault(name, {})
        if not isinstance(section, dict):
            section = {}
            state[name] = section
            self.save(state)
        return section

    def update_section(self, name: str, section: dict[str, Any]) -> None:
        state = self.load()
        state[name] = section
        self.save(state)

    @staticmethod
    def _empty_state() -> dict[str, Any]:
        return {
            "topics": {},
            "topic_edges": [],
            "strategy_trees": {},
            "tree_executions": [],
            "personal_profiles": {},
            "expert_profiles": {},
            "client_service_profiles": {},
            "relationship_graph": {},
            "relationship_graphs": {},
            "confirmation_records": [],
            "feedback_events": [],
            "rag_fragments": {},
        }

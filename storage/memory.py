"""In-memory storage used by the first AICO framework skeleton."""

from __future__ import annotations

from collections import defaultdict
from typing import Iterable

from aico.api.schemas import AlignmentEvent


class InMemoryEventStore:
    """Append-only event store for alignment signals."""

    def __init__(self) -> None:
        self._events: list[AlignmentEvent] = []
        self._processed: set[str] = set()

    def append(self, event: AlignmentEvent) -> None:
        self._events.append(event)

    def extend(self, events: Iterable[AlignmentEvent]) -> None:
        for event in events:
            self.append(event)

    def all(self) -> list[AlignmentEvent]:
        return list(self._events)

    def pending(self) -> list[AlignmentEvent]:
        return [event for event in self._events if event.event_id not in self._processed]

    def mark_processed(self, event_id: str) -> None:
        self._processed.add(event_id)


class InMemoryConversationStore:
    """Minimal conversation history for routing and generation."""

    def __init__(self) -> None:
        self._messages: dict[str, list[str]] = defaultdict(list)

    def append(self, conversation_id: str, text: str) -> None:
        self._messages[conversation_id].append(text)

    def history(self, conversation_id: str, limit: int = 8) -> list[str]:
        return self._messages[conversation_id][-limit:]

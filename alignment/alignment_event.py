"""Utilities for creating alignment events from runtime actions."""

from __future__ import annotations

from dataclasses import asdict, is_dataclass
from typing import Any

from aico.api.schemas import AlignmentEvent, AlignmentEventType


def payload_dict(value: Any) -> dict[str, Any]:
    if is_dataclass(value):
        return asdict(value)
    if isinstance(value, dict):
        return dict(value)
    return {"value": value}


def make_event(event_type: AlignmentEventType, payload: Any) -> AlignmentEvent:
    return AlignmentEvent(event_type=event_type, payload=payload_dict(payload))

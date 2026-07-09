"""Dyadic relationship memory for community-style interactions."""

from __future__ import annotations

from aico.api.schemas import (
    AlignmentEvent,
    AlignmentEventType,
    ClientMessage,
    RelationshipState,
    utc_now,
)


class RelationshipService:
    def __init__(self) -> None:
        self._relationships: dict[str, RelationshipState] = {}

    def get_or_create(self, user_a_id: str, user_b_id: str) -> RelationshipState:
        relationship_id = self.relationship_id(user_a_id, user_b_id)
        if relationship_id not in self._relationships:
            ordered = sorted([user_a_id, user_b_id])
            self._relationships[relationship_id] = RelationshipState(
                relationship_id=relationship_id,
                user_a_id=ordered[0],
                user_b_id=ordered[1],
            )
        return self._relationships[relationship_id]

    def update_from_message(self, message: ClientMessage, counterpart_id: str | None) -> RelationshipState | None:
        if not counterpart_id:
            return None
        state = self.get_or_create(message.client_id, counterpart_id)
        state.interaction_count += 1
        state.shared_context.append(message.text[:160])
        state.shared_context = state.shared_context[-30:]
        state.communication_style["last_speaker"] = message.client_id
        state.communication_style["last_message_length"] = len(message.text)
        state.updated_at = utc_now()
        return state

    def apply_event(self, event: AlignmentEvent) -> bool:
        if event.event_type != AlignmentEventType.RELATIONSHIP_UPDATE:
            return False
        user_a_id = str(event.payload.get("user_a_id") or "")
        user_b_id = str(event.payload.get("user_b_id") or "")
        if not user_a_id or not user_b_id:
            return False
        state = self.get_or_create(user_a_id, user_b_id)
        note = event.payload.get("alignment_note")
        if note:
            state.alignment_notes.append(str(note))
            state.alignment_notes = state.alignment_notes[-20:]
        state.communication_style.update(event.payload.get("communication_style_patch") or {})
        state.updated_at = utc_now()
        return True

    @staticmethod
    def relationship_id(user_a_id: str, user_b_id: str) -> str:
        left, right = sorted([user_a_id, user_b_id])
        return f"rel_{left}__{right}"

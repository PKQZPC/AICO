"""Relationship memory for user-user or user-expert interactions."""

from __future__ import annotations

from aico.api.schemas import ClientMessage, RelationshipState, utc_now


class RelationshipMemoryService:
    """Tracks context shared by two people so AICO can adapt to both sides."""

    def __init__(self) -> None:
        self._relationships: dict[str, RelationshipState] = {}

    def get_or_create(self, user_a_id: str, user_b_id: str) -> RelationshipState:
        relationship_id = self._relationship_id(user_a_id, user_b_id)
        if relationship_id not in self._relationships:
            ordered = sorted([user_a_id, user_b_id])
            self._relationships[relationship_id] = RelationshipState(
                relationship_id=relationship_id,
                user_a_id=ordered[0],
                user_b_id=ordered[1],
            )
        return self._relationships[relationship_id]

    def update_from_message(self, message: ClientMessage) -> RelationshipState | None:
        counterparty_id = message.metadata.get("counterparty_id")
        if not counterparty_id:
            return None

        state = self.get_or_create(message.client_id, str(counterparty_id))
        state.interaction_count += 1
        state.updated_at = utc_now()
        state.shared_context.append(f"{message.client_id}: {message.text[:180]}")
        state.shared_context = state.shared_context[-80:]

        relationship_type = message.metadata.get("relationship_type")
        if relationship_type:
            state.communication_style["relationship_type"] = relationship_type
        if "tone" not in state.communication_style:
            state.communication_style["tone"] = "careful_and_contextual"

        return state

    @staticmethod
    def _relationship_id(user_a_id: str, user_b_id: str) -> str:
        left, right = sorted([user_a_id, user_b_id])
        return f"rel_{left}__{right}"

"""Long-term personal alignment memory.

This service models the broader AICO goal: every user owns a persistent
alignment system that becomes more accurate through long-term use.
"""

from __future__ import annotations

from aico.api.schemas import (
    AlignmentEvent,
    AlignmentEventType,
    ClientMessage,
    ClientProfile,
    PersonalAlignmentState,
    utc_now,
)


class PersonalAlignmentService:
    def __init__(self) -> None:
        self._states: dict[str, PersonalAlignmentState] = {}

    def get_or_create(self, user_id: str) -> PersonalAlignmentState:
        if user_id not in self._states:
            self._states[user_id] = PersonalAlignmentState(user_id=user_id)
        return self._states[user_id]

    def update_from_message(self, message: ClientMessage, profile: ClientProfile) -> PersonalAlignmentState:
        state = self.get_or_create(message.client_id)
        state.interaction_count += 1
        state.updated_at = utc_now()

        state.dynamic_state["last_emotion"] = profile.subjective.get("estimated_emotion")
        state.dynamic_state["last_topics"] = profile.subjective.get("topic_hints", [])
        state.dynamic_state["last_message"] = message.text

        for topic in profile.subjective.get("topic_hints", []):
            state.preferences[f"topic:{topic}"] = state.preferences.get(f"topic:{topic}", 0) + 1

        memory = self._compress_memory(message.text)
        if memory and memory not in state.long_term_memory:
            state.long_term_memory.append(memory)
            state.long_term_memory = state.long_term_memory[-20:]
        return state

    def apply_event(self, event: AlignmentEvent) -> bool:
        if event.event_type != AlignmentEventType.PERSONAL_MEMORY_UPDATE:
            return False
        user_id = str(event.payload.get("user_id") or "")
        if not user_id:
            return False
        state = self.get_or_create(user_id)
        state.stable_profile.update(event.payload.get("stable_profile_patch") or {})
        state.dynamic_state.update(event.payload.get("dynamic_state_patch") or {})
        state.preferences.update(event.payload.get("preference_patch") or {})
        memory = event.payload.get("memory")
        if memory:
            state.long_term_memory.append(str(memory))
            state.long_term_memory = state.long_term_memory[-20:]
        state.updated_at = utc_now()
        return True

    @staticmethod
    def _compress_memory(text: str) -> str:
        stripped = " ".join(text.split())
        if not stripped:
            return ""
        return stripped[:160]

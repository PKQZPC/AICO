"""Personal long-term alignment memory for every AICO user."""

from __future__ import annotations

from aico.api.schemas import ClientMessage, PersonalAlignmentState, utc_now


class PersonalMemoryService:
    """Maintains an evolving alignment state for each individual user."""

    def __init__(self) -> None:
        self._states: dict[str, PersonalAlignmentState] = {}

    def get_or_create(self, user_id: str) -> PersonalAlignmentState:
        if user_id not in self._states:
            self._states[user_id] = PersonalAlignmentState(user_id=user_id)
        return self._states[user_id]

    def update_from_message(self, message: ClientMessage) -> PersonalAlignmentState:
        state = self.get_or_create(message.client_id)
        state.interaction_count += 1
        state.updated_at = utc_now()

        text = message.text.strip()
        if text:
            state.dynamic_state["last_message"] = text
            state.dynamic_state["recent_intent"] = self._estimate_intent(text)
            state.dynamic_state["recent_emotion"] = self._estimate_emotion(text)
            memory = self._extract_memory_candidate(text)
            if memory and memory not in state.long_term_memory:
                state.long_term_memory.append(memory)
                state.long_term_memory = state.long_term_memory[-50:]

        preferred_style = message.metadata.get("preferred_style")
        if preferred_style:
            state.preferences["reply_style"] = preferred_style

        return state

    @staticmethod
    def _estimate_intent(text: str) -> str:
        lower = text.lower()
        if "how" in lower or "怎么" in lower:
            return "seeking_method"
        if "why" in lower or "为什么" in lower:
            return "seeking_reason"
        if "should" in lower or "要不要" in lower:
            return "seeking_decision"
        return "sharing_context"

    @staticmethod
    def _estimate_emotion(text: str) -> str:
        lower = text.lower()
        if any(word in lower for word in ["worry", "anxious", "担心", "焦虑"]):
            return "anxious"
        if any(word in lower for word in ["angry", "生气", "愤怒"]):
            return "angry"
        if any(word in lower for word in ["sad", "难过", "低落"]):
            return "sad"
        return "neutral_or_unknown"

    @staticmethod
    def _extract_memory_candidate(text: str) -> str | None:
        if len(text) < 12:
            return None
        return text[:180]

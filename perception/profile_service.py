"""Client perception and profile service."""

from __future__ import annotations

from aico.api.schemas import (
    AlignmentEvent,
    AlignmentEventType,
    ClientMessage,
    ClientProfile,
    utc_now,
)


class ClientProfileService:
    def __init__(self) -> None:
        self._profiles: dict[str, ClientProfile] = {}

    def get_or_create(self, client_id: str) -> ClientProfile:
        if client_id not in self._profiles:
            self._profiles[client_id] = ClientProfile(client_id=client_id)
        return self._profiles[client_id]

    def update_from_message(self, message: ClientMessage) -> ClientProfile:
        profile = self.get_or_create(message.client_id)
        profile.interaction_count += 1
        profile.updated_at = utc_now()

        text = message.text.lower()
        subjective = profile.subjective
        subjective["last_message"] = message.text
        subjective["estimated_emotion"] = self._estimate_emotion(text)
        subjective["topic_hints"] = self._topic_hints(text)
        return profile

    def apply_event(self, event: AlignmentEvent) -> bool:
        if event.event_type != AlignmentEventType.PROFILE_UPDATE:
            return False
        client_id = str(event.payload.get("client_id") or "")
        if not client_id:
            return False
        profile = self.get_or_create(client_id)
        profile.objective.update(event.payload.get("objective_patch") or {})
        profile.subjective.update(event.payload.get("subjective_patch") or {})
        profile.updated_at = utc_now()
        return True

    @staticmethod
    def _estimate_emotion(text: str) -> str:
        anxious_words = ["anxious", "worry", "worried", "焦虑", "担心", "害怕"]
        angry_words = ["angry", "mad", "furious", "生气", "愤怒", "吵"]
        sad_words = ["sad", "down", "hopeless", "难过", "低落", "绝望"]
        if any(word in text for word in anxious_words):
            return "anxious"
        if any(word in text for word in angry_words):
            return "angry"
        if any(word in text for word in sad_words):
            return "sad"
        return "unknown"

    @staticmethod
    def _topic_hints(text: str) -> list[str]:
        topics: list[str] = []
        if any(word in text for word in ["homework", "作业", "学习"]):
            topics.append("learning")
        if any(word in text for word in ["sleep", "睡眠", "失眠"]):
            topics.append("sleep")
        if any(word in text for word in ["conflict", "吵", "冲突"]):
            topics.append("family_conflict")
        if any(word in text for word in ["phone", "手机", "游戏"]):
            topics.append("screen_time")
        return topics

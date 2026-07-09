"""Knowledge integration service.

This module provides a stable interface that can later wrap the legacy RAG/KG
code under ``backend_mock``.
"""

from __future__ import annotations

from aico.api.schemas import AlignmentEvent, AlignmentEventType, KnowledgeFragment, new_id


class InMemoryKnowledgeService:
    def __init__(self, seed_fragments: list[KnowledgeFragment] | None = None) -> None:
        self._fragments: dict[str, KnowledgeFragment] = {}
        for fragment in seed_fragments or self.default_seed_fragments():
            self.upsert(fragment)

    @staticmethod
    def default_seed_fragments() -> list[KnowledgeFragment]:
        return [
            KnowledgeFragment(
                fragment_id="kg_family_routine",
                text="Stable family routines help reduce conflict and make child behavior easier to observe.",
                source="aico_seed",
                tags=["family", "routine", "observation"],
            ),
            KnowledgeFragment(
                fragment_id="kg_emotion_validation",
                text="Before proposing actions, validate the client's emotion and clarify concrete facts.",
                source="aico_seed",
                tags=["emotion", "clarify", "response"],
            ),
            KnowledgeFragment(
                fragment_id="kg_task_breakdown",
                text="For task avoidance, break the task into observable first steps and reduce startup friction.",
                source="aico_seed",
                tags=["task", "avoidance", "action"],
            ),
        ]

    def upsert(self, fragment: KnowledgeFragment) -> None:
        self._fragments[fragment.fragment_id] = fragment

    def retrieve(self, query: str, limit: int = 3) -> list[KnowledgeFragment]:
        query_tokens = self._tokens(query)
        scored: list[KnowledgeFragment] = []
        for fragment in self._fragments.values():
            if not fragment.approved:
                continue
            text_tokens = self._tokens(" ".join([fragment.text, " ".join(fragment.tags)]))
            overlap = len(query_tokens & text_tokens)
            tag_bonus = sum(1 for tag in fragment.tags if tag.lower() in query.lower())
            score = overlap + tag_bonus * 0.5
            if score > 0 or not query_tokens:
                scored.append(
                    KnowledgeFragment(
                        fragment_id=fragment.fragment_id,
                        text=fragment.text,
                        source=fragment.source,
                        tags=list(fragment.tags),
                        score=round(score, 3),
                        approved=fragment.approved,
                        metadata=dict(fragment.metadata),
                    )
                )
        scored.sort(key=lambda item: item.score, reverse=True)
        return scored[:limit]

    def apply_event(self, event: AlignmentEvent) -> bool:
        if event.event_type != AlignmentEventType.KNOWLEDGE_GAP:
            return False
        text = str(event.payload.get("candidate_text") or "").strip()
        if not text:
            return False
        self.upsert(
            KnowledgeFragment(
                fragment_id=new_id("kg"),
                text=text,
                source="expert_feedback",
                tags=list(event.payload.get("tags") or ["expert_feedback"]),
                approved=False,
                metadata={"status": "pending_expert_review"},
            )
        )
        return True

    @staticmethod
    def _tokens(text: str) -> set[str]:
        normalized = "".join(ch.lower() if ch.isalnum() else " " for ch in text)
        return {token for token in normalized.split() if token}

"""Persistent dynamic Topic Graph for AICO."""

from __future__ import annotations

from dataclasses import asdict

from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.api.schemas import ConfirmationRecord, ConfirmationSource, NodeStatus, TopicSignal, utc_now
from aico.storage.json_store import JsonStateStore


class TopicGraphService:
    def __init__(
        self,
        store: JsonStateStore | None = None,
        embedding: EmbeddingSimilarity | None = None,
        reuse_threshold: float = 0.84,
        related_threshold: float = 0.66,
    ) -> None:
        self.store = store or JsonStateStore()
        self.embedding = embedding or EmbeddingSimilarity()
        self.reuse_threshold = reuse_threshold
        self.related_threshold = related_threshold

    def merge_or_create(self, topic: TopicSignal, mode: str, aligned_subject_id: str) -> dict:
        state = self.store.load()
        topics = state.setdefault("topics", {})
        best_id = None
        best_score = 0.0
        for topic_id, existing in topics.items():
            if existing.get("mode") != mode:
                continue
            if existing.get("aligned_subject_id") != aligned_subject_id:
                continue
            score = self.embedding.similarity(topic.embedding, existing.get("embedding") or [])
            if score > best_score:
                best_score = score
                best_id = topic_id

        if best_id and best_score >= self.reuse_threshold:
            existing = topics[best_id]
            existing["observation_count"] = int(existing.get("observation_count") or 0) + 1
            existing["last_seen_at"] = utc_now().isoformat()
            existing["confidence"] = max(float(existing.get("confidence") or 0), topic.confidence, best_score)
            existing.setdefault("examples", []).append(topic.description)
            existing["examples"] = existing["examples"][-12:]
            state.setdefault("confirmation_records", []).append(asdict(topic.confirmation) if topic.confirmation else {})
            self.store.save(state)
            return {
                "action": "reuse_topic",
                "topic": existing,
                "similarity": best_score,
                "tree_policy": "reuse_bound_tree",
            }

        record = asdict(topic)
        record.update(
            {
                "mode": mode,
                "aligned_subject_id": aligned_subject_id,
                "status": NodeStatus.CANDIDATE.value,
                "created_at": utc_now().isoformat(),
                "last_seen_at": utc_now().isoformat(),
                "observation_count": 1,
                "examples": [topic.description],
                "bound_strategy_trees": [],
                "related_topics": [],
            }
        )
        topics[topic.topic_id] = record

        action = "create_new_topic"
        tree_policy = "create_candidate_tree"
        if best_id and best_score >= self.related_threshold:
            record["related_topics"].append({"topic_id": best_id, "similarity": best_score})
            topics[best_id].setdefault("related_topics", []).append({"topic_id": topic.topic_id, "similarity": best_score})
            state.setdefault("topic_edges", []).append(
                {
                    "source_topic_id": best_id,
                    "target_topic_id": topic.topic_id,
                    "relation": "semantic_related",
                    "similarity": best_score,
                    "created_at": utc_now().isoformat(),
                }
            )
            action = "create_related_topic"
            tree_policy = "extend_or_branch_tree"

        state.setdefault("confirmation_records", []).append(asdict(topic.confirmation) if topic.confirmation else {})
        self.store.save(state)
        return {
            "action": action,
            "topic": record,
            "matched_topic_id": best_id,
            "similarity": best_score,
            "tree_policy": tree_policy,
        }

    def bind_tree(self, topic_id: str, tree_id: str) -> None:
        state = self.store.load()
        topic = state.setdefault("topics", {}).get(topic_id)
        if not topic:
            return
        topic.setdefault("bound_strategy_trees", [])
        if tree_id not in topic["bound_strategy_trees"]:
            topic["bound_strategy_trees"].append(tree_id)
        self.store.save(state)

    def confirm_topic(self, topic_id: str, source: ConfirmationSource, source_id: str, status: NodeStatus, reason: str) -> dict | None:
        state = self.store.load()
        topic = state.setdefault("topics", {}).get(topic_id)
        if not topic:
            return None
        topic["status"] = status.value
        topic["confirmation"] = asdict(
            ConfirmationRecord(status=status, source=source, source_id=source_id, reason=reason)
        )
        state.setdefault("confirmation_records", []).append(topic["confirmation"])
        self.store.save(state)
        return topic

    def context_for_topic(self, topic_result: dict) -> str:
        topic = topic_result.get("topic") or {}
        return "\n".join(
            [
                "## AICO Topic Graph",
                f"- action: {topic_result.get('action')}",
                f"- topic_id: {topic.get('topic_id')}",
                f"- name: {topic.get('canonical_name')}",
                f"- intent: {topic.get('intent_summary')}",
                f"- goal: {topic.get('goal_summary')}",
                f"- status: {topic.get('status')}",
                f"- similarity: {topic_result.get('similarity', 0)}",
                f"- tree_policy: {topic_result.get('tree_policy')}",
            ]
        )

"""LLM-shaped structured topic extraction for AICO."""

from __future__ import annotations

import json
import os
import re
import urllib.request
from dataclasses import asdict

from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.api.schemas import ConfirmationRecord, ConfirmationSource, NodeStatus, TopicSignal, new_id


class LLMTopicExtractor:
    """Extracts dynamic topics without fixed scene enumeration.

    If ``AICO_LLM_ENDPOINT`` is configured, the extractor calls an
    OpenAI-compatible HTTP endpoint and validates the returned JSON. Without an
    endpoint it still produces the same structured contract through local LLM
    prompt rules, so downstream graph/tree code does not branch on availability.
    """

    def __init__(self, embedding: EmbeddingSimilarity | None = None) -> None:
        self.embedding = embedding or EmbeddingSimilarity()
        self.endpoint = os.getenv("AICO_LLM_ENDPOINT", "").strip()
        self.api_key = os.getenv("AICO_LLM_API_KEY", "").strip()
        self.model = os.getenv("AICO_LLM_MODEL", "aico-topic-extractor").strip()

    def extract(self, text: str, history: list[str] | None = None, mode: str = "personal") -> TopicSignal:
        raw = self._call_llm(text, history or [], mode) if self.endpoint else self._local_structured_extract(text, history or [], mode)
        topic = self._normalize(raw, text, mode)
        topic.embedding = self.embedding.embed(self._feature_text(topic))
        return topic

    def _call_llm(self, text: str, history: list[str], mode: str) -> dict:
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are AICO's dynamic topic extractor. Return strict JSON with keys: "
                        "canonical_name, description, intent_summary, goal_summary, confidence, evidence. "
                        "Do not use a fixed scene enumeration; infer the specific topic from the dialogue."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps({"mode": mode, "history": history[-8:], "message": text}, ensure_ascii=False),
                },
            ],
            "temperature": 0.1,
        }
        request = urllib.request.Request(
            self.endpoint,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                **({"Authorization": f"Bearer {self.api_key}"} if self.api_key else {}),
            },
        )
        with urllib.request.urlopen(request, timeout=20) as response:
            data = json.loads(response.read().decode("utf-8"))
        content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
        return json.loads(self._json_object(content))

    def _local_structured_extract(self, text: str, history: list[str], mode: str) -> dict:
        merged = " ".join(history[-4:] + [text])
        phrases = re.findall(r"[\u4e00-\u9fff]{2,8}|[A-Za-z][A-Za-z0-9_+-]{2,}", merged)
        ranked = sorted(set(phrases), key=lambda item: (-merged.count(item), merged.index(item)))
        canonical = " / ".join(ranked[:3]) if ranked else "未命名动态话题"
        intent = self._infer_intent(text)
        return {
            "canonical_name": canonical,
            "description": text[:220] or canonical,
            "intent_summary": intent,
            "goal_summary": self._infer_goal(intent, mode),
            "confidence": 0.72 if ranked else 0.45,
            "evidence": ranked[:6],
        }

    @staticmethod
    def _infer_intent(text: str) -> str:
        if any(word in text for word in ["怎么", "如何", "怎么办", "how"]):
            return "寻求方法或表达策略"
        if any(word in text for word in ["为什么", "原因", "why"]):
            return "理解原因和关系逻辑"
        if any(word in text for word in ["要不要", "能不能", "是否", "should"]):
            return "决策与风险权衡"
        return "组织当前多轮对话"

    @staticmethod
    def _infer_goal(intent: str, mode: str) -> str:
        if mode == "expert":
            return f"触发并校准专家逻辑树：{intent}"
        return f"结合个人画像、关系状态和历史策略组织回复：{intent}"

    def _normalize(self, raw: dict, text: str, mode: str) -> TopicSignal:
        confidence = float(raw.get("confidence") or 0.5)
        evidence = [str(item) for item in raw.get("evidence") or []]
        return TopicSignal(
            topic_id=new_id("topic"),
            canonical_name=str(raw.get("canonical_name") or text[:24] or "未命名动态话题"),
            description=str(raw.get("description") or text[:220]),
            intent_summary=str(raw.get("intent_summary") or "组织当前多轮对话"),
            goal_summary=str(raw.get("goal_summary") or self._infer_goal("组织当前多轮对话", mode)),
            confidence=max(0.0, min(1.0, confidence)),
            confirmation=ConfirmationRecord(
                status=NodeStatus.CANDIDATE,
                source=ConfirmationSource.AI,
                source_id="aico_topic_extractor",
                reason="LLM-shaped dynamic topic extraction",
                evidence=evidence,
            ),
            metadata={"mode": mode, "raw": raw},
        )

    @staticmethod
    def _feature_text(topic: TopicSignal) -> str:
        return " ".join([topic.canonical_name, topic.description, topic.intent_summary, topic.goal_summary])

    @staticmethod
    def _json_object(content: str) -> str:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        return match.group(0) if match else content

    @staticmethod
    def to_dict(topic: TopicSignal) -> dict:
        return asdict(topic)

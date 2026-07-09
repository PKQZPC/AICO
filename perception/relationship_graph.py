"""Owner-scoped relationship graph extraction, merging, and retrieval.

The graph is private to one aligned subject. It is built from that user's
conversation history, so the same real-world person can appear differently in
different owners' graphs.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import urllib.request
from copy import deepcopy
from datetime import datetime, timezone
from typing import Any

from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.api.schemas import ClientMessage
from aico.storage.json_store import JsonStateStore


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


class RelationshipGraphExtractor:
    """LLM-shaped extractor that returns graph update patches.

    If an OpenAI-compatible endpoint is configured through ``AICO_LLM_ENDPOINT``,
    it is used for structured extraction. Otherwise the local extractor keeps
    the same patch contract and only emits conservative updates.
    """

    def __init__(self, embedding: EmbeddingSimilarity | None = None) -> None:
        self.embedding = embedding or EmbeddingSimilarity()
        self.endpoint = os.getenv("AICO_LLM_ENDPOINT", "").strip()
        self.api_key = os.getenv("AICO_LLM_API_KEY", "").strip()
        self.model = os.getenv("AICO_LLM_MODEL", "aico-relationship-graph-extractor").strip()

    def extract_patch(
        self,
        *,
        owner_id: str,
        message: ClientMessage,
        direct_partner_id: str | None,
        history: list[str],
        current_graph_slice: dict[str, Any],
        topic_context: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        if self.endpoint:
            raw = self._call_llm(owner_id, message, direct_partner_id, history, current_graph_slice, topic_context or {})
        else:
            raw = self._local_patch(owner_id, message, direct_partner_id, history, current_graph_slice, topic_context or {})
        return self._normalize_patch(raw, owner_id, message, direct_partner_id)

    def _call_llm(
        self,
        owner_id: str,
        message: ClientMessage,
        direct_partner_id: str | None,
        history: list[str],
        current_graph_slice: dict[str, Any],
        topic_context: dict[str, Any],
    ) -> dict[str, Any]:
        payload = {
            "model": self.model,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are AICO's relationship graph extractor. The graph is private to one owner. "
                        "Return strict JSON only. Do not invent facts. Emit only fields that the current "
                        "message supports. Keys: direct_partner, mentioned_people, person_updates, "
                        "edge_updates, confidence, reasoning_note. Person fields may include aliases, "
                        "role_to_owner, profile_summary, communication_preferences, known_constraints. "
                        "Edge fields may include relationship_label, relationship_summary, dimensions, "
                        "important_events, recent_events, shared_topics, sensitive_topics_between_us, "
                        "interaction_patterns, strategy_implication."
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "owner_id": owner_id,
                            "direct_partner_id": direct_partner_id,
                            "message": {
                                "client_id": message.client_id,
                                "text": message.text,
                                "conversation_id": message.conversation_id,
                                "metadata": message.metadata,
                            },
                            "history": history[-12:],
                            "current_graph_slice": current_graph_slice,
                            "topic_context": topic_context,
                        },
                        ensure_ascii=False,
                    ),
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
        with urllib.request.urlopen(request, timeout=30) as response:
            data = json.loads(response.read().decode("utf-8"))
        content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
        return json.loads(self._json_object(content))

    def _local_patch(
        self,
        owner_id: str,
        message: ClientMessage,
        direct_partner_id: str | None,
        history: list[str],
        current_graph_slice: dict[str, Any],
        topic_context: dict[str, Any],
    ) -> dict[str, Any]:
        text = message.text or ""
        people = self._metadata_people(message.metadata)
        for alias in self._extract_people_mentions(text):
            people.setdefault(alias, {"alias": alias, "role_to_owner": ""})

        direct_partner = None
        if direct_partner_id:
            direct_partner = {
                "person_id_or_alias": direct_partner_id,
                "role_to_owner": str(message.metadata.get("relationship_type") or ""),
                "confidence": 0.8,
            }

        person_updates = []
        if direct_partner_id:
            fields = {
                "aliases": [direct_partner_id],
                "communication_preferences": self._communication_preferences(text),
                "known_constraints": self._known_constraints(text),
            }
            if fields["communication_preferences"] or fields["known_constraints"]:
                person_updates.append(
                    {
                        "person": direct_partner_id,
                        "fields": fields,
                        "evidence": [text[:180]],
                        "confidence": 0.72,
                    }
                )

        for alias, value in people.items():
            if alias in {owner_id, direct_partner_id}:
                continue
            person_updates.append(
                {
                    "person": alias,
                    "fields": {
                        "aliases": [alias],
                        "role_to_owner": value.get("role_to_owner", ""),
                    },
                    "evidence": [text[:180]],
                    "confidence": 0.58,
                }
            )

        edge_updates = []
        if direct_partner_id:
            edge_updates.append(
                {
                    "source": owner_id,
                    "target": direct_partner_id,
                    "fields": {
                        "relationship_label": str(message.metadata.get("relationship_type") or ""),
                        "relationship_summary": self._edge_summary(text, current_graph_slice),
                        "dimensions": self._dimensions(text),
                        "important_events": self._events(text),
                        "recent_events": self._events(text, recent=True),
                        "shared_topics": self._shared_topics(text, topic_context),
                        "sensitive_topics_between_us": self._sensitive_topics(text),
                        "interaction_patterns": self._interaction_patterns(text),
                        "strategy_implication": self._strategy_implication(text),
                    },
                    "evidence": [text[:220]],
                    "confidence": 0.68,
                }
            )

        for alias in people:
            if direct_partner_id and alias not in {owner_id, direct_partner_id}:
                edge_updates.append(
                    {
                        "source": direct_partner_id,
                        "target": alias,
                        "fields": {
                            "relationship_label": people[alias].get("role_to_owner", ""),
                            "relationship_summary": f"{direct_partner_id} mentioned {alias} in the owner's conversation.",
                            "recent_events": self._events(text, recent=True),
                        },
                        "evidence": [text[:220]],
                        "confidence": 0.5,
                    }
                )

        return {
            "direct_partner": direct_partner,
            "mentioned_people": list(people.values()),
            "person_updates": person_updates,
            "edge_updates": edge_updates,
            "confidence": 0.66 if edge_updates or person_updates else 0.35,
            "reasoning_note": "local conservative relationship patch",
        }

    def _normalize_patch(
        self,
        raw: dict[str, Any],
        owner_id: str,
        message: ClientMessage,
        direct_partner_id: str | None,
    ) -> dict[str, Any]:
        patch = {
            "owner_id": owner_id,
            "message_id": message.message_id,
            "conversation_id": message.conversation_id,
            "created_at": _now(),
            "direct_partner": raw.get("direct_partner") or {},
            "mentioned_people": self._list_of_dict(raw.get("mentioned_people")),
            "person_updates": self._list_of_dict(raw.get("person_updates")),
            "edge_updates": self._list_of_dict(raw.get("edge_updates")),
            "confidence": float(raw.get("confidence") or 0.5),
            "reasoning_note": str(raw.get("reasoning_note") or ""),
        }
        if direct_partner_id and not patch["direct_partner"]:
            patch["direct_partner"] = {"person_id_or_alias": direct_partner_id, "confidence": 0.7}
        return patch

    @staticmethod
    def _metadata_people(metadata: dict[str, Any]) -> dict[str, dict[str, Any]]:
        people: dict[str, dict[str, Any]] = {}
        raw_people = metadata.get("mentioned_people") or metadata.get("people") or []
        if isinstance(raw_people, str):
            raw_people = [item.strip() for item in raw_people.split(",") if item.strip()]
        for item in raw_people if isinstance(raw_people, list) else []:
            if isinstance(item, dict):
                alias = str(item.get("alias") or item.get("name") or item.get("id") or "").strip()
                if alias:
                    people[alias] = {"alias": alias, "role_to_owner": str(item.get("role_to_owner") or "")}
            else:
                alias = str(item).strip()
                if alias:
                    people[alias] = {"alias": alias, "role_to_owner": ""}
        return people

    @staticmethod
    def _extract_people_mentions(text: str) -> list[str]:
        patterns = [
            r"\b[A-Z][A-Za-z0-9_-]{0,20}\b",
            r"my\s+(mom|mother|father|dad|parent|friend|colleague|boss|teacher|partner)\b",
            r"(?:A's|B's)\s+(mom|mother|father|dad|parent|friend|colleague|boss|teacher|partner)\b",
        ]
        mentions: list[str] = []
        for pattern in patterns:
            for match in re.findall(pattern, text):
                value = match if isinstance(match, str) else " ".join(match)
                if value.lower() not in {"i", "ai", "aico"}:
                    mentions.append(value)
        return list(dict.fromkeys(mentions))[:8]

    @staticmethod
    def _communication_preferences(text: str) -> list[str]:
        lowered = text.lower()
        preferences = []
        if any(word in lowered for word in ["pressure", "push", "rush", "催", "压力"]):
            preferences.append("Use low-pressure wording and avoid repeated urging.")
        if any(word in lowered for word in ["joke", "humor", "轻松", "开玩笑"]):
            preferences.append("Light and relaxed wording may be accepted.")
        return preferences

    @staticmethod
    def _known_constraints(text: str) -> list[str]:
        lowered = text.lower()
        constraints = []
        if any(word in lowered for word in ["money", "borrow", "借钱", "还钱"]):
            constraints.append("Money-related requests need gradual framing and a clear exit option.")
        if any(word in lowered for word in ["privacy", "secret", "隐私", "丢脸"]):
            constraints.append("Protect privacy and face-saving boundaries.")
        return constraints

    @staticmethod
    def _dimensions(text: str) -> dict[str, float]:
        lowered = text.lower()
        return {
            "closeness": 0.68 if any(word in lowered for word in ["close", "best friend", "亲密", "熟"]) else 0.48,
            "trust": 0.7 if any(word in lowered for word in ["trust", "helped", "支持", "信任"]) else 0.52,
            "tension": 0.68 if any(word in lowered for word in ["fight", "angry", "conflict", "吵", "生气"]) else 0.22,
            "familiarity": 0.72 if any(word in lowered for word in ["again", "old friend", "多年", "很久"]) else 0.5,
            "boundary_sensitivity": 0.7 if any(word in lowered for word in ["money", "borrow", "privacy", "借钱", "隐私"]) else 0.35,
        }

    @staticmethod
    def _events(text: str, recent: bool = False) -> list[dict[str, Any]]:
        lowered = text.lower()
        event_type = ""
        if any(word in lowered for word in ["borrow", "money", "借钱"]):
            event_type = "money_request_or_concern"
        elif any(word in lowered for word in ["sorry", "apolog", "道歉"]):
            event_type = "apology_or_repair"
        elif any(word in lowered for word in ["fight", "conflict", "吵", "争执"]):
            event_type = "conflict_or_tension"
        elif any(word in lowered for word in ["long time", "again", "很久"]):
            event_type = "reconnection"
        if not event_type:
            return []
        return [{"event_type": event_type, "summary": text[:180], "recent": recent, "at": _now()}]

    @staticmethod
    def _shared_topics(text: str, topic_context: dict[str, Any]) -> list[str]:
        topic = topic_context.get("topic") or {}
        canonical = topic.get("canonical_name") or topic.get("canonicalName")
        topics = [str(canonical)] if canonical else []
        if "money" in text.lower() or "借钱" in text:
            topics.append("money_request")
        if "apolog" in text.lower() or "道歉" in text:
            topics.append("relationship_repair")
        return list(dict.fromkeys([item for item in topics if item]))

    @staticmethod
    def _sensitive_topics(text: str) -> list[str]:
        lowered = text.lower()
        topics = []
        if any(word in lowered for word in ["money", "borrow", "借钱"]):
            topics.append("money")
        if any(word in lowered for word in ["privacy", "secret", "隐私"]):
            topics.append("privacy")
        return topics

    @staticmethod
    def _interaction_patterns(text: str) -> list[str]:
        lowered = text.lower()
        patterns = []
        if any(word in lowered for word in ["slow reply", "late reply", "回得慢"]):
            patterns.append("The other party may reply slowly; avoid interpreting it too aggressively.")
        if any(word in lowered for word in ["主动", "reach out", "contact"]):
            patterns.append("Owner is considering initiating contact.")
        return patterns

    @staticmethod
    def _strategy_implication(text: str) -> str:
        lowered = text.lower()
        if any(word in lowered for word in ["money", "borrow", "借钱"]):
            return "Warm up the relationship, test receptiveness, state the request clearly, and leave an exit option."
        if any(word in lowered for word in ["sorry", "apolog", "道歉"]):
            return "Acknowledge impact first, avoid defending too early, then ask whether the other party is open to repair."
        if any(word in lowered for word in ["fight", "conflict", "吵", "争执"]):
            return "De-escalate first and avoid blame before discussing facts."
        return ""

    @staticmethod
    def _edge_summary(text: str, current_graph_slice: dict[str, Any]) -> str:
        previous = current_graph_slice.get("direct_edge", {}).get("relationship_summary", "")
        if previous:
            return f"{previous} Latest signal: {text[:120]}"
        return text[:180]

    @staticmethod
    def _list_of_dict(value: Any) -> list[dict[str, Any]]:
        if not isinstance(value, list):
            return []
        return [item for item in value if isinstance(item, dict)]

    @staticmethod
    def _json_object(content: str) -> str:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        return match.group(0) if match else content


class RelationshipGraphService:
    """Maintains owner-private relationship graphs and active subgraphs."""

    def __init__(
        self,
        store: JsonStateStore | None = None,
        embedding: EmbeddingSimilarity | None = None,
        extractor: RelationshipGraphExtractor | None = None,
    ) -> None:
        self.store = store or JsonStateStore()
        self.embedding = embedding or EmbeddingSimilarity()
        self.extractor = extractor or RelationshipGraphExtractor(self.embedding)

    def update_from_message(
        self,
        *,
        owner_id: str,
        message: ClientMessage,
        direct_partner_id: str | None,
        history: list[str],
        topic_context: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        if not direct_partner_id:
            return {"updated": False, "patch": {}, "active_subgraph": {}}

        state = self.store.load()
        graph = self._owner_graph(state, owner_id)
        self._ensure_person(graph, owner_id, display_name=owner_id, role_to_owner="self", source="rule")
        self._ensure_person(graph, direct_partner_id, display_name=direct_partner_id, role_to_owner="", source="rule")
        current_slice = self.active_subgraph(owner_id, direct_partner_id, topic_query=message.text, radius=2, state=state)
        patch = self.extractor.extract_patch(
            owner_id=owner_id,
            message=message,
            direct_partner_id=direct_partner_id,
            history=history,
            current_graph_slice=current_slice,
            topic_context=topic_context or {},
        )
        self._apply_patch(graph, patch, owner_id, message)
        graph["updated_at"] = _now()
        graph["stats"]["message_count"] = int(graph["stats"].get("message_count", 0)) + 1
        self._mirror_direct_edges(state, graph, owner_id)
        self.store.save(state)
        return {
            "updated": True,
            "owner_id": owner_id,
            "patch": patch,
            "active_subgraph": self.active_subgraph(owner_id, direct_partner_id, topic_query=message.text, radius=2),
        }

    def active_subgraph(
        self,
        owner_id: str,
        direct_partner_id: str,
        topic_query: str = "",
        radius: int = 2,
        state: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        state = state or self.store.load()
        graph = self._owner_graph(state, owner_id, save=False)
        if not graph:
            return {}
        selected_person_ids = {owner_id, direct_partner_id}
        selected_edge_ids: set[str] = set()

        for edge_id, edge in graph.get("edges", {}).items():
            if self._edge_touches(edge, {owner_id, direct_partner_id}):
                selected_edge_ids.add(edge_id)
                selected_person_ids.add(str(edge.get("source_person_id")))
                selected_person_ids.add(str(edge.get("target_person_id")))

        if radius >= 2:
            frontier = set(selected_person_ids)
            for edge_id, edge in graph.get("edges", {}).items():
                if self._edge_touches(edge, frontier) and self._edge_relevant(edge, topic_query):
                    selected_edge_ids.add(edge_id)
                    selected_person_ids.add(str(edge.get("source_person_id")))
                    selected_person_ids.add(str(edge.get("target_person_id")))

        persons = {
            person_id: deepcopy(person)
            for person_id, person in graph.get("persons", {}).items()
            if person_id in selected_person_ids
        }
        edges = {
            edge_id: deepcopy(edge)
            for edge_id, edge in graph.get("edges", {}).items()
            if edge_id in selected_edge_ids
        }
        direct_edge_id = self._edge_id(owner_id, owner_id, direct_partner_id)
        return {
            "owner_id": owner_id,
            "direct_partner_id": direct_partner_id,
            "direct_edge": deepcopy(graph.get("edges", {}).get(direct_edge_id, {})),
            "persons": persons,
            "edges": edges,
            "retrieval_policy": {
                "radius": radius,
                "topic_query": topic_query[:160],
                "uses_direct_edge": True,
                "uses_topic_relevant_two_hop_edges": radius >= 2,
            },
        }

    def _apply_patch(self, graph: dict[str, Any], patch: dict[str, Any], owner_id: str, message: ClientMessage) -> None:
        evidence = {
            "message_id": message.message_id,
            "conversation_id": message.conversation_id,
            "text": message.text[:240],
            "created_at": _now(),
        }
        for update in patch.get("person_updates", []):
            raw_person = str(update.get("person") or update.get("person_id") or update.get("alias") or "").strip()
            if not raw_person:
                continue
            person_id = self._resolve_person_id(graph, owner_id, raw_person, update)
            person = self._ensure_person(graph, person_id, display_name=raw_person, role_to_owner="", source="llm")
            self._merge_person_fields(person, update.get("fields") or {}, update, evidence)

        for update in patch.get("edge_updates", []):
            source = self._resolve_person_id(graph, owner_id, str(update.get("source") or owner_id), update)
            target = self._resolve_person_id(graph, owner_id, str(update.get("target") or ""), update)
            if not target:
                continue
            self._ensure_person(graph, source, display_name=source, role_to_owner="", source="llm")
            self._ensure_person(graph, target, display_name=target, role_to_owner="", source="llm")
            edge_id = self._edge_id(owner_id, source, target)
            edge = graph["edges"].setdefault(edge_id, self._new_edge(edge_id, owner_id, source, target))
            self._merge_edge_fields(edge, update.get("fields") or {}, update, evidence)

    def _merge_person_fields(
        self,
        person: dict[str, Any],
        fields: dict[str, Any],
        update: dict[str, Any],
        evidence: dict[str, Any],
    ) -> None:
        confidence = float(update.get("confidence") or fields.get("confidence") or 0.5)
        self._merge_list(person, "aliases", fields.get("aliases"), confidence)
        self._merge_scalar(person, "role_to_owner", fields.get("role_to_owner"), confidence)
        self._merge_scalar(person, "profile_summary", fields.get("profile_summary"), confidence, summary=True)
        self._merge_list(person, "communication_preferences", fields.get("communication_preferences"), confidence)
        self._merge_list(person, "known_constraints", fields.get("known_constraints"), confidence)
        self._merge_evidence(person, evidence, confidence, update.get("evidence"))
        person["updated_at"] = _now()

    def _merge_edge_fields(
        self,
        edge: dict[str, Any],
        fields: dict[str, Any],
        update: dict[str, Any],
        evidence: dict[str, Any],
    ) -> None:
        confidence = float(update.get("confidence") or fields.get("confidence") or 0.5)
        self._merge_scalar(edge, "relationship_label", fields.get("relationship_label"), confidence)
        self._merge_scalar(edge, "relationship_summary", fields.get("relationship_summary"), confidence, summary=True)
        self._merge_dimensions(edge, fields.get("dimensions") or {}, confidence)
        for key in [
            "important_events",
            "recent_events",
            "shared_topics",
            "sensitive_topics_between_us",
            "interaction_patterns",
        ]:
            self._merge_list(edge, key, fields.get(key), confidence)
        self._merge_scalar(edge, "strategy_implication", fields.get("strategy_implication"), confidence, summary=True)
        self._merge_evidence(edge, evidence, confidence, update.get("evidence"))
        edge["interaction_count"] = int(edge.get("interaction_count", 0)) + 1
        edge["last_interaction_at"] = _now()
        edge["updated_at"] = _now()

    def _merge_scalar(self, target: dict[str, Any], key: str, value: Any, confidence: float, summary: bool = False) -> None:
        if value is None or value == "":
            return
        incoming = str(value)
        old = str(target.get(key) or "")
        if not old or confidence >= float(target.get(f"{key}_confidence", 0.0)):
            target[key] = incoming if not summary or not old else self._combine_summary(old, incoming)
            target[f"{key}_confidence"] = round(confidence, 3)

    def _merge_list(self, target: dict[str, Any], key: str, values: Any, confidence: float) -> None:
        if values in (None, "", []):
            return
        if not isinstance(values, list):
            values = [values]
        items = target.setdefault(key, [])
        for value in values:
            if value in (None, ""):
                continue
            item = value if isinstance(value, dict) else str(value)
            text = json.dumps(item, ensure_ascii=False, sort_keys=True) if isinstance(item, dict) else item
            duplicate_index = self._similar_item_index(items, text)
            if duplicate_index is None:
                items.append(
                    {
                        "value": item,
                        "confidence": round(confidence, 3),
                        "source": "llm_patch",
                        "updated_at": _now(),
                    }
                )
            elif confidence > float(items[duplicate_index].get("confidence", 0.0)):
                items[duplicate_index].update({"value": item, "confidence": round(confidence, 3), "updated_at": _now()})
        target[key] = items[-40:]

    def _merge_dimensions(self, edge: dict[str, Any], values: dict[str, Any], confidence: float) -> None:
        dimensions = edge.setdefault("dimensions", {})
        for key, value in values.items():
            try:
                observed = max(0.0, min(1.0, float(value)))
            except (TypeError, ValueError):
                continue
            previous = float(dimensions.get(key, observed))
            weight = max(0.08, min(0.35, confidence * 0.25))
            dimensions[key] = round(previous * (1 - weight) + observed * weight, 3)

    def _merge_evidence(
        self,
        target: dict[str, Any],
        evidence: dict[str, Any],
        confidence: float,
        llm_evidence: Any,
    ) -> None:
        record = dict(evidence)
        record["confidence"] = round(confidence, 3)
        if llm_evidence:
            record["llm_evidence"] = llm_evidence
        evidence_items = target.setdefault("evidence_messages", [])
        if not any(item.get("message_id") == record["message_id"] for item in evidence_items):
            evidence_items.append(record)
        target["evidence_messages"] = evidence_items[-30:]
        target["confidence"] = round(max(float(target.get("confidence", 0.0)), confidence), 3)
        target["confirmation_status"] = target.get("confirmation_status") or "ai_inferred"

    def _ensure_person(
        self,
        graph: dict[str, Any],
        person_id: str,
        display_name: str,
        role_to_owner: str,
        source: str,
    ) -> dict[str, Any]:
        person = graph["persons"].setdefault(
            person_id,
            {
                "person_id": person_id,
                "owner_id": graph["owner_id"],
                "display_name": display_name,
                "aliases": [],
                "role_to_owner": role_to_owner,
                "profile_summary": "",
                "communication_preferences": [],
                "known_constraints": [],
                "mentioned_by": [],
                "source_conversations": [],
                "confidence": 1.0 if source == "rule" else 0.5,
                "confirmation_status": "rule_confirmed" if source == "rule" else "ai_inferred",
                "created_at": _now(),
                "updated_at": _now(),
            },
        )
        if display_name and not any(item.get("value") == display_name for item in person.get("aliases", [])):
            self._merge_list(person, "aliases", [display_name], 0.8 if source == "rule" else 0.5)
        return person

    @staticmethod
    def _new_edge(edge_id: str, owner_id: str, source: str, target: str) -> dict[str, Any]:
        return {
            "edge_id": edge_id,
            "owner_id": owner_id,
            "source_person_id": source,
            "target_person_id": target,
            "relationship_label": "",
            "relationship_summary": "",
            "dimensions": {},
            "important_events": [],
            "recent_events": [],
            "shared_topics": [],
            "sensitive_topics_between_us": [],
            "interaction_patterns": [],
            "strategy_implication": "",
            "tree_bindings": [],
            "evidence_messages": [],
            "interaction_count": 0,
            "confidence": 0.0,
            "confirmation_status": "ai_inferred",
            "created_at": _now(),
            "updated_at": _now(),
        }

    def _resolve_person_id(self, graph: dict[str, Any], owner_id: str, alias: str, update: dict[str, Any]) -> str:
        alias = alias.strip()
        if not alias:
            return ""
        if alias in {owner_id, graph.get("owner_id")}:
            return owner_id
        if alias in graph.get("persons", {}):
            return alias
        alias_embedding = self.embedding.embed(alias)
        best_id = ""
        best_score = 0.0
        for person_id, person in graph.get("persons", {}).items():
            alias_text = " ".join([person.get("display_name", "")] + [str(item.get("value", "")) for item in person.get("aliases", [])])
            score = self.embedding.similarity(alias_embedding, self.embedding.embed(alias_text))
            if score > best_score:
                best_id = person_id
                best_score = score
        if best_id and best_score >= 0.9:
            return best_id
        explicit = update.get("person_id") or update.get("target_person_id")
        if explicit:
            return str(explicit)
        digest = hashlib.sha1(f"{owner_id}:{alias}".encode("utf-8")).hexdigest()[:10]
        return f"person_{digest}"

    @staticmethod
    def _owner_graph(state: dict[str, Any], owner_id: str, save: bool = True) -> dict[str, Any]:
        graphs = state.setdefault("relationship_graphs", {})
        if not save and owner_id not in graphs:
            return {}
        return graphs.setdefault(
            owner_id,
            {
                "owner_id": owner_id,
                "persons": {},
                "edges": {},
                "stats": {"message_count": 0},
                "created_at": _now(),
                "updated_at": _now(),
            },
        )

    @staticmethod
    def _edge_id(owner_id: str, source: str, target: str) -> str:
        left, right = sorted([source, target])
        digest = hashlib.sha1(f"{owner_id}:{left}:{right}".encode("utf-8")).hexdigest()[:12]
        return f"edge_{digest}"

    @staticmethod
    def _edge_touches(edge: dict[str, Any], person_ids: set[str]) -> bool:
        return str(edge.get("source_person_id")) in person_ids or str(edge.get("target_person_id")) in person_ids

    def _edge_relevant(self, edge: dict[str, Any], topic_query: str) -> bool:
        if not topic_query:
            return True
        feature = json.dumps(
            {
                "label": edge.get("relationship_label"),
                "summary": edge.get("relationship_summary"),
                "topics": edge.get("shared_topics"),
                "events": edge.get("important_events"),
            },
            ensure_ascii=False,
        )
        return self.embedding.similarity(topic_query, feature) >= 0.12

    def _similar_item_index(self, items: list[dict[str, Any]], incoming_text: str) -> int | None:
        incoming_embedding = self.embedding.embed(incoming_text)
        for index, item in enumerate(items):
            existing = json.dumps(item.get("value"), ensure_ascii=False, sort_keys=True)
            if self.embedding.similarity(incoming_embedding, self.embedding.embed(existing)) >= 0.86:
                return index
        return None

    @staticmethod
    def _combine_summary(old: str, incoming: str) -> str:
        if incoming in old:
            return old
        return f"{old} {incoming}"[-800:]

    @staticmethod
    def _mirror_direct_edges(state: dict[str, Any], graph: dict[str, Any], owner_id: str) -> None:
        mirror = state.setdefault("relationship_graph", {})
        for edge_id, edge in graph.get("edges", {}).items():
            mirror[f"{owner_id}:{edge_id}"] = {
                "owner_id": owner_id,
                "edge_id": edge_id,
                "source_person_id": edge.get("source_person_id"),
                "target_person_id": edge.get("target_person_id"),
                "relationship_summary": edge.get("relationship_summary"),
                "dimensions": edge.get("dimensions"),
                "important_events": edge.get("important_events"),
                "strategy_implication": edge.get("strategy_implication"),
                "updated_at": edge.get("updated_at"),
            }

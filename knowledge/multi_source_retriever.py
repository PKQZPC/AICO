"""RAG over professional knowledge, personal memory, relationship memory, and strategy history."""

from __future__ import annotations

from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.api.schemas import KnowledgeFragment, new_id
from aico.storage.json_store import JsonStateStore


class MultiSourceRAG:
    def __init__(self, store: JsonStateStore | None = None, embedding: EmbeddingSimilarity | None = None) -> None:
        self.store = store or JsonStateStore()
        self.embedding = embedding or EmbeddingSimilarity()

    def retrieve(self, query: str, mode: str, aligned_subject_id: str, partner_id: str | None = None, limit: int = 6) -> list[KnowledgeFragment]:
        state = self.store.load()
        query_embedding = self.embedding.embed(query)
        candidates: list[KnowledgeFragment] = []
        self._collect_map(candidates, state.get("rag_fragments", {}), "professional_knowledge")
        self._collect_profile(candidates, state.get("personal_profiles", {}).get(aligned_subject_id), "personal_memory")
        self._collect_profile(candidates, state.get("expert_profiles", {}).get(aligned_subject_id), "expert_profile")
        if partner_id:
            client_service_profiles = state.get("client_service_profiles", {}) or state.get("client_context_profiles", {})
            self._collect_profile(candidates, client_service_profiles.get(partner_id), "client_service_profile")
            self._collect_relationship_subgraph(candidates, state, aligned_subject_id, partner_id, query)
        self._collect_map(candidates, state.get("strategy_trees", {}), "strategy_memory")

        scored = []
        for fragment in candidates:
            feature = " ".join([fragment.text, " ".join(fragment.tags)])
            fragment.score = self.embedding.similarity(query_embedding, self.embedding.embed(feature))
            if fragment.score > 0:
                scored.append(fragment)
        scored.sort(key=lambda item: item.score, reverse=True)
        return scored[:limit]

    @staticmethod
    def _collect_map(items: list[KnowledgeFragment], values: dict, source: str) -> None:
        for key, value in values.items():
            items.append(
                KnowledgeFragment(
                    fragment_id=str(key),
                    text=str(value)[:600],
                    source=source,
                    tags=[source],
                    approved=True,
                )
            )

    @staticmethod
    def _collect_profile(items: list[KnowledgeFragment], value: dict | None, source: str) -> None:
        if not value:
            return
        items.append(
            KnowledgeFragment(
                fragment_id=new_id(source),
                text=str(value)[:800],
                source=source,
                tags=[source],
                approved=True,
            )
        )

    def _collect_relationship_subgraph(
        self,
        items: list[KnowledgeFragment],
        state: dict,
        owner_id: str,
        partner_id: str,
        query: str,
    ) -> None:
        owner_graph = (state.get("relationship_graphs") or {}).get(owner_id) or {}
        if not owner_graph:
            legacy_id = "__".join(sorted([owner_id, partner_id]))
            self._collect_profile(items, (state.get("relationship_graph") or {}).get(legacy_id), "relationship_memory")
            return

        relevant_persons = {owner_id, partner_id}
        selected_edges = []
        for edge in (owner_graph.get("edges") or {}).values():
            if self._edge_touches(edge, relevant_persons):
                selected_edges.append(edge)
                relevant_persons.add(str(edge.get("source_person_id")))
                relevant_persons.add(str(edge.get("target_person_id")))

        for edge in (owner_graph.get("edges") or {}).values():
            if edge in selected_edges:
                continue
            if self._edge_touches(edge, relevant_persons) and self._relationship_score(edge, query) >= 0.12:
                selected_edges.append(edge)
                relevant_persons.add(str(edge.get("source_person_id")))
                relevant_persons.add(str(edge.get("target_person_id")))

        persons = {
            person_id: person
            for person_id, person in (owner_graph.get("persons") or {}).items()
            if person_id in relevant_persons
        }
        if selected_edges or persons:
            items.append(
                KnowledgeFragment(
                    fragment_id=f"relationship_subgraph_{owner_id}_{partner_id}",
                    text=str(
                        {
                            "owner_id": owner_id,
                            "current_partner_id": partner_id,
                            "persons": persons,
                            "edges": selected_edges[:12],
                        }
                    )[:1400],
                    source="relationship_memory",
                    tags=["relationship_memory", "owner_private_graph", owner_id, partner_id],
                    approved=True,
                )
            )

    @staticmethod
    def _edge_touches(edge: dict, person_ids: set[str]) -> bool:
        return str(edge.get("source_person_id")) in person_ids or str(edge.get("target_person_id")) in person_ids

    def _relationship_score(self, edge: dict, query: str) -> float:
        feature = " ".join(
            [
                str(edge.get("relationship_label") or ""),
                str(edge.get("relationship_summary") or ""),
                str(edge.get("shared_topics") or ""),
                str(edge.get("important_events") or ""),
                str(edge.get("strategy_implication") or ""),
            ]
        )
        return self.embedding.similarity(query, feature)

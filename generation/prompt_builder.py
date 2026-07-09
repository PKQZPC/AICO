"""Prompt/context builder for response generation."""

from __future__ import annotations

from aico.api.schemas import ClientMessage, ClientProfile, DecisionTrace, KnowledgeFragment, ResponseStage


class PromptBuilder:
    def build_context(
        self,
        message: ClientMessage,
        profile: ClientProfile,
        decision: DecisionTrace,
        knowledge: list[KnowledgeFragment],
        stage: ResponseStage,
        stage_instruction: str,
    ) -> dict[str, object]:
        return {
            "client_message": message.text,
            "client_profile": {
                "objective": profile.objective,
                "subjective": profile.subjective,
                "interaction_count": profile.interaction_count,
            },
            "decision_trace": {
                "tree_id": decision.tree_id,
                "matched_node_id": decision.matched_node_id,
                "path": decision.path,
                "reason": decision.reason,
                "confidence": decision.confidence,
            },
            "knowledge": [
                {
                    "fragment_id": item.fragment_id,
                    "text": item.text,
                    "source": item.source,
                    "score": item.score,
                }
                for item in knowledge
            ],
            "response_stage": stage.value,
            "stage_instruction": stage_instruction,
        }

"""Feedback policies for PERSONAL and EXPERT modes."""

from __future__ import annotations

from aico.api.schemas import ConfirmationSource, NodeStatus


class MultiSourceFeedbackEvaluator:
    def evaluate_personal(self, user_score: float | None, partner_signal: float | None, system_score: float | None, llm_score: float | None) -> dict:
        values = [value for value in [user_score, partner_signal, system_score, llm_score] if value is not None]
        score = sum(values) / len(values) if values else 0.0
        if score >= 0.72:
            status = NodeStatus.AI_CONFIRMED
        elif user_score is not None and user_score >= 0.7:
            status = NodeStatus.USER_CONFIRMED
        else:
            status = NodeStatus.CANDIDATE
        return {
            "mode": "personal",
            "score": round(score, 3),
            "status": status.value,
            "source": ConfirmationSource.AI.value if status == NodeStatus.AI_CONFIRMED else ConfirmationSource.USER.value,
            "inputs": {
                "user_score": user_score,
                "partner_signal": partner_signal,
                "system_score": system_score,
                "llm_score": llm_score,
            },
        }

    def evaluate_expert(self, expert_action: str, expert_id: str) -> dict:
        status = NodeStatus.EXPERT_CONFIRMED if expert_action in {"approve", "edit", "add"} else NodeStatus.REJECTED
        return {
            "mode": "expert",
            "status": status.value,
            "source": ConfirmationSource.EXPERT.value,
            "source_id": expert_id,
            "rule": "Only expert feedback can modify expert strategy trees.",
        }

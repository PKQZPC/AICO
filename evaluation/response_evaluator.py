"""Response evaluation module."""

from __future__ import annotations

from aico.api.schemas import ClientMessage, EvaluationResult, GeneratedResponse, RiskLevel
from aico.evaluation.safety_guard import SafetyGuard


class ResponseEvaluator:
    def __init__(self, safety_guard: SafetyGuard | None = None) -> None:
        self.safety_guard = safety_guard or SafetyGuard()

    def evaluate(self, message: ClientMessage, response: GeneratedResponse) -> EvaluationResult:
        risk_level, reasons = self.safety_guard.assess(message.text, response.text)
        has_knowledge = bool(response.knowledge_fragments)
        has_decision = bool(response.decision_trace.path)
        need_expert_review = (
            risk_level != RiskLevel.LOW
            or response.decision_trace.need_expert_review
            or response.decision_trace.confidence < 0.45
        )

        if response.decision_trace.need_expert_review:
            reasons.append("Decision router created a pending expert node.")
        if not has_knowledge:
            reasons.append("No approved knowledge fragment was retrieved.")

        safety_score = 0.35 if risk_level == RiskLevel.HIGH else 0.65 if risk_level == RiskLevel.MEDIUM else 0.95
        knowledge_score = 0.85 if has_knowledge else 0.55
        decision_score = 0.85 if has_decision else 0.5

        return EvaluationResult(
            response_id=response.response_id,
            content_accuracy=knowledge_score,
            professional_appropriateness=decision_score,
            emotional_suitability=0.78,
            safety_and_boundary=safety_score,
            expert_alignment=max(0.3, response.decision_trace.confidence),
            actionability=0.75,
            risk_level=risk_level,
            need_expert_review=need_expert_review,
            reasons=reasons,
        )

"""Five-stage response policy preserved from the legacy agent idea."""

from __future__ import annotations

from aico.api.schemas import ClientProfile, DecisionTrace, ResponseStage


class FiveStagePolicy:
    """Selects the local dialogue stage under the current expert tree node."""

    def choose_stage(self, profile: ClientProfile, decision: DecisionTrace, history_size: int) -> ResponseStage:
        if decision.need_expert_review:
            return ResponseStage.CLARIFY_INTENT
        if history_size <= 1:
            return ResponseStage.CLARIFY_INTENT
        if history_size <= 3:
            return ResponseStage.COLLECT_EVIDENCE
        if history_size <= 5:
            return ResponseStage.INFER_CAUSE
        if history_size <= 7:
            return ResponseStage.PROPOSE_ACTION
        return ResponseStage.SUMMARIZE_AND_CLOSE

    def instruction_for(self, stage: ResponseStage) -> str:
        instructions = {
            ResponseStage.CLARIFY_INTENT: "Clarify the client's goal and reflect the emotional concern.",
            ResponseStage.COLLECT_EVIDENCE: "Ask for observable facts before making conclusions.",
            ResponseStage.INFER_CAUSE: "Offer a cautious hypothesis and separate facts from assumptions.",
            ResponseStage.PROPOSE_ACTION: "Give a small, concrete, low-risk action plan.",
            ResponseStage.SUMMARIZE_AND_CLOSE: "Summarize what was learned and define the next checkpoint.",
        }
        return instructions[stage]

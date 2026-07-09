"""Response generation constrained by ThoughtTree, knowledge, and five-stage policy."""

from __future__ import annotations

from aico.api.schemas import (
    ClientMessage,
    ClientProfile,
    DecisionTrace,
    GeneratedResponse,
    KnowledgeFragment,
    new_id,
)
from aico.generation.five_stage_policy import FiveStagePolicy
from aico.generation.prompt_builder import PromptBuilder


class ResponseGenerator:
    def __init__(self, policy: FiveStagePolicy | None = None, prompt_builder: PromptBuilder | None = None) -> None:
        self.policy = policy or FiveStagePolicy()
        self.prompt_builder = prompt_builder or PromptBuilder()

    def generate(
        self,
        message: ClientMessage,
        profile: ClientProfile,
        decision: DecisionTrace,
        knowledge: list[KnowledgeFragment],
        history_size: int,
    ) -> GeneratedResponse:
        stage = self.policy.choose_stage(profile, decision, history_size)
        instruction = self.policy.instruction_for(stage)
        context = self.prompt_builder.build_context(message, profile, decision, knowledge, stage, instruction)

        knowledge_hint = knowledge[0].text if knowledge else "No approved knowledge fragment was retrieved."
        if decision.need_expert_review:
            text = (
                "I want to understand this more carefully before giving advice. "
                "Could you describe the most concrete recent scene, including what happened, "
                "what you said, and how the child responded?"
            )
        else:
            text = (
                f"{instruction} Based on the current expert path '{decision.matched_node_id}', "
                f"the first useful step is to clarify one concrete scene. "
                f"Relevant knowledge: {knowledge_hint}"
            )

        return GeneratedResponse(
            response_id=new_id("resp"),
            text=text,
            stage=stage,
            decision_trace=decision,
            knowledge_fragments=knowledge,
            metadata={"generation_context": context},
        )

"""Run a tiny AICO expert-alignment loop demo.

Usage:
    python -m aico.examples.run_demo
"""

from __future__ import annotations

from aico import AICOOrchestrator
from aico.api.schemas import ClientMessage, ExpertFeedback, FeedbackAction


def main() -> None:
    orchestrator = AICOOrchestrator()
    output = orchestrator.process_client_message(
        ClientMessage(
            client_id="client_demo",
            conversation_id="conv_demo",
            text="My child avoids homework every night and I am worried.",
        )
    )

    print("Decision:", output.decision_trace)
    print("Response:", output.response.text)
    print("Need expert:", output.routed_to_expert)

    feedback = ExpertFeedback(
        expert_id="expert_demo",
        response_id=output.response.response_id,
        action=FeedbackAction.EDIT,
        score=0.82,
        edited_text="First validate the parent's worry, then ask for one concrete homework scene.",
        comment="Good direction, but the first reply should be more concrete.",
    )
    orchestrator.submit_expert_feedback(feedback)
    report = orchestrator.run_iteration()
    print("Iteration:", report)


if __name__ == "__main__":
    main()

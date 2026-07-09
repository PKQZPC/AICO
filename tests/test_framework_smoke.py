import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from aico import AICOOrchestrator
from aico.api.schemas import ClientMessage, ExpertFeedback, FeedbackAction


class AICOFrameworkSmokeTest(unittest.TestCase):
    def test_client_expert_iteration_loop(self) -> None:
        orchestrator = AICOOrchestrator()

        output = orchestrator.process_client_message(
            ClientMessage(
                client_id="client_1",
                conversation_id="conv_1",
                text="My child avoids homework and I worry about learning.",
            )
        )

        self.assertTrue(output.response.text)
        self.assertEqual(output.decision_trace.matched_node_id, "learning")
        self.assertGreater(output.evaluation.overall_score, 0)
        self.assertIsNotNone(output.personal_alignment)
        self.assertGreaterEqual(output.personal_alignment.interaction_count, 1)

        event_ids = orchestrator.submit_expert_feedback(
            ExpertFeedback(
                expert_id="expert_1",
                response_id=output.response.response_id,
                action=FeedbackAction.EDIT,
                edited_text="Ask for the concrete homework scene before giving strategies.",
                comment="Use a more evidence-first reply.",
            )
        )
        self.assertTrue(event_ids)

        report = orchestrator.run_iteration()
        self.assertGreaterEqual(report.processed_events, len(event_ids))
        self.assertGreaterEqual(report.knowledge_updates, 1)

    def test_relationship_state_for_two_users(self) -> None:
        orchestrator = AICOOrchestrator()

        output = orchestrator.process_client_message(
            ClientMessage(
                client_id="user_a",
                conversation_id="conv_social",
                text="I want to reply more gently because my friend feels stressed.",
                metadata={"counterpart_id": "user_b"},
            )
        )

        self.assertIsNotNone(output.relationship_state)
        self.assertEqual(output.relationship_state.interaction_count, 1)
        self.assertIn("user_a", output.relationship_state.relationship_id)
        self.assertIn("user_b", output.relationship_state.relationship_id)


if __name__ == "__main__":
    unittest.main()

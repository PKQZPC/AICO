import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from aico import AICOOrchestrator
from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.alignment.topic_extractor import LLMTopicExtractor
from aico.alignment.topic_graph import TopicGraphService
from aico.api.schemas import AICOTurnInput, ClientMessage, InteractionMode
from aico.perception.relationship_graph import RelationshipGraphService
from aico.storage.json_store import JsonStateStore


class DynamicAlignmentTest(unittest.TestCase):
    def test_topic_extractor_returns_structured_embedding(self) -> None:
        extractor = LLMTopicExtractor()
        topic = extractor.extract("我想找很久没联系的朋友帮忙，但不想显得太突然", mode="personal")

        self.assertTrue(topic.canonical_name)
        self.assertTrue(topic.intent_summary)
        self.assertEqual(len(topic.embedding), 96)
        self.assertIsNotNone(topic.confirmation)

    def test_embedding_topic_graph_reuses_similar_topic(self) -> None:
        store = JsonStateStore(":memory:")
        store.save(JsonStateStore._empty_state())
        graph = TopicGraphService(store=store, embedding=EmbeddingSimilarity())
        extractor = LLMTopicExtractor()

        first = graph.merge_or_create(
            extractor.extract("我想向朋友借钱但不知道怎么开口", mode="personal"),
            mode="personal",
            aligned_subject_id="user_1",
        )
        second = graph.merge_or_create(
            extractor.extract("怎么跟朋友开口借一笔钱比较不尴尬", mode="personal"),
            mode="personal",
            aligned_subject_id="user_1",
        )

        self.assertIn(first["action"], {"create_new_topic", "create_related_topic"})
        self.assertIn(second["action"], {"reuse_topic", "create_related_topic", "create_new_topic"})
        self.assertIn("topics", store.load())

    def test_orchestrator_marks_expert_as_aligned_subject(self) -> None:
        orchestrator = AICOOrchestrator()
        output = orchestrator.process_client_message(
            AICOTurnInput(
                interaction_mode=InteractionMode.EXPERT,
                expert_mode=True,
                message=ClientMessage(
                    client_id="client_1",
                    conversation_id="expert_conv",
                    text="我最近和孩子沟通经常争执，想问咨询师怎么办",
                    metadata={"expert_id": "expert_9"},
                ),
            )
        )

        self.assertEqual(output.response.metadata["alignment_mode"], "expert")
        self.assertEqual(output.response.metadata["aligned_subject_type"], "expert")
        self.assertEqual(output.response.metadata["aligned_subject_id"], "expert_9")
        self.assertEqual(output.response.metadata["interaction_partner_type"], "client")
        self.assertIn("strategy_tree_execution", output.response.metadata)
        self.assertFalse(output.response.metadata["relationship_graph_update"]["updated"])

    def test_relationship_graph_updates_owner_private_edge_and_person_fields(self) -> None:
        store = JsonStateStore(":memory:")
        store.save(JsonStateStore._empty_state())
        service = RelationshipGraphService(store=store, embedding=EmbeddingSimilarity())

        result = service.update_from_message(
            owner_id="user_1",
            direct_partner_id="A",
            history=[],
            topic_context={"topic": {"canonical_name": "asking A for help"}},
            message=ClientMessage(
                client_id="user_1",
                conversation_id="personal_conv",
                text="A dislikes pressure, replies slowly, and money requests need care.",
                metadata={"relationship_type": "friend"},
            ),
        )
        state = store.load()
        graph = state["relationship_graphs"]["user_1"]
        direct_edge = result["active_subgraph"]["direct_edge"]

        self.assertTrue(result["updated"])
        self.assertIn("A", result["active_subgraph"]["persons"])
        self.assertEqual(direct_edge["relationship_label"], "friend")
        self.assertIn("tension", direct_edge["dimensions"])
        self.assertTrue(direct_edge["sensitive_topics_between_us"])
        self.assertTrue(graph["persons"]["A"]["communication_preferences"])

    def test_orchestrator_personal_exposes_active_relationship_subgraph(self) -> None:
        orchestrator = AICOOrchestrator()
        output = orchestrator.process_client_message(
            AICOTurnInput(
                interaction_mode=InteractionMode.PERSONAL,
                counterpart_id="A",
                message=ClientMessage(
                    client_id="user_1",
                    conversation_id="personal_conv",
                    text="I want to contact A again, but I do not want to pressure him about borrowing money.",
                    metadata={"relationship_type": "friend"},
                ),
            )
        )

        graph_update = output.response.metadata["relationship_graph_update"]
        active_subgraph = output.response.metadata["active_relationship_subgraph"]
        self.assertTrue(graph_update["updated"])
        self.assertEqual(active_subgraph["owner_id"], "user_1")
        self.assertEqual(active_subgraph["direct_partner_id"], "A")
        self.assertIn("direct_edge", active_subgraph)


if __name__ == "__main__":
    unittest.main()

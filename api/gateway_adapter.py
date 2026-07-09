"""Gateway-facing orchestrator for the AICO interactive evolution loop."""

from __future__ import annotations

from dataclasses import asdict

from aico.alignment.alignment_event import make_event
from aico.alignment.alignment_job import AICOAlignmentJob
from aico.alignment.embedding_similarity import EmbeddingSimilarity
from aico.alignment.expert_feedback import ExpertFeedbackProcessor
from aico.alignment.topic_extractor import LLMTopicExtractor
from aico.alignment.topic_graph import TopicGraphService
from aico.api.schemas import (
    AICOTurnInput,
    AICOTurnOutput,
    AlignmentEventType,
    ClientMessage,
    ExpertFeedback,
    IterationReport,
)
from aico.decision.router import ThoughtTreeRouter
from aico.decision.strategy_tree_runtime import StrategyTreeRuntime
from aico.decision.thought_tree import ThoughtTree
from aico.decision.tree_iteration import DecisionChainIteration
from aico.evaluation.multi_source_feedback import MultiSourceFeedbackEvaluator
from aico.evaluation.response_evaluator import ResponseEvaluator
from aico.generation.response_generator import ResponseGenerator
from aico.knowledge.multi_source_retriever import MultiSourceRAG
from aico.knowledge.retrieval_service import InMemoryKnowledgeService
from aico.perception.personal_alignment_service import PersonalAlignmentService
from aico.perception.profile_service import ClientProfileService
from aico.perception.relationship_graph import RelationshipGraphService
from aico.perception.relationship_service import RelationshipService
from aico.storage.json_store import JsonStateStore
from aico.storage.memory import InMemoryConversationStore, InMemoryEventStore


class AICOOrchestrator:
    """Coordinates client, AI, expert feedback, and automatic iteration."""

    def __init__(self) -> None:
        self.event_store = InMemoryEventStore()
        self.conversations = InMemoryConversationStore()
        self.json_store = JsonStateStore()
        self.embedding = EmbeddingSimilarity()
        self.topic_extractor = LLMTopicExtractor(self.embedding)
        self.topic_graph = TopicGraphService(self.json_store, self.embedding)
        self.strategy_runtime = StrategyTreeRuntime(self.json_store)
        self.multi_source_rag = MultiSourceRAG(self.json_store, self.embedding)
        self.feedback_evaluator = MultiSourceFeedbackEvaluator()
        self.knowledge_service = InMemoryKnowledgeService()
        self.profile_service = ClientProfileService()
        self.personal_alignment_service = PersonalAlignmentService()
        self.relationship_service = RelationshipService()
        self.relationship_graph_service = RelationshipGraphService(self.json_store, self.embedding)
        self.thought_tree = ThoughtTree.with_default_seed()
        self.router = ThoughtTreeRouter(self.thought_tree)
        self.generator = ResponseGenerator()
        self.evaluator = ResponseEvaluator()
        self.feedback_processor = ExpertFeedbackProcessor()
        self.iteration_job = AICOAlignmentJob(
            event_store=self.event_store,
            knowledge_service=self.knowledge_service,
            profile_service=self.profile_service,
            personal_alignment_service=self.personal_alignment_service,
            relationship_service=self.relationship_service,
            tree_iteration=DecisionChainIteration(self.thought_tree),
        )
        self._response_to_client: dict[str, str] = {}

    def process_client_message(self, message: ClientMessage | AICOTurnInput) -> AICOTurnOutput:
        turn_input = message if isinstance(message, AICOTurnInput) else AICOTurnInput(message=message)
        client_message = turn_input.message
        mode = "expert" if turn_input.expert_mode or turn_input.interaction_mode.value == "expert" else "personal"
        aligned_subject_id = (
            str(client_message.metadata.get("expert_id") or "expert_1")
            if mode == "expert"
            else client_message.client_id
        )
        interaction_partner_id = client_message.client_id if mode == "expert" else (
            turn_input.counterpart_id or client_message.metadata.get("counterpart_id")
        )

        self.conversations.append(client_message.conversation_id, client_message.text)
        history = self.conversations.history(client_message.conversation_id, limit=12)
        profile = self.profile_service.update_from_message(client_message)
        counterpart_id = turn_input.counterpart_id or client_message.metadata.get("counterpart_id")
        personal_alignment = self.personal_alignment_service.update_from_message(client_message, profile)
        relationship_state = self.relationship_service.update_from_message(client_message, counterpart_id) if mode == "personal" else None
        topic_signal = self.topic_extractor.extract(client_message.text, history=history, mode=mode)
        topic_result = self.topic_graph.merge_or_create(topic_signal, mode=mode, aligned_subject_id=aligned_subject_id)
        tree_result = self.strategy_runtime.select_or_create(topic_result, mode=mode, aligned_subject_id=aligned_subject_id)
        relationship_graph_update = (
            self.relationship_graph_service.update_from_message(
                owner_id=aligned_subject_id,
                message=client_message,
                direct_partner_id=str(interaction_partner_id) if interaction_partner_id else None,
                history=history,
                topic_context=topic_result,
            )
            if mode == "personal" and interaction_partner_id
            else {"updated": False, "patch": {}, "active_subgraph": {}}
        )
        tree_execution = self.strategy_runtime.execute(
            tree_result["tree"]["tree_id"],
            client_message.text,
            actor_id=client_message.client_id,
            partner_id=str(interaction_partner_id) if interaction_partner_id else None,
        )
        legacy_knowledge = self.knowledge_service.retrieve(client_message.text)
        rag_knowledge = self.multi_source_rag.retrieve(
            client_message.text,
            mode=mode,
            aligned_subject_id=aligned_subject_id,
            partner_id=str(interaction_partner_id) if interaction_partner_id else None,
        )
        knowledge = (rag_knowledge + legacy_knowledge)[:8]
        decision = self.router.route(client_message, profile)
        history_size = len(self.conversations.history(client_message.conversation_id, limit=1000))
        response = self.generator.generate(client_message, profile, decision, knowledge, history_size)
        response.metadata.update(
            {
                "alignment_mode": mode,
                "aligned_subject_id": aligned_subject_id,
                "aligned_subject_type": "expert" if mode == "expert" else "self",
                "interaction_partner_id": interaction_partner_id,
                "interaction_partner_type": "client" if mode == "expert" else "peer_or_self",
                "client_service_profile_policy": {
                    "used_for_expert_reply": mode == "expert",
                    "alignment_target": False if mode == "expert" else None,
                    "can_confirm_expert_tree": False if mode == "expert" else None,
                },
                "topic_graph": topic_result,
                "strategy_tree": tree_result,
                "strategy_tree_execution": asdict(tree_execution),
                "topic_context": self.topic_graph.context_for_topic(topic_result),
                "relationship_graph_update": relationship_graph_update,
                "active_relationship_subgraph": relationship_graph_update.get("active_subgraph", {}),
            }
        )
        evaluation = self.evaluator.evaluate(client_message, response)
        routed_to_expert = turn_input.expert_mode or evaluation.need_expert_review

        events = [
            make_event(AlignmentEventType.CLIENT_MESSAGE, client_message),
            make_event(
                AlignmentEventType.PERSONAL_MEMORY_UPDATE,
                {
                    "user_id": client_message.client_id,
                    "dynamic_state_patch": personal_alignment.dynamic_state,
                    "preference_patch": personal_alignment.preferences,
                    "memory": client_message.text[:160],
                },
            ),
            make_event(AlignmentEventType.AI_RESPONSE, response),
            make_event(AlignmentEventType.TOPIC_UPDATE, topic_result),
            make_event(AlignmentEventType.STRATEGY_TREE_EXECUTION, asdict(tree_execution)),
            make_event(AlignmentEventType.EVALUATION, asdict(evaluation)),
        ]
        if relationship_state:
            events.append(
                make_event(
                    AlignmentEventType.RELATIONSHIP_UPDATE,
                    {
                        "user_a_id": relationship_state.user_a_id,
                        "user_b_id": relationship_state.user_b_id,
                        "communication_style_patch": relationship_state.communication_style,
                        "alignment_note": f"Latest shared context: {client_message.text[:120]}",
                    },
                )
            )
        self.event_store.extend(events)
        self._response_to_client[response.response_id] = client_message.client_id

        return AICOTurnOutput(
            message=client_message,
            profile=profile,
            personal_alignment=personal_alignment,
            relationship_state=relationship_state,
            decision_trace=decision,
            response=response,
            evaluation=evaluation,
            routed_to_expert=routed_to_expert,
            iteration_events=events,
        )

    def submit_expert_feedback(self, feedback: ExpertFeedback) -> list[str]:
        client_id = self._response_to_client.get(feedback.response_id)
        events = self.feedback_processor.to_events(feedback, client_id=client_id)
        self.event_store.extend(events)
        return [event.event_id for event in events]

    def run_iteration(self) -> IterationReport:
        return self.iteration_job.run_once()

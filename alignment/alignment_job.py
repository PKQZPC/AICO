"""Event-driven iteration job for AICO."""

from __future__ import annotations

from aico.api.schemas import AlignmentEventType, IterationReport
from aico.decision.tree_iteration import DecisionChainIteration
from aico.knowledge.retrieval_service import InMemoryKnowledgeService
from aico.perception.personal_alignment_service import PersonalAlignmentService
from aico.perception.profile_service import ClientProfileService
from aico.perception.relationship_service import RelationshipService
from aico.storage.memory import InMemoryEventStore


class AICOAlignmentJob:
    """Processes pending alignment events and updates framework modules."""

    def __init__(
        self,
        event_store: InMemoryEventStore,
        knowledge_service: InMemoryKnowledgeService,
        profile_service: ClientProfileService,
        tree_iteration: DecisionChainIteration,
        personal_alignment_service: PersonalAlignmentService | None = None,
        relationship_service: RelationshipService | None = None,
    ) -> None:
        self.event_store = event_store
        self.knowledge_service = knowledge_service
        self.profile_service = profile_service
        self.tree_iteration = tree_iteration
        self.personal_alignment_service = personal_alignment_service
        self.relationship_service = relationship_service

    def run_once(self) -> IterationReport:
        pending = self.event_store.pending()
        report = IterationReport(processed_events=0)

        for event in pending:
            handled = False
            if self.knowledge_service.apply_event(event):
                report.knowledge_updates += 1
                handled = True
            if self.profile_service.apply_event(event):
                report.profile_updates += 1
                handled = True
            if self.personal_alignment_service and self.personal_alignment_service.apply_event(event):
                report.personal_updates += 1
                handled = True
            if self.relationship_service and self.relationship_service.apply_event(event):
                report.relationship_updates += 1
                handled = True
            if self.tree_iteration.apply_event(event):
                report.tree_updates += 1
                handled = True
            if event.event_type == AlignmentEventType.EVALUATION:
                report.evaluation_updates += 1
                handled = True

            if handled:
                report.notes.append(f"Processed {event.event_type.value}: {event.event_id}")
            self.event_store.mark_processed(event.event_id)
            report.processed_events += 1

        return report

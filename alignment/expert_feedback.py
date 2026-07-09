"""Transforms expert actions into alignment events."""

from __future__ import annotations

from aico.api.schemas import (
    AlignmentEvent,
    AlignmentEventType,
    ExpertFeedback,
    FeedbackAction,
)
from aico.alignment.alignment_event import make_event


class ExpertFeedbackProcessor:
    def to_events(self, feedback: ExpertFeedback, client_id: str | None = None) -> list[AlignmentEvent]:
        events: list[AlignmentEvent] = []
        review_payload = {
            "feedback_id": feedback.feedback_id,
            "expert_id": feedback.expert_id,
            "response_id": feedback.response_id,
            "action": feedback.action.value,
            "score": feedback.score,
            "comment": feedback.comment,
            "node_id": feedback.node_id,
        }
        events.append(make_event(AlignmentEventType.EXPERT_REVIEW, review_payload))

        if feedback.action in {FeedbackAction.EDIT, FeedbackAction.TAKEOVER} and feedback.edited_text:
            events.append(
                make_event(
                    AlignmentEventType.EXPERT_EDIT,
                    {
                        **review_payload,
                        "edited_text": feedback.edited_text,
                    },
                )
            )
            events.append(
                make_event(
                    AlignmentEventType.KNOWLEDGE_GAP,
                    {
                        "candidate_text": feedback.edited_text,
                        "tags": ["expert_edit"],
                        "response_id": feedback.response_id,
                    },
                )
            )

        if client_id and feedback.comment:
            events.append(
                make_event(
                    AlignmentEventType.PROFILE_UPDATE,
                    {
                        "client_id": client_id,
                        "subjective_patch": {"last_expert_comment": feedback.comment},
                    },
                )
            )

        if feedback.action in {
            FeedbackAction.ADD_NODE,
            FeedbackAction.APPROVE_NODE,
            FeedbackAction.REJECT_NODE,
            FeedbackAction.MERGE_NODE,
            FeedbackAction.MOVE_NODE,
        }:
            events.append(
                make_event(
                    AlignmentEventType.THOUGHT_TREE_UPDATE,
                    {
                        "action": feedback.action.value,
                        "node_id": feedback.node_id,
                        "proposed_node": feedback.proposed_node,
                        "comment": feedback.comment,
                    },
                )
            )

        if feedback.action == FeedbackAction.TAKEOVER:
            events.append(
                make_event(
                    AlignmentEventType.EXPERT_TAKEOVER,
                    {
                        "expert_id": feedback.expert_id,
                        "response_id": feedback.response_id,
                        "reason": feedback.comment,
                    },
                )
            )

        return events

"""Shared data contracts for the AICO alignment loop.

The first implementation is intentionally dependency-free. These dataclasses
can later be mirrored by Pydantic, FastAPI, database models, or message queues.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any
from uuid import uuid4


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:12]}"


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class ResponseStage(str, Enum):
    CLARIFY_INTENT = "clarify_intent"
    COLLECT_EVIDENCE = "collect_evidence"
    INFER_CAUSE = "infer_cause"
    PROPOSE_ACTION = "propose_action"
    SUMMARIZE_AND_CLOSE = "summarize_and_close"


class InteractionMode(str, Enum):
    PERSONAL = "personal"
    PEER = "peer"
    SELF = "self"
    EXPERT = "expert"


class DialoguePurpose(str, Enum):
    OPEN_ENDED = "open_ended"
    DYNAMIC_TOPIC = "dynamic_topic"
    RELATIONSHIP_ORIENTED = "relationship_oriented"
    PROFESSIONAL_SUPPORT = "professional_support"


class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class NodeStatus(str, Enum):
    CANDIDATE = "candidate"
    AI_CONFIRMED = "ai_confirmed"
    USER_CONFIRMED = "user_confirmed"
    EXPERT_CONFIRMED = "expert_confirmed"
    REJECTED = "rejected"
    ARCHIVED = "archived"


class FeedbackAction(str, Enum):
    ADOPT = "adopt"
    REJECT = "reject"
    EDIT = "edit"
    SCORE = "score"
    TAKEOVER = "takeover"
    ADD_NODE = "add_node"
    MERGE_NODE = "merge_node"
    MOVE_NODE = "move_node"
    APPROVE_NODE = "approve_node"
    REJECT_NODE = "reject_node"


class AlignmentMode(str, Enum):
    PERSONAL = "personal"
    EXPERT = "expert"


class SubjectType(str, Enum):
    SELF = "self"
    EXPERT = "expert"
    CLIENT = "client"
    PEER = "peer"


class ConfirmationSource(str, Enum):
    AI = "ai"
    USER = "user"
    EXPERT = "expert"
    SYSTEM = "system"


class AlignmentEventType(str, Enum):
    CLIENT_MESSAGE = "client_message"
    AI_RESPONSE = "ai_response"
    EXPERT_REVIEW = "expert_review"
    EXPERT_EDIT = "expert_edit"
    EXPERT_TAKEOVER = "expert_takeover"
    KNOWLEDGE_GAP = "knowledge_gap"
    THOUGHT_TREE_UPDATE = "thought_tree_update"
    PROFILE_UPDATE = "profile_update"
    PERSONAL_MEMORY_UPDATE = "personal_memory_update"
    RELATIONSHIP_UPDATE = "relationship_update"
    EVALUATION = "evaluation"
    TOPIC_UPDATE = "topic_update"
    STRATEGY_TREE_EXECUTION = "strategy_tree_execution"
    MULTI_SOURCE_FEEDBACK = "multi_source_feedback"


@dataclass
class SubjectRef:
    subject_type: SubjectType
    subject_id: str


@dataclass
class ConfirmationRecord:
    status: NodeStatus
    source: ConfirmationSource
    source_id: str
    reason: str = ""
    evidence: list[str] = field(default_factory=list)
    confirmed_at: datetime = field(default_factory=utc_now)


@dataclass
class TopicSignal:
    topic_id: str
    canonical_name: str
    description: str
    intent_summary: str
    goal_summary: str
    confidence: float
    embedding: list[float] = field(default_factory=list)
    confirmation: ConfirmationRecord | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class StrategyTreeExecution:
    tree_id: str
    current_node_id: str
    next_node_id: str
    action: str
    transition_reason: str
    trace: list[str] = field(default_factory=list)
    created_at: datetime = field(default_factory=utc_now)


@dataclass
class ClientMessage:
    client_id: str
    text: str
    conversation_id: str = "default"
    metadata: dict[str, Any] = field(default_factory=dict)
    message_id: str = field(default_factory=lambda: new_id("msg"))
    created_at: datetime = field(default_factory=utc_now)


@dataclass
class ClientProfile:
    client_id: str
    objective: dict[str, Any] = field(default_factory=dict)
    subjective: dict[str, Any] = field(default_factory=dict)
    interaction_count: int = 0
    updated_at: datetime = field(default_factory=utc_now)


@dataclass
class ClientServiceProfile:
    """Rich case/profile model for a client served by an expert.

    This profile supports expert replies and case understanding. It is not the
    aligned subject in EXPERT mode and cannot confirm or modify expert trees.
    """

    client_id: str
    profile: str = ""
    reply_strategy: str = ""
    event_summary: str = ""
    tag: str = ""
    current_need: str = ""
    presenting_problem: str = ""
    emotion_state: str = ""
    risk_signals: list[str] = field(default_factory=list)
    objective_background: dict[str, Any] = field(default_factory=dict)
    subjective_perception: dict[str, Any] = field(default_factory=dict)
    relationship_context: str = ""
    communication_style: str = ""
    cognitive_style: str = ""
    avoidance_pattern: str = ""
    sensitivity_points: list[str] = field(default_factory=list)
    preferred_tone: str = ""
    questioning_strategy: str = ""
    avoidance_guidelines: list[str] = field(default_factory=list)
    next_best_question: str = ""
    case_observations: list[str] = field(default_factory=list)
    case_timeline: list[dict[str, Any]] = field(default_factory=list)
    source_messages: list[dict[str, Any]] = field(default_factory=list)
    permission_boundary: dict[str, Any] = field(
        default_factory=lambda: {
            "used_for_expert_reply": True,
            "alignment_target": False,
            "can_confirm_expert_tree": False,
            "can_modify_expert_tree": False,
        }
    )
    updated_at: datetime = field(default_factory=utc_now)


@dataclass
class PersonalAlignmentState:
    """Long-term AICO memory owned by one user."""

    user_id: str
    stable_profile: dict[str, Any] = field(default_factory=dict)
    dynamic_state: dict[str, Any] = field(default_factory=dict)
    preferences: dict[str, Any] = field(default_factory=dict)
    long_term_memory: list[str] = field(default_factory=list)
    interaction_count: int = 0
    updated_at: datetime = field(default_factory=utc_now)


@dataclass
class RelationshipState:
    """Dyadic memory for interactions between two users."""

    relationship_id: str
    user_a_id: str
    user_b_id: str
    interaction_count: int = 0
    shared_context: list[str] = field(default_factory=list)
    communication_style: dict[str, Any] = field(default_factory=dict)
    alignment_notes: list[str] = field(default_factory=list)
    updated_at: datetime = field(default_factory=utc_now)


@dataclass
class KnowledgeFragment:
    fragment_id: str
    text: str
    source: str = "seed"
    tags: list[str] = field(default_factory=list)
    score: float = 0.0
    approved: bool = True
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class ThoughtNode:
    node_id: str
    name: str
    description: str
    parent_id: str | None = None
    child_ids: list[str] = field(default_factory=list)
    keywords: list[str] = field(default_factory=list)
    status: NodeStatus = NodeStatus.CANDIDATE
    aligned_count: int = 0
    version: int = 1
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass
class DecisionTrace:
    tree_id: str
    matched_node_id: str
    path: list[str]
    reason: str
    confidence: float
    need_expert_review: bool = False
    generated_pending_node_id: str | None = None


@dataclass
class GeneratedResponse:
    response_id: str
    text: str
    stage: ResponseStage
    decision_trace: DecisionTrace
    knowledge_fragments: list[KnowledgeFragment] = field(default_factory=list)
    metadata: dict[str, Any] = field(default_factory=dict)
    created_at: datetime = field(default_factory=utc_now)


@dataclass
class EvaluationResult:
    response_id: str
    content_accuracy: float
    professional_appropriateness: float
    emotional_suitability: float
    safety_and_boundary: float
    expert_alignment: float
    actionability: float
    risk_level: RiskLevel
    need_expert_review: bool
    reasons: list[str] = field(default_factory=list)

    @property
    def overall_score(self) -> float:
        values = [
            self.content_accuracy,
            self.professional_appropriateness,
            self.emotional_suitability,
            self.safety_and_boundary,
            self.expert_alignment,
            self.actionability,
        ]
        return round(sum(values) / len(values), 3)


@dataclass
class ExpertFeedback:
    expert_id: str
    response_id: str
    action: FeedbackAction
    score: float | None = None
    edited_text: str | None = None
    comment: str | None = None
    node_id: str | None = None
    proposed_node: ThoughtNode | None = None
    metadata: dict[str, Any] = field(default_factory=dict)
    feedback_id: str = field(default_factory=lambda: new_id("fb"))
    created_at: datetime = field(default_factory=utc_now)


@dataclass
class AlignmentEvent:
    event_type: AlignmentEventType
    payload: dict[str, Any]
    event_id: str = field(default_factory=lambda: new_id("evt"))
    created_at: datetime = field(default_factory=utc_now)


@dataclass
class AICOTurnInput:
    message: ClientMessage
    interaction_mode: InteractionMode = InteractionMode.PERSONAL
    dialogue_purpose: DialoguePurpose = DialoguePurpose.OPEN_ENDED
    expert_mode: bool = False
    counterpart_id: str | None = None


@dataclass
class AICOTurnOutput:
    message: ClientMessage
    profile: ClientProfile
    personal_alignment: PersonalAlignmentState | None
    relationship_state: RelationshipState | None
    decision_trace: DecisionTrace
    response: GeneratedResponse
    evaluation: EvaluationResult
    routed_to_expert: bool
    iteration_events: list[AlignmentEvent] = field(default_factory=list)


@dataclass
class IterationReport:
    processed_events: int
    knowledge_updates: int = 0
    profile_updates: int = 0
    personal_updates: int = 0
    relationship_updates: int = 0
    tree_updates: int = 0
    evaluation_updates: int = 0
    notes: list[str] = field(default_factory=list)

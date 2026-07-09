"""Applies expert-approved updates to the decision chain."""

from __future__ import annotations

from aico.api.schemas import AlignmentEvent, AlignmentEventType, FeedbackAction, NodeStatus, ThoughtNode
from aico.decision.thought_tree import ThoughtTree


class DecisionChainIteration:
    def __init__(self, tree: ThoughtTree) -> None:
        self.tree = tree

    def apply_event(self, event: AlignmentEvent) -> bool:
        if event.event_type != AlignmentEventType.THOUGHT_TREE_UPDATE:
            return False
        action = event.payload.get("action")
        if action == FeedbackAction.APPROVE_NODE.value:
            node_id = event.payload.get("node_id")
            if node_id in self.tree.nodes:
                self.tree.nodes[node_id].status = NodeStatus.EXPERT_CONFIRMED
                self.tree.nodes[node_id].version += 1
                return True
        if action == FeedbackAction.REJECT_NODE.value:
            node_id = event.payload.get("node_id")
            if node_id in self.tree.nodes:
                self.tree.nodes[node_id].status = NodeStatus.REJECTED
                self.tree.nodes[node_id].version += 1
                return True
        if action == FeedbackAction.ADD_NODE.value:
            proposed = event.payload.get("proposed_node")
            if isinstance(proposed, ThoughtNode):
                self.tree.add_node(proposed)
                return True
        return False

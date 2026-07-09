"""Routes interaction context onto the AICO dialogue logic tree."""

from __future__ import annotations

from aico.api.schemas import ClientMessage, ClientProfile, DecisionTrace, NodeStatus
from aico.decision.thought_tree import ThoughtTree


class ThoughtTreeRouter:
    def __init__(self, tree: ThoughtTree) -> None:
        self.tree = tree

    def route(self, message: ClientMessage, profile: ClientProfile) -> DecisionTrace:
        text = " ".join(
            [
                message.text,
                " ".join(profile.subjective.get("topic_hints") or []),
                str(profile.subjective.get("estimated_emotion") or ""),
            ]
        ).lower()

        best_node_id = self.tree.root_id
        best_score = 0
        best_reason = "No approved dialogue node matched; route remains at root."

        confirmed_statuses = {
            NodeStatus.AI_CONFIRMED,
            NodeStatus.USER_CONFIRMED,
            NodeStatus.EXPERT_CONFIRMED,
        }
        for node in self.tree.nodes.values():
            if node.status not in confirmed_statuses or node.node_id == self.tree.root_id:
                continue
            score = sum(1 for keyword in node.keywords if keyword.lower() in text)
            if score > best_score:
                best_node_id = node.node_id
                best_score = score
                best_reason = f"Matched dialogue node by {score} keyword signal(s)."

        generated_pending_node_id: str | None = None
        need_expert_review = False
        confidence = min(0.95, 0.35 + best_score * 0.2)

        if best_node_id == self.tree.root_id:
            pending = self.tree.create_pending_node(
                parent_id=self.tree.root_id,
                name="Pending dialogue branch",
                description=f"Unmatched dialogue situation: {message.text[:120]}",
                keywords=[],
            )
            generated_pending_node_id = pending.node_id
            need_expert_review = True
            confidence = 0.25
            best_reason = "Created a pending dialogue node because no branch covered the situation."
        else:
            self.tree.get(best_node_id).aligned_count += 1

        return DecisionTrace(
            tree_id=self.tree.tree_id,
            matched_node_id=best_node_id,
            path=self.tree.path_to_root(best_node_id),
            reason=best_reason,
            confidence=round(confidence, 3),
            need_expert_review=need_expert_review,
            generated_pending_node_id=generated_pending_node_id,
        )

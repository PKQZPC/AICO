"""Dialogue logic tree primitives for AICO.

The tree is not only for expert counseling. It is the macro organizer
for multi-turn dialogue: relationship stage, topic purpose, social distance,
branch conditions, and when to reveal or delay an intent.
"""

from __future__ import annotations

from aico.api.schemas import NodeStatus, ThoughtNode, new_id


class DialogueLogicTree:
    def __init__(self, tree_id: str = "aico_default_tree") -> None:
        self.tree_id = tree_id
        self.nodes: dict[str, ThoughtNode] = {}
        self.root_id = "root"
        self.add_node(
            ThoughtNode(
                node_id=self.root_id,
                name="AICO Root",
                description="Entry node for AICO multi-turn dialogue organization.",
                keywords=[],
            )
        )

    @classmethod
    def with_default_seed(cls) -> "DialogueLogicTree":
        tree = cls()
        tree.add_child(
            "root",
            "relationship_warmup",
            "Relationship warm-up and social distance calibration",
            ["熟", "不熟", "刚认识", "套近乎", "寒暄", "尴尬", "warm", "close"],
            description="Choose opening strategy based on familiarity, relationship history, and current emotional distance.",
        )
        tree.add_child(
            "root",
            "borrow_money",
            "Borrowing money purpose-driven conversation",
            ["借钱", "周转", "还钱", "开口", "money", "borrow"],
            description="Organize the dialogue from warm-up, context building, credibility signals, request timing, to fallback handling.",
        )
        tree.add_child(
            "root",
            "gossip",
            "Gossip and informal information exchange",
            ["八卦", "听说", "吐槽", "闲聊", "gossip"],
            description="Support informal talk while managing trust, privacy boundary, and relationship impact.",
        )
        tree.add_child(
            "root",
            "relationship_repair",
            "Relationship repair or difficult conversation",
            ["道歉", "和好", "误会", "冷战", "修复", "apologize"],
            description="Plan a multi-stage repair conversation from acknowledgment to clarification and future agreement.",
        )
        tree.add_child(
            "root",
            "learning",
            "Learning and homework support",
            ["homework", "learning", "study", "作业", "学习"],
            description="Professional or daily support around learning, homework, and family education issues.",
        )
        tree.add_child(
            "root",
            "family_conflict",
            "Family communication and conflict support",
            ["conflict", "argue", "family", "吵", "冲突", "沟通"],
        )
        tree.add_child(
            "root",
            "screen_time",
            "Screen time and game use support",
            ["phone", "game", "screen", "手机", "游戏"],
        )
        tree.add_child(
            "root",
            "emotion_support",
            "Emotion validation and stress support",
            ["anxious", "worry", "sad", "焦虑", "担心", "难过"],
        )
        return tree

    def add_node(self, node: ThoughtNode) -> None:
        self.nodes[node.node_id] = node
        if node.parent_id and node.parent_id in self.nodes:
            parent = self.nodes[node.parent_id]
            if node.node_id not in parent.child_ids:
                parent.child_ids.append(node.node_id)

    def add_child(
        self,
        parent_id: str,
        node_id: str,
        name: str,
        keywords: list[str],
        description: str | None = None,
        status: NodeStatus = NodeStatus.USER_CONFIRMED,
    ) -> ThoughtNode:
        node = ThoughtNode(
            node_id=node_id,
            name=name,
            description=description or name,
            parent_id=parent_id,
            keywords=keywords,
            status=status,
        )
        self.add_node(node)
        return node

    def get(self, node_id: str) -> ThoughtNode:
        return self.nodes[node_id]

    def children_of(self, node_id: str) -> list[ThoughtNode]:
        node = self.nodes[node_id]
        return [self.nodes[child_id] for child_id in node.child_ids if child_id in self.nodes]

    def path_to_root(self, node_id: str) -> list[str]:
        path: list[str] = []
        current_id: str | None = node_id
        while current_id and current_id in self.nodes:
            path.append(current_id)
            current_id = self.nodes[current_id].parent_id
        return list(reversed(path))

    def create_pending_node(self, parent_id: str, name: str, description: str, keywords: list[str]) -> ThoughtNode:
        node = ThoughtNode(
            node_id=new_id("node"),
            name=name,
            description=description,
            parent_id=parent_id,
            keywords=keywords,
            status=NodeStatus.CANDIDATE,
            metadata={"source": "router_unmatched_case"},
        )
        self.add_node(node)
        return node


ThoughtTree = DialogueLogicTree

"""Strict Strategy Tree runtime for macro multi-turn dialogue control."""

from __future__ import annotations

from dataclasses import asdict

from aico.api.schemas import (
    ConfirmationRecord,
    ConfirmationSource,
    NodeStatus,
    StrategyTreeExecution,
    TopicSignal,
    new_id,
    utc_now,
)
from aico.storage.json_store import JsonStateStore


class StrategyTreeRuntime:
    def __init__(self, store: JsonStateStore | None = None) -> None:
        self.store = store or JsonStateStore()

    def select_or_create(self, topic_result: dict, mode: str, aligned_subject_id: str) -> dict:
        state = self.store.load()
        trees = state.setdefault("strategy_trees", {})
        topic = topic_result.get("topic") or {}
        bound_trees = topic.get("bound_strategy_trees") or []
        if topic_result.get("tree_policy") == "reuse_bound_tree":
            for tree_id in reversed(bound_trees):
                tree = trees.get(tree_id)
                if tree:
                    tree["last_used_at"] = utc_now().isoformat()
                    self.store.save(state)
                    return {"action": "reuse_tree", "tree": tree}

        tree = self._new_tree(topic, mode, aligned_subject_id)
        trees[tree["tree_id"]] = tree
        if topic.get("topic_id"):
            topic.setdefault("bound_strategy_trees", []).append(tree["tree_id"])
            state.setdefault("topics", {})[topic["topic_id"]] = topic
        self.store.save(state)
        return {"action": "create_candidate_tree", "tree": tree}

    def execute(self, tree_id: str, message_text: str, actor_id: str, partner_id: str | None = None) -> StrategyTreeExecution:
        state = self.store.load()
        tree = state.setdefault("strategy_trees", {}).get(tree_id)
        if not tree:
            raise KeyError(f"Unknown strategy tree: {tree_id}")

        current_id = tree.get("current_node_id") or "orient"
        next_id, reason = self._choose_next(tree, current_id, message_text)
        tree["current_node_id"] = next_id
        tree["last_used_at"] = utc_now().isoformat()
        execution = StrategyTreeExecution(
            tree_id=tree_id,
            current_node_id=current_id,
            next_node_id=next_id,
            action=self._node(tree, next_id).get("dialogue_action", ""),
            transition_reason=reason,
            trace=[current_id, next_id],
        )
        record = asdict(execution)
        record.update({"actor_id": actor_id, "partner_id": partner_id})
        state.setdefault("tree_executions", []).append(record)
        state["tree_executions"] = state["tree_executions"][-1000:]
        self.store.save(state)
        return execution

    def propose_child_node(self, tree_id: str, parent_node_id: str, label: str, reason: str, mode: str) -> dict:
        state = self.store.load()
        tree = state.setdefault("strategy_trees", {}).get(tree_id)
        if not tree:
            raise KeyError(f"Unknown strategy tree: {tree_id}")
        node_id = new_id("candidate_node")
        status = NodeStatus.CANDIDATE.value
        node = {
            "node_id": node_id,
            "label": label,
            "stage_goal": label,
            "dialogue_action": "等待确认后进入该分支",
            "completion_condition": "由确认者补充",
            "status": status,
            "confirmation": asdict(
                ConfirmationRecord(
                    status=NodeStatus.CANDIDATE,
                    source=ConfirmationSource.AI,
                    source_id="aico_strategy_tree_runtime",
                    reason=reason,
                )
            ),
        }
        tree.setdefault("nodes", {})[node_id] = node
        tree.setdefault("edges", []).append(
            {
                "edge_id": new_id("edge"),
                "source": parent_node_id,
                "target": node_id,
                "condition": reason,
                "status": NodeStatus.CANDIDATE.value,
            }
        )
        self.store.save(state)
        return node

    def confirm_node(self, tree_id: str, node_id: str, source: ConfirmationSource, source_id: str, status: NodeStatus, reason: str) -> dict | None:
        state = self.store.load()
        tree = state.setdefault("strategy_trees", {}).get(tree_id)
        if not tree:
            return None
        node = tree.setdefault("nodes", {}).get(node_id)
        if not node:
            return None
        node["status"] = status.value
        node["confirmation"] = asdict(ConfirmationRecord(status=status, source=source, source_id=source_id, reason=reason))
        tree["version"] = int(tree.get("version") or 1) + 1
        state.setdefault("confirmation_records", []).append(node["confirmation"])
        self.store.save(state)
        return node

    @staticmethod
    def _new_tree(topic: dict, mode: str, aligned_subject_id: str) -> dict:
        source = ConfirmationSource.EXPERT if mode == "expert" else ConfirmationSource.AI
        return {
            "tree_id": new_id("tree"),
            "topic_id": topic.get("topic_id"),
            "topic_name": topic.get("canonical_name"),
            "mode": mode,
            "aligned_subject_id": aligned_subject_id,
            "current_node_id": "orient",
            "status": NodeStatus.CANDIDATE.value,
            "version": 1,
            "created_at": utc_now().isoformat(),
            "last_used_at": utc_now().isoformat(),
            "confirmation_policy": {
                "personal": "ai_confirmed and user_confirmed are visible and reversible",
                "expert": "expert_confirmed is the only solidification authority",
            },
            "nodes": {
                "orient": {
                    "node_id": "orient",
                    "label": "识别话题、关系和边界",
                    "stage_goal": "判断当前 topic 是否沿用旧树、扩展旧树或新建分支",
                    "dialogue_action": "先澄清上下文和对方状态",
                    "completion_condition": "上下文足够或对方愿意继续",
                    "status": NodeStatus.CANDIDATE.value,
                    "confirmation": asdict(ConfirmationRecord(status=NodeStatus.CANDIDATE, source=source, source_id="aico", reason="initial candidate")),
                },
                "probe": {
                    "node_id": "probe",
                    "label": "低压力试探",
                    "stage_goal": "观察对方接受、回避、拒绝或情绪变化",
                    "dialogue_action": "提出可退让的问题或轻量表达",
                    "completion_condition": "收到明确反馈",
                    "status": NodeStatus.CANDIDATE.value,
                },
                "advance": {
                    "node_id": "advance",
                    "label": "推进核心目标",
                    "stage_goal": "表达请求、观点、支持或专业建议",
                    "dialogue_action": "结合 RAG 和关系状态生成当前回复",
                    "completion_condition": "目标被表达且反馈被记录",
                    "status": NodeStatus.CANDIDATE.value,
                },
                "close": {
                    "node_id": "close",
                    "label": "修复或收束",
                    "stage_goal": "处理误解、拒绝、情绪升高或自然结束",
                    "dialogue_action": "降级、修复、总结或暂停",
                    "completion_condition": "关系风险下降或本轮结束",
                    "status": NodeStatus.CANDIDATE.value,
                },
            },
            "edges": [
                {"edge_id": "orient_probe", "source": "orient", "target": "probe", "condition": "上下文初步明确"},
                {"edge_id": "probe_advance", "source": "probe", "target": "advance", "condition": "对方接受或信息充分"},
                {"edge_id": "probe_close", "source": "probe", "target": "close", "condition": "对方回避、拒绝或风险升高"},
                {"edge_id": "advance_close", "source": "advance", "target": "close", "condition": "核心目标已表达"},
            ],
        }

    @staticmethod
    def _choose_next(tree: dict, current_id: str, message_text: str) -> tuple[str, str]:
        text = message_text.lower()
        outgoing = [edge for edge in tree.get("edges", []) if edge.get("source") == current_id]
        if not outgoing:
            return current_id, "当前节点没有可用下一跳"
        if any(word in text for word in ["不", "算了", "拒绝", "生气", "stop", "no"]):
            close = next((edge for edge in outgoing if edge.get("target") == "close"), None)
            if close:
                return close["target"], "检测到拒绝、回避或风险信号，转入修复/收束"
        return outgoing[0]["target"], f"满足跳转条件：{outgoing[0].get('condition')}"

    @staticmethod
    def _node(tree: dict, node_id: str) -> dict:
        return tree.get("nodes", {}).get(node_id, {})

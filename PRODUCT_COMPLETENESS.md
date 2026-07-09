# AICO Product Completeness Review

当前 `aico/` 已经包含 April 最新前端、后端、算法代码，但还不是完整 AICO 对齐产品版。原因是：April 代码提供了可运行产品基础，alignment core 提供了长期个人对齐和关系对齐框架，但二者尚未完全打通。

## 当前已经完成

- `api/schemas.py`：统一数据协议。
- `api/gateway_adapter.py`：AICO 编排入口。
- `perception/profile_service.py`：用户即时画像。
- `perception/personal_alignment_service.py`：长期个人对齐状态。
- `perception/relationship_service.py`：两人关系/社区交互状态。
- `decision/thought_tree.py`：专家/决策逻辑树骨架。
- `generation/five_stage_policy.py`：五阶段回复策略。
- `evaluation/response_evaluator.py`：回复评估与人工审核路由。
- `alignment/`：expert feedback 和 alignment event 自动迭代任务。
- `algorithm/`：April 最新算法代码。
- `backend/`：April 最新 Spring Boot 后端代码。
- `frontend/`：April 最新 Vue 前端代码。
- `legacy/`：其他参考代码副本。

## 为什么还不完善

April 最新代码确实包含前端和后端，但它们还不是完整 AICO 对齐框架，原因是：

- 前端已有 ParentChat、ExpertChat、DecisionTree 等页面，但还没有展示长期个人记忆、关系记忆、alignment event。
- 后端已有用户、专家、消息、决策树等模块，但还没有接入 `PersonalAlignmentState`、`RelationshipState` 和 alignment event pipeline。
- 算法端已有 `src_reconstruct`，但还没有与后端形成稳定的 AICO Algorithm API。
- 当前 clean alignment core 仍是内存版，没有接入 Java 后端数据库。
- 当前没有把“用户自己使用 AICO”和“用户与其他人聊天时 AICO 辅助理解对方”做成完整闭环。

所以，现状应定义为：

```text
可运行算法框架骨架：已完成
完整前后端产品：未完成
```

## 参考代码已经复制到 aico

```text
aico/legacy/frontend_app
  -> 前端参考代码副本

aico/legacy/backend_gateway
  -> 后端网关参考代码副本

aico/legacy/backend_mock
  -> 算法服务参考代码副本

aico/legacy/thought_tree_main
  -> ThoughtTree 参考代码副本
```

根目录参考代码保持不删除、不移动；后续开发、清理和重构只在 `aico/` 目录下进行。

## 目标产品面

### User App

面向普通用户，支持：

- 与 AICO 长期对话。
- 查看或编辑自己的长期画像。
- 在与其他人聊天时获得 AICO 辅助。
- 管理自己的记忆、偏好和隐私边界。

### Other-Person / Community Chat

面向多人关系，支持：

- 用户 A 与用户 B 的关系记忆。
- 对话双方的差异理解。
- 根据双方历史互动调整回复建议。
- 社区内不同人的个人 AICO 系统互相协作。

### Expert Console

面向专家或高可信校准者，支持：

- 审核 AI 回复。
- 编辑回复。
- 评分与标注错误类型。
- 校准用户画像和关系状态。
- 审核 ThoughtTree 新节点。
- 发布新版本策略。

### Backend

后端需要提供：

- Identity API
- Conversation API
- Personal Alignment API
- Relationship Alignment API
- Expert Review API
- ThoughtTree API
- Knowledge Retrieval API
- Evaluation API
- Alignment Event API

## 下一步工程优先级

1. 用 `aico/api/schemas.py` 定义正式 HTTP schema。
2. 把 `AICOOrchestrator` 包成后端服务。
3. 从 `legacy/backend_gateway` 迁移用户、会话、消息、专家接口。
4. 从 `legacy/frontend_app` 迁移 user app 和 expert console 页面。
5. 增加持久化层，替换当前内存存储。
6. 增加权限、隐私、记忆可编辑和版本回滚。

# Long-Term Personal and Relationship Alignment

AICO 的目标不应局限于“AI 向专家对齐”。更广泛地说，AICO 应该让每个用户拥有一套长期演化的个人对齐系统。

用户长期使用 AICO 后，系统应逐渐理解：

- 这个人是谁。
- 这个人的表达方式是什么。
- 这个人的稳定偏好、价值边界和沟通习惯是什么。
- 这个人当下状态如何变化。
- 这个人与另一个人之间有什么历史关系。
- 当这个人与别人聊天时，怎样的回复更准确、更合适、更不容易误解。

## 从 Expert Alignment 到 Personal Alignment

原始专家对齐机制：

```text
client message
  -> AI response
  -> expert review/edit/score
  -> alignment event
  -> update expert strategy
```

扩展后的长期个人对齐机制：

```text
user message
  -> personal memory
  -> relationship memory
  -> context-aware response
  -> user/expert/peer feedback
  -> alignment event
  -> update personal profile, relationship state, decision policy
```

这里的“expert”可以是心理专家、领域专家，也可以是用户本人、可信朋友、社区 moderator、组织内部导师。核心不是专家身份，而是“高质量校准信号”。

## 三层记忆

### 1. Personal Alignment State

每个用户自己的长期对齐状态。

当前代码：

```text
perception/personal_alignment_service.py
api/schemas.py::PersonalAlignmentState
```

包含：

- stable_profile：长期稳定信息。
- dynamic_state：当前状态。
- preferences：偏好、话题倾向、沟通风格。
- long_term_memory：长期记忆片段。
- interaction_count：交互次数。

### 2. Relationship State

两个用户之间的关系状态。

当前代码：

```text
perception/relationship_service.py
api/schemas.py::RelationshipState
```

包含：

- user_a_id / user_b_id。
- shared_context：双方共同历史。
- communication_style：双方互动风格。
- alignment_notes：对这段关系的校准记录。
- interaction_count：关系互动次数。

### 3. Community Alignment Layer

社区层尚未实现，但应作为下一步设计目标。

它负责：

- 多人对话上下文。
- 社区规则。
- 群体关系图谱。
- 可信校准者机制。
- 隐私和可见性边界。

## 长对话算法循环

```text
Message
  -> update short-term conversation state
  -> update personal alignment state
  -> update relationship state
  -> retrieve knowledge and memory
  -> route through decision tree
  -> generate response with five-stage policy
  -> evaluate safety, relevance, alignment
  -> collect feedback
  -> update memory and policies
```

## 对话场景

### 用户和 AICO 对话

```text
user -> AICO
```

系统主要使用：

- PersonalAlignmentState
- ClientProfile
- Knowledge
- DecisionTree

目标：AICO 越来越懂这个用户本人。

### 用户和另一个人聊天

```text
user A -> user B
```

系统需要同时使用：

- user A 的个人对齐状态。
- user B 的个人对齐状态。
- A-B 的 relationship state。
- 当前对话语境。

目标：AICO 不只知道“我是谁”，也知道“我正在和谁说话，以及我们过去怎么互动”。

### 用户、他人、专家共同参与

```text
user A + user B + expert/moderator
```

系统需要：

- 标记 expert/moderator 的校准信号。
- 把高质量反馈转成 alignment event。
- 对个人状态和关系状态做版本化更新。

## 关键技术机制

### Memory Write Policy

不是所有内容都写入长期记忆。需要判断：

- 是否稳定。
- 是否重要。
- 是否重复。
- 是否敏感。
- 是否需要用户确认。

### Memory Read Policy

生成回复前，需要决定读取哪些记忆：

- 当前用户个人记忆。
- 对话对象的可见记忆。
- 两人关系记忆。
- 当前会话短期记忆。
- 相关知识。

### Alignment Event

当前已有事件：

```text
PERSONAL_MEMORY_UPDATE
RELATIONSHIP_UPDATE
EXPERT_REVIEW
EXPERT_EDIT
EVALUATION
```

后续应扩展：

```text
USER_CORRECTION
PEER_FEEDBACK
MEMORY_CONFIRMATION
MEMORY_DELETE_REQUEST
PRIVACY_BOUNDARY_UPDATE
COMMUNITY_RULE_UPDATE
```

## 当前代码状态

已实现：

- `PersonalAlignmentState`
- `RelationshipState`
- `PersonalAlignmentService`
- `RelationshipService`
- `AICOOrchestrator` 内的个人和关系状态更新。
- smoke test 覆盖个人状态和两人关系状态。

未实现：

- 持久化数据库。
- 记忆可视化和编辑。
- 多人社区关系图谱。
- 隐私权限。
- 记忆写入确认。
- 前端交互。
- 与真实后端 API 的整合。

## 设计原则

AICO 不应只是“更会回答问题”，而应是：

```text
一个持续理解用户、理解关系、理解社区语境，并能被用户和可信校准者持续修正的交互式对齐系统。
```

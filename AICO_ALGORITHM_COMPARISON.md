# AICO 算法逻辑对比与讨论稿

本文用于讨论当前 `AICO/aico` 算法逻辑相对 `AICO_April` 的变化。结论先说清楚：**当前版本不是推翻 April，而是在 April 已有五阶段咨询/RAG/专家 prompt 链路前增加长期个人对齐和关系对齐层。**

## 1. AICO_April 的算法主线

April 的算法入口主要在：

```text
algorithm/backend_mock/src_reconstruct/dialog_generation.py
algorithm/backend_mock/src_reconstruct/stage_strategy.py
algorithm/backend_mock/src_reconstruct/knowledge_retrieval.py
algorithm/backend_mock/src_reconstruct/input_processing.py
```

核心流程：

```text
education_agent_reply
  -> get_chat_memory(knowledge_base_uuid)
  -> stage_choose_agent(policy_state, chat_history, input_text)
  -> key_word_agent(input_text)
  -> knowledge_retrival_agent(...)
  -> reply_agent(...)
  -> reply_mess(...)
  -> gpt_model.azure_gpt_mess(messages)
  -> chat_knowledge_memory 写回历史
```

April 的关键能力：

- 五阶段咨询推进：建立关系、收集信息、推理根源、制定方案、结束咨询。
- RAG/知识检索：根据用户输入、关键词、会话状态检索专业知识。
- 专家 prompt：支持 CBT 等专家角色。
- 历史会话记忆：围绕 `knowledge_base_uuid` 管理当前会话的历史、阶段、知识、策略。

April 的局限：

- 主要是“单个咨询会话”的记忆，不是跨场景、跨对象的长期个人对齐。
- 缺少“我和这个对方长期如何互动”的关系记忆。
- 专家采纳、修改、拒绝 AI 建议的行为还没有系统沉淀为可学习的 alignment signal。
- 五阶段是流程控制树，但不是严格意义上的专家逻辑节点树；它不能完整表达可编辑、可版本化、可分支追踪的 ThoughtTree。

## 2. 当前 AICO/aico 修改后的算法主线

新增文件：

```text
algorithm/backend_mock/src_reconstruct/aico_alignment.py
algorithm/backend_mock/src_reconstruct/aico_topic_graph.py
algorithm/backend_mock/src_reconstruct/aico_strategy_tree.py
```

已修改：

```text
algorithm/backend_mock/src_reconstruct/dialog_generation.py
```

当前流程：

```text
education_agent_reply
  -> get_chat_memory(knowledge_base_uuid)
  -> stable_user_id(...)
  -> stable_counterpart_id(...)
  -> update_personal_alignment(...)
  -> update_relationship_alignment(...)
  -> build_alignment_context(...)
  -> extract_candidate_topic(...)
  -> merge_or_create_topic(...)
  -> select_or_create_strategy_tree(...)
  -> record_tree_execution(...)
  -> stage_choose_agent(...)
  -> knowledge_retrival_agent(...)
  -> reply_agent(..., alignment_context, strategy_context)
  -> reply_mess(..., alignment_context, strategy_context)
  -> prompt 中注入 AICO长期个人/关系对齐记忆 + 动态 topic/tree 上下文
  -> gpt_model.azure_gpt_mess(messages)
```

新增的对齐状态包括：

- `PersonalAlignmentState`
  - 用户历史交互次数
  - 长期关注主题
  - 稳定画像线索
  - 近期表达线索
  - 已确认偏好/边界

- `RelationshipAlignmentState`
  - 双方历史交互次数
  - 双方共同议题
  - 关系沟通注意点
  - 近期关系互动线索

当前保存位置：

```text
algorithm/backend_mock/src_reconstruct/knowledge_chat/aico_alignment_state.json
algorithm/backend_mock/src_reconstruct/knowledge_chat/aico_topic_graph_state.json
algorithm/backend_mock/src_reconstruct/knowledge_chat/aico_strategy_tree_state.json
```

这只是第一版轻量存储，后续应迁移到数据库、事件流和向量检索系统。

## 3. 五阶段、ThoughtTree、对话逻辑树的关系

这里需要修正一个关键概念：**ThoughtTree 不应该只理解为专家逻辑树，它更应该升级为 AICO 的 Dialogue Strategy Tree，即对话策略树。**

对话逻辑树的作用是在宏观层面组织多轮对话，不只服务专家咨询，也服务日常聊天。它回答的是：

- 当前对话属于什么话题或目的？
- 双方关系是刚接触、一般熟悉、很熟悉，还是关系紧张？
- 当前处于这个话题的哪个阶段？
- 什么时候应该寒暄、铺垫、收集信息、推进目的、表达请求、收束对话？
- 如果对方拒绝、回避、情绪变化，应该跳到哪个分支？

因此目前更准确的判断是：

```text
Topic Graph != Dialogue Strategy Tree
五阶段回复 != Dialogue Strategy Tree
专家逻辑树 = Dialogue Strategy Tree 在专家-client 场景下的一种实例
日常聊天逻辑树 = Dialogue Strategy Tree 在 PERSONAL / PEER 场景下的一种实例
```

Topic Graph 和 Tree 的分工：

```text
Topic Graph:
  负责动态 topic 抽取、语义合并、topic 关联和树生命周期决策。
  它判断当前输入是否应复用已有 topic/tree，还是创建新 topic/tree。

Dialogue Strategy Tree:
  负责一个 topic/intent/relationship state 下多轮对话如何推进。
  它定义节点目标、对话动作、跳转条件、RAG 需求、风险边界和执行轨迹。
```

判断逻辑：

```text
topic 相似 + intent 相似 + relationship state 相似
  -> 沿用已有 topic 和已绑定 tree

topic 相似但 intent/relationship state 有明显差异
  -> 复用 topic，但创建候选分支或候选 tree

topic 只是同义表达
  -> 合并 topic，不新建 tree

topic 明显新
  -> 新建 topic node，并生成候选 Dialogue Strategy Tree
```

April 的五阶段机制解决的是：

- 当前咨询推进到哪一步？
- 当前阶段应该问什么、避免什么？
- 是否可以进入下一阶段？

Dialogue Strategy Tree 应该解决的是：

- 某类话题/目的有哪些阶段和节点？
- 节点之间的触发条件是什么？
- 对方回答后走哪条边？
- 当前关系亲疏程度如何影响回复策略？
- 当前目的是否应该直接表达、延迟表达、试探表达，还是放弃表达？
- 哪些高频临时分支需要固化成稳定节点？
- 在专家模式下，不同专家流派/版本之间如何切换？

所以当前更合理的路线是：

```text
Topic Graph = tree lifecycle manager
Dialogue Strategy Tree = 多轮对话宏观组织器
五阶段 = 专家咨询类 Dialogue Strategy Tree 的一个高层阶段模板
长期对齐记忆 = 跨会话、跨关系的个体化上下文
RAG/KG = 为当前节点提供记忆、关系、知识、策略 grounding
专家/用户反馈 = 迭代更新信号
```

举例：

```text
借钱场景 Dialogue Strategy Tree
  -> 关系距离判断：刚认识 / 普通熟人 / 很熟 / 关系紧张
  -> 开场策略：寒暄 / 近况关心 / 共同背景
  -> 铺垫阶段：说明最近状态 / 建立可信背景
  -> 目的试探：观察对方是否愿意继续听
  -> 明确请求：金额、期限、还款方式
  -> 拒绝分支：保全面子 / 降低请求 / 结束对话
  -> 接受分支：确认细节 / 表达感谢 / 后续记录
```

```text
八卦场景 Dialogue Strategy Tree
  -> 关系与信任判断
  -> 话题安全性判断
  -> 轻量试探
  -> 信息交换
  -> 隐私边界控制
  -> 关系影响评估
  -> 收束或转移话题
```

```text
专家-client 场景 Dialogue Strategy Tree
  -> 建立关系
  -> 明确问题
  -> 收集事实
  -> 推理根源
  -> 制定方案
  -> 风险/伦理审查
  -> 总结与跟进
```

## 4. 从专家对齐扩展到社区长期对齐

你现在描述的 AICO 已经不只是“专家与 client 的对齐”，而是一个社区级长期个人化系统：

```text
每个用户
  -> 拥有自己的长期对齐系统
  -> 和专家互动时，专家反馈会校准系统
  -> 和其他用户互动时，关系记忆会逐渐形成
  -> 和自己对话时，目标、偏好、边界会持续沉淀
  -> 系统在未来生成回复时同时理解自己和对方
```

因此算法层建议拆成四类记忆：

1. 个人稳定记忆：身份、长期目标、偏好、表达风格、边界。
2. 个人动态记忆：近期情绪、近期议题、目标变化、状态波动。
3. 关系记忆：与某个专家、client、朋友、家庭成员或社区成员的互动模式。
4. 校准记忆：专家/用户对 AI 回复的修改、采纳、拒绝、评分和原因。

但这四类记忆本身还不够。AICO 还需要一个跨场景的对话组织层：

5. 对话策略树：按话题、目的、关系距离和多轮阶段组织对话。

所以 PERSONAL 和 EXPERT 不应该是“有没有逻辑树”的区别，而是“对齐主体、确认权和 AI 扩展权”的区别：

```text
PERSONAL:
  个人画像 + 关系画像 + 偏好记忆 + 日常/目的型 Dialogue Strategy Tree + 个人/关系/场景 RAG
  AI 可以更主动生成候选树，也可以基于反馈形成 ai_confirmed；用户可 user_confirmed、修改或撤销

EXPERT:
  expert 画像/流派 + 专家偏好 + 专家知识结构 + 专业 Dialogue Strategy Tree + 专业 RAG + 风险评估 + 专家反馈
  client 只是正常交互输入和案例来源，不作为长期对齐主体
  AI 只能基于专家已有树做小幅候选节点/分支扩展，专家拥有最高编辑和确认权
```

## 5. 当前第一版还不完善的地方

已经完成：

- April 算法主链路中注入长期个人/关系对齐上下文。
- Spring 后端新增对齐 API。
- Vue 前端新增 `/aico-alignment` 工作台。
- README 已改为 April 母版 + aico 主开发目录的口径。

仍需继续做：

- 后端对齐状态目前是内存版，需要持久化。
- 前端工作台还没有和真实聊天页合并。
- 专家采纳/修改 AI 推荐的行为还没有自动写入对齐反馈接口。
- Dialogue Strategy Tree 当前只是以 prompt 上下文形式接入，还没有成为严格节点执行器。
- Topic Graph 当前使用轻量词项相似度 fallback，后续应替换为 LLM 结构化抽取 + embedding 语义相似度 + 人工确认。

## 6. 建议下一步算法路线

下一轮可以优先做这三件事：

1. 在 `ParentChat.vue` / `ExpertChat.vue` 的发消息流程里调用 `/aico/alignment/turns`。
2. 在 `ExpertAIController` 的 instruction/adopt/recommend 流程中写入 `/aico/alignment/feedback` 对应服务。
3. 把 ThoughtTree-main 中的节点/边/条件结构抽象成 `AICO Dialogue Strategy Tree`，让 PERSONAL 和 EXPERT 都通过它组织多轮对话。

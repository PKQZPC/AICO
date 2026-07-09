# AICO Dynamic Alignment Plan

本文记录当前已对齐的核心理解与修改计划。重点是：AICO 不做固定场景枚举，而是通过动态 Topic Graph、关系图谱和 Dialogue Strategy Tree 来组织长期 AI-human alignment。

## 完整理解

| 概念 | 当前共识 | 作用 |
|---|---|---|
| Topic Graph | topic 由 LLM/模型动态抽取生成，不做固定枚举；相似 topic 通过语义相似度合并 | 判断当前对话是否复用已有 topic，以及是否复用、扩展或新建策略树 |
| Relationship Graph | 关系状态采用连续维度 + LLM 描述，并维护人际关系网络图谱 | 让同一 topic 在不同关系下选择不同树或不同节点策略 |
| Dialogue Strategy Tree | 树不是分类器，而是多轮对话宏观组织器 | 定义当前阶段目标、对话动作、跳转条件、RAG 需求和风险边界 |
| PERSONAL | AI 可更主动生成候选树，也可基于反馈形成 `ai_confirmed`；用户可确认、修改或撤销 | 面向日常长期自我/关系对齐 |
| EXPERT | AI 只能基于专家已有树做小幅候选扩展；专家拥有完整编辑、删除、新增、确认权 | 面向专家逻辑、风格、知识和服务流程对齐 |
| RAG | 不只检索专业知识，也检索个人记忆、关系记忆、历史相似 topic、历史策略树和反馈 | 为当前树节点提供 grounding |
| Feedback | PERSONAL 综合用户、对方、系统观察和 LLM 评估；EXPERT 专家反馈最高权重 | 决定 topic/tree 是否合并、扩展、固化或废弃 |

## Topic Graph 与 Tree 的关系

| 判断情况 | Topic Graph 决策 | Tree 决策 |
|---|---|---|
| topic/intent/relationship 都高度相似 | 复用已有 topic | 沿用该 topic 绑定的 tree |
| topic 相似，但 intent 或 relationship state 不同 | 复用或关联 topic | 创建候选分支或候选 tree |
| 新 topic 是旧 topic 的同义表达 | 合并 topic | 不新建 tree |
| topic 明显新 | 新建 topic node | 动态生成候选 tree |
| 当前 tree 无法覆盖新回复路径 | 保留 topic | PERSONAL 可生成候选节点并由 AI/用户确认；EXPERT 只能生成候选节点并等待专家确认 |

## 本次已完成的修改

| 文件 | 修改内容 | 为什么 |
|---|---|---|
| `algorithm/backend_mock/src_reconstruct/aico_topic_graph.py` | 新增动态 topic 候选、相似度匹配、合并/新建、tree policy 机制 | 避免固定场景枚举，让 topic 决定树的生命周期 |
| `algorithm/backend_mock/src_reconstruct/aico_strategy_tree.py` | 新增 Dialogue Strategy Tree 候选树、确认策略、执行轨迹 | 让树承担多轮对话组织，而不是 topic 分类 |
| `algorithm/backend_mock/src_reconstruct/aico_alignment.py` | 关系状态加入连续维度和 LLM 描述字段 | 支持关系图谱式对齐 |
| `algorithm/backend_mock/src_reconstruct/dialog_generation.py` | 将 Topic Graph 与 Strategy Tree 上下文注入 April 原回复链路 | 保留 April 可运行主干，同时接入新的对齐结构 |
| `README.md` | 更新总设计与当前流程 | 保证后续开发不再回到固定场景分类 |
| `AICO_ALGORITHM_COMPARISON.md` | 更新算法对比与 Topic/Tree 关系 | 便于继续讨论算法设计 |
| `frontend/src/views/ExpertScenario.vue` | 新增 EXPERT 场景入口 | 明确专家场景对齐专家，client 只是交互输入 |
| `frontend/src/views/PersonalWorkbench.vue` | 新增 PERSONAL 完整工作台第一版 | 避免 PERSONAL 继续复用专家端 `/expert-chat`，体现每个人都是自己的专家端 |
| `frontend/src/router/index.ts` | `/expert-client` 改为重定向到 `/expert` | 避免继续使用旧 expert-client 命名造成“对齐 client”的误解 |

## 前端场景边界

| 场景 | 前端形态 | 当前入口 | 说明 |
|---|---|---|---|
| PERSONAL | 入口页 + 个人完整工作台 | `/personal` -> `/personal-workbench` | 每个用户都是自己长期对齐系统的 owner，也拥有类似专家端的完整能力 |
| EXPERT | 专家完整工作台 + client 简单输入端 | `/expert` -> `/expert-chat` / `/parent-chat` | 专家是 aligned subject；client 只是正常输入与案例来源，不作为长期对齐主体 |
| 个人长期对齐观察 | 对齐状态页 | `/aico-alignment` | 查看个人画像、关系画像、topic/tree 上下文 |
| 策略树编辑 | 通用树编辑器 | `/decision-tree` | 后续应升级为 PERSONAL 与 EXPERT 共用的 Strategy Tree Editor |

需要注意：PERSONAL 不是 client 端。PERSONAL 中“个人用户”也应拥有完整工作台，因为他要确认自己的 topic、关系描述、偏好记忆和策略树。EXPERT 则保留两个前端：专家端复杂，client 端简单；对齐的是专家，client 只是选择专家、打字聊天、触发专家树和专家回复策略的交互输入。

## 确认状态

| 状态 | PERSONAL | EXPERT |
|---|---|---|
| `candidate` | AI 生成的候选 topic/tree/profile/relationship | AI 生成的候选专家节点/分支 |
| `ai_confirmed` | AI 根据用户、对方、系统观察和 LLM 评估自动确认，必须可解释、可撤销 | 不用于专家树固化 |
| `user_confirmed` | 用户本人确认，是 PERSONAL 的强确认 | 仅作为辅助反馈 |
| `expert_confirmed` | 不适用 | 专家确认，是 EXPERT 的唯一强确认 |
| `rejected` | 用户拒绝或系统废弃 | 专家拒绝 |
| `archived` | 保留历史但不活跃 | 旧专家树版本或废弃分支 |

## 后续修改优先级

| 优先级 | 任务 | 说明 |
|---|---|---|
| P0 | 将 fallback topic 抽取替换为 LLM 结构化抽取 | 输出 topic、intent、goal、confidence、relationship relevance |
| P0 | 将词项相似度替换为 embedding 语义相似度 | 解决相似话题反复存入问题 |
| P1 | 将 Topic Graph / Strategy Tree 状态迁移到后端数据库 | 当前是 JSON 轻量状态，不能作为长期生产存储 |
| P1 | 前端新增 topic/tree/relationship 可视化确认入口 | PERSONAL 标清 AI 确认/用户确认/待确认；EXPERT 专家确认 |
| P1 | 专家端 DecisionTree 升级为通用 Strategy Tree Editor | 兼容专家树和日常树 |
| P1 | 显式区分 aligned subject 与 interaction partner | PERSONAL aligned subject 是自己；EXPERT aligned subject 是专家，client 是 partner/input |
| P2 | Strategy Tree 从 prompt 上下文升级为严格节点执行器 | 真正控制当前节点、边跳转、执行轨迹 |
| P2 | PersonalWorkbench 后端接入 | `/personal-workbench` 已有第一版前端雏形，需要接入真实消息流、个人身份、Topic Graph、Strategy Tree 和 RAG |
| P2 | 专家候选节点确认 UI | EXPERT 中 AI 生成候选节点后必须等待专家确认 |
| P2 | 引入多源反馈评估器 | 综合用户、对方、专家、系统观察和 LLM 评估 |

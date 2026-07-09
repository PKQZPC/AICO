# AICO algorithm 算法端开发

本目录来自 `AICO_April/algorithm`，是当前 `AICO/aico` 的真实算法主干。后续算法迭代在本目录进行，`AICO_April` 保留为原始母版。

## April 原主线

April 原算法主要由以下模块构成：

```text
backend_mock/src_reconstruct/dialog_generation.py
backend_mock/src_reconstruct/stage_strategy.py
backend_mock/src_reconstruct/knowledge_retrieval.py
backend_mock/src_reconstruct/input_processing.py
```

原流程：

```text
输入分类
  -> get_chat_memory
  -> 五阶段 stage_choose_agent
  -> 关键词抽取
  -> RAG/知识检索
  -> 专家 prompt / CBT prompt
  -> 生成回复
  -> 写回 chat_knowledge_memory
```

## 当前 AICO 新增对齐层

新增：

```text
backend_mock/src_reconstruct/aico_alignment.py
```

已接入：

```text
backend_mock/src_reconstruct/dialog_generation.py
```

新流程：

```text
用户输入
  -> 更新长期个人对齐记忆
  -> 更新关系对齐记忆
  -> 构造 alignment_context
  -> 进入 April 五阶段/RAG/专家回复流程
  -> prompt 中注入长期个人/关系上下文
  -> 输出更贴合用户和对方关系的回复
```

当前长期记忆存储：

```text
backend_mock/src_reconstruct/knowledge_chat/aico_alignment_state.json
```

## 启动

1. 安装 `requirements.txt` 下的包。

2. 运行 April 原服务：

```powershell
python backend_mock/src/parent_app.py
```

或根据后续重构，改为运行 `src_reconstruct` 下的服务入口。

## 进一步讨论

详见上级目录：

```text
AICO_ALGORITHM_COMPARISON.md
```

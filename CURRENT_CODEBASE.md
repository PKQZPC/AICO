# AICO Current Codebase

当前 `aico/` 已按 `AICO_April` 最新代码重新整理。`AICO_April` 是当前产品代码主线；之前搭建的 alignment core 保留为算法框架和后续重构目标。

## Source of Truth

```text
AICO_April/
  algorithm/
  backend/
  fronted/
```

已经同步到：

```text
aico/
  algorithm/
  backend/
  frontend/
```

同步方式是复制，不删除 `AICO_April`，不删除根目录参考代码。

## Current Layout

```text
aico/
  algorithm/
    backend_mock/
      src_reconstruct/
      src/
      tools/
      utils/
    requirements.txt
    readme.md

  backend/
    pom.xml
    src/main/java/com/project/smart_intervention/
    src/main/resources/application.yml

  frontend/
    package.json
    vite.config.ts
    src/views/
    src/api/
    src/stores/
    src/components/

  api/
  alignment/
  decision/
  evaluation/
  generation/
  knowledge/
  perception/
  storage/

  legacy/
    backend_gateway/
    backend_mock/
    frontend_app/
    thought_tree_main/
```

## What is Product Code Now

The current product code should be considered:

- `aico/frontend`: Vue 3 + Vite frontend from April.
- `aico/backend`: Spring Boot backend from April.
- `aico/algorithm`: Python algorithm service from April.

## What is Framework Support Code

These directories are the clean AICO alignment framework built for future integration:

- `aico/api`
- `aico/alignment`
- `aico/decision`
- `aico/evaluation`
- `aico/generation`
- `aico/knowledge`
- `aico/perception`
- `aico/storage`

They currently run independently with an in-memory demo and tests. They are not yet fully wired into April frontend/backend/algorithm runtime.

## Important Integration Gap

The latest April code has real frontend/backend/algorithm assets, but it is not yet a unified AICO architecture:

- Frontend calls the April backend API, not the new alignment core.
- Backend talks to algorithm endpoints, but not the new personal/relationship alignment services.
- Algorithm has `src_reconstruct`, which should become the main algorithm path.
- The new long-term personal alignment and relationship memory exist in clean framework code, but not yet in the Java backend database or Vue UI.

## Next Integration Target

The next implementation should connect these layers:

```text
frontend
  -> backend
  -> algorithm/src_reconstruct
  -> alignment core
  -> personal memory + relationship memory + expert feedback events
```

Recommended next files to bridge:

```text
aico/backend/src/main/java/com/project/smart_intervention/expert_ai/
aico/backend/src/main/java/com/project/smart_intervention/decision_tree/
aico/backend/src/main/java/com/project/smart_intervention/message/
aico/frontend/src/views/ParentChat.vue
aico/frontend/src/views/ExpertChat.vue
aico/frontend/src/views/DecisionTree.vue
aico/algorithm/backend_mock/src_reconstruct/agent_new_reconstruct.py
aico/algorithm/backend_mock/src_reconstruct/dialog_generation.py
aico/algorithm/backend_mock/src_reconstruct/stage_strategy.py
```

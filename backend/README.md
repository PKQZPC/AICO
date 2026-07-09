# AICO Backend Target

当前 `aico` 已有算法编排器 `api/gateway_adapter.py`，但还没有正式 HTTP 后端。

后端目标：

- expose `AICOOrchestrator` as service APIs
- persist personal and relationship states
- manage users, conversations, expert reviews, and alignment events
- connect frontend user app, community chat, and expert console

Legacy reference:

```text
aico/legacy/backend_gateway
```

Next implementation should happen here, under `aico/backend`.

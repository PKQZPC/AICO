<h1 align="center">
  <img src="./assets/AICO2.png" alt="AICO icon" width="88" />
</h1>

<p align="center">
  <b>An AI companion framework that grows with people, relationships, and expertise over time</b>
</p>

<p align="center">
  English | <a href="./README_zh.md">简体中文</a>
</p>

<p align="center">
  <b>Version:</b> v0.1.0-alpha &nbsp; | &nbsp;
  <b>Status:</b> Active refactor in progress &nbsp; | &nbsp;
  <b>Technical Report:</b> Coming soon
</p>

AICO explores a simple idea: AI should not only answer what you just said, but gradually understand who you are, who you are talking to, what matters between you, and how conversations should evolve over time.

The project aims to make AI feel less like a stateless tool and more like a long-term collaborator: one that can learn personal preferences, adapt to different relationships, support expert workflows, and improve through human feedback.

---

## Why AICO

Most AI assistants respond mainly to the latest message. AICO explores a different direction: an AI system should become more aligned as it interacts with a person or expert over time.

```text
Long-term interaction
  -> personal or expert profile
  -> owner-private relationship graph
  -> dynamic topic graph
  -> dialogue strategy tree
  -> retrieval-augmented response
  -> feedback and confirmation
  -> iterative update
```

AICO focuses on four questions:

| Question | Meaning |
|---|---|
| Who is the aligned subject? | The user themself in PERSONAL mode, or the expert in EXPERT mode. |
| Who is the current interaction partner? | A friend, family member, colleague, self-dialogue partner, or client. |
| What is the current topic and multi-turn purpose? | The dynamic topic, intent, relation context, and dialogue stage. |
| How should the system evolve? | Profiles, relationship graph, knowledge, strategy trees, and confirmation records are updated over time. |

---

## Core Idea

AICO is built around a write/read alignment loop.

```text
WRITE -- update alignment state                     READ -- generate aligned context
──────────────────────────────────────              ─────────────────────────────────────
chat message   -> structured extraction             user query     -> context routing
feedback       -> patch + merge                     relationship   -> subgraph retrieval
expert edit    -> confirmation record               topic          -> tree selection
interaction    -> graph / tree evolution            strategy node  -> response guidance
```

### Write

1. Normalize the mode, aligned subject, interaction partner, conversation, and message.
2. Extract dynamic topics from dialogue instead of selecting from fixed scene labels.
3. Update personal, expert, client-service, and relationship states through structured patches.
4. Select, reuse, extend, or create a strategy tree for the current topic and relationship context.
5. Record confirmation sources from AI, user, expert, or system.

### Read

1. Retrieve the aligned subject profile.
2. Retrieve the current topic and related strategy tree.
3. In PERSONAL mode, retrieve the owner-private relationship subgraph around the current partner.
4. In EXPERT mode, retrieve the expert profile and client service profile.
5. Build compact context for response generation or expert decision support.

---

## Two Alignment Modes

| Mode | Aligned Subject | Interaction Partner | Purpose |
|---|---|---|---|
| PERSONAL | The user themself | Self, friends, family, colleagues, other people | Learn the user's identity, preferences, memories, relationship network, and everyday dialogue strategies. |
| EXPERT | The expert | Client | Align with the expert's logic, style, knowledge structure, decision tree, and service workflow. |

In EXPERT mode, the client is modeled as a service context for better expert-side replies. The client is not the long-term aligned subject.

---

## What AICO Builds

| Layer | Meaning | Used for |
|---|---|---|
| Personal Profile | Stable and dynamic profile of the user | Long-term personal alignment |
| Expert Profile | Expert style, school, knowledge structure, preferences | Expert logic alignment |
| Client Service Profile | Client case context, current need, risk signals, communication style | Supporting expert replies |
| Relationship Graph | Owner-private graph of people, relationships, events, constraints, and strategies | Reading relationship context before replying |
| Topic Graph | Dynamic semantic graph of extracted topics | Reuse, extend, or create strategy trees |
| Strategy Tree | Macro-level dialogue logic tree | Organizing multi-turn conversation flow |
| RAG Memory | Professional knowledge, personal memory, relationship memory, historical strategy | Compact retrieval for generation |
| Confirmation Records | AI/user/expert/system confirmation source | Traceability, revision, and governance |

---

## Owner-private Relationship Graph

AICO's relationship graph is not a global social graph. Each user owns a private graph built from their own conversations.

For user `me`, AICO may build:

```text
me
├── A
│   ├── A's parent
│   └── A's colleague
├── B
│   └── B's mother
└── C
```

When `me` chats with `A`, AICO retrieves a local subgraph instead of the whole graph.

| Retrieved Context | Why |
|---|---|
| Direct `me-A` edge | Relationship state, events, trust, tension |
| Person nodes for `me` and `A` | Profiles, preferences, constraints |
| Topic-relevant one-hop or two-hop relations | Background people and indirect context |
| Strategy implications | How relationship should affect the reply |

AICO's rule of thumb:

> The LLM proposes what should be updated; the system controls evidence, merging, deduplication, confidence, permissions, and persistence.

---

## Current Features

| Feature | Status |
|---|---|
| PERSONAL and EXPERT mode separation | Implemented in algorithm layer and frontend routes |
| Dynamic topic extraction | LLM-compatible extractor with local fallback |
| Topic graph reuse / merge / create | Implemented |
| Strategy tree runtime | First executable version |
| Owner-private relationship graph | First version implemented |
| Multi-source RAG | Supports knowledge, personal memory, relationship memory, strategy memory, expert profile, and client service profile |
| Expert/client frontend structure | April frontend retained and extended |
| Java alignment endpoints | Extended with AICO state, client service profile, and relationship edge details |
| Technical report | Coming soon |

---

## Repository Layout

```text
aico/
├── api/                 # Shared schemas and AICOOrchestrator
├── alignment/           # Topic extraction, topic graph, embedding, iteration jobs
├── perception/          # Personal state, profile service, relationship graph
├── decision/            # Thought tree and strategy tree runtime
├── knowledge/           # Retrieval and multi-source RAG
├── evaluation/          # Response and feedback evaluation
├── generation/          # Prompt and response generation
├── backend/             # Spring Boot backend extended from April
├── frontend/            # Vue frontend extended from April
├── algorithm/           # Preserved and reconstructed algorithm assets
├── storage/             # JSON state store for current development
└── tests/               # Python tests
```

---

## Quick Start

### Python Environment

AICO uses a dedicated conda environment named `aico`.

```powershell
conda env create -f environment.yml
conda activate aico
python -m pip install -e .
```

Run Python tests:

```powershell
python -m unittest discover -s tests
```

Expected current result:

```text
Ran 7 tests
OK
```

### Python Example

```python
from aico import AICOOrchestrator
from aico.api.schemas import AICOTurnInput, ClientMessage, InteractionMode

orchestrator = AICOOrchestrator()

output = orchestrator.process_client_message(
    AICOTurnInput(
        interaction_mode=InteractionMode.PERSONAL,
        counterpart_id="A",
        message=ClientMessage(
            client_id="user_1",
            conversation_id="conv_personal_A",
            text="I want to contact A again, but I do not want to pressure him.",
            metadata={"relationship_type": "friend"},
        ),
    )
)

print(output.response.text)
print(output.response.metadata["active_relationship_subgraph"])
```

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

| Route | Meaning |
|---|---|
| `/personal` | PERSONAL entry |
| `/personal-workbench` | Personal alignment workbench |
| `/expert` | EXPERT entry |
| `/expert-chat` | Expert workspace |
| `/parent-chat` | Lightweight client chat |
| `/decision-tree` | Strategy tree editor |
| `/aico-alignment` | Alignment state viewer |

### Backend

```powershell
cd backend
mvn spring-boot:run
```

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/aico/alignment/turns` | Record a turn and update alignment state |
| GET | `/api/aico/alignment/users/{userId}/state` | Get aligned subject state |
| GET | `/api/aico/alignment/relationships` | Get PERSONAL relationship state |
| POST | `/api/aico/alignment/feedback` | Record user or expert feedback |

---

## LLM Configuration

Topic and relationship extraction are designed around an OpenAI-compatible chat endpoint. If no endpoint is configured, AICO uses conservative local fallback logic so tests and offline development remain runnable.

```powershell
$env:AICO_LLM_ENDPOINT="http://localhost:11434/v1/chat/completions"
$env:AICO_LLM_API_KEY="local-dev-key"
$env:AICO_LLM_MODEL="your-local-model"
```

Embedding currently uses a deterministic local implementation for development and tests. It can later be replaced with a production embedding provider.

---

## Roadmap

| Area | Next Step |
|---|---|
| Technical report | Release AICO technical report and architecture diagrams |
| Relationship graph UI | Click an edge to inspect people, events, evidence, constraints, and confirmation status |
| Strategy tree executor | Tighten node transition control and bind traces to real conversations |
| Expert confirmation workflow | Complete candidate topic/tree/node confirmation in expert workbench |
| Persistent backend | Move JSON state toward database or event-stream persistence |
| LLM extraction | Replace local fallback with stronger structured extraction and production embeddings |

---

## Development Notes

- Current active development directory: `AICO/aico`.
- Earlier April code is preserved as reference material and is being reorganized into AICO naming and architecture.
- Legacy names such as `SynergyAdvisor`, `Digital-avatar`, and `meta_ware_user` should be treated as historical names and gradually unified under `AICO`.

---

## License

AICO is released under the [GNU Affero General Public License v3.0](./LICENSE) (AGPL-3.0).

<div align="center">

```
╔════════════════════════════════════════════════════╗
║          T E L E C O M O P S   A I                ║
║      Telecom AI Operations Agent                   ║
║      Java 21 · Spring Boot 3 · React · GPT-4o      ║
╚════════════════════════════════════════════════════╝
```

![Java](https://img.shields.io/badge/Java-21_LTS-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-18.3-61DAFB?style=flat-square&logo=react)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o--mini-412991?style=flat-square&logo=openai)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/kadriyeisik/telecomops-ai-agent)

**A domain-specific AI agent that diagnoses and resolves telecom network issues through a fully automated, multi-step diagnostic pipeline.**

*PIA Group AI Training Assignment — built from scratch, not an n8n template.*

</div>

---

## What Makes This Different

Most AI "agents" in demos are just chatbots with a few tools bolted on. This project is architected as a real operations system:

| Dimension | Generic Chatbot | TelecomOps AI |
|-----------|----------------|---------------|
| Decision process | Fixed prompt → one answer | 7-tool diagnostic pipeline, LLM decides the sequence |
| Memory | Conversation only | Persistent case history per customer in H2 DB |
| Trend analysis | ❌ | Signal degradation, station load growth, recurring-issue detection |
| Escalation logic | ❌ | Auto-recommends NOC escalation after 2+ recurring issues |
| Loading experience | Spinner | Live step-by-step progress animation in UI |
| UI | Chat bubble | Dual-pane: chat left, live diagnostic trace right |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Browser (localhost:5173)                    │
│                                                                     │
│  ┌──────────────────────┐   ┌────────────────────────────────────┐  │
│  │    Chat Column       │   │       Agent Trace Panel            │  │
│  │                      │   │                                    │  │
│  │  User message        │   │  🔍 intent_analysis      ✓         │  │
│  │  ──────────          │   │  👤 customer_verification ✓        │  │
│  │  AI response         │   │  📶 signal_check          ✓        │  │
│  │  (with Markdown)     │   │  🗼 base_station_load     ✓        │  │
│  │                      │   │  📦 active_package_check  ✓        │  │
│  │  [Input bar]         │   │  📋 case_history_check    ✓        │  │
│  │                      │   │  ✅ suggest_resolution    ✓        │  │
│  └──────────────────────┘   │  ─────────────────────────        │  │
│                              │  Intent: SLOW_DATA  MEDIUM        │  │
│                              │  ✓ Diagnosis Complete              │  │
│                              │  6 tools · 4.21 s                 │  │
│                              └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
              │ REST / JSON (POST /api/chat)
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring Boot (localhost:8080)                     │
│                                                                     │
│  AgentService ──► LlmClient interface                               │
│       │                │                                            │
│       │           ┌────┴─────┐                                      │
│       │     OpenAiService   OllamaService  (switchable)             │
│       │                                                             │
│  ToolRegistry (auto-discovers all @Component tools)                 │
│       │                                                             │
│  ┌────┴──────────────────────────────────────────┐                  │
│  │  IntentAnalysisTool     CustomerVerification  │                  │
│  │  SignalCheckTool        BaseStationLoadTool   │                  │
│  │  ActivePackageCheckTool CaseHistoryTool       │                  │
│  │  ResolutionTool (auto-saves DiagnosticRecord) │                  │
│  └───────────────────────────────────────────────┘                  │
│                                                                     │
│  H2 In-Memory DB:  conversations · messages · diagnostic_records    │
│  TelecomDataStore: mock CRM / OSS / BSS (customers, plans, network) │
└─────────────────────────────────────────────────────────────────────┘
              │ function calling (tool definitions + results)
              ▼
         OpenAI API  (gpt-4o-mini)
```

### Agent Loop (`AgentService.java`)

```
User message
    │
    ▼
[Build message history] ──► POST to LLM (with all tool schemas)
                                │
                    ┌───────────┴───────────┐
                    │ tool_calls in response?│
                    └───────────┬───────────┘
                               YES → execute tool → append result → loop (max 5 iterations)
                               NO  → final answer returned to user
```

---

## Diagnostic Workflow

Every customer complaint triggers this 7-step pipeline:

```
Step 1  intent_analysis        Classify issue type + severity
Step 2  customer_verification  CRM lookup — account status, region, plan
Step 3  signal_check           Signal dBm, quality, technology (5G/4G+/4G)
Step 4  base_station_load      Tower load %, status, connected device count
Step 5  active_package_check   Data used/remaining, voice minutes, billing status
Step 6  case_history_check     Previous cases + trend analysis (NEW)
Step 7  suggest_resolution     Root-cause report + numbered action plan + ETA
                               └─ auto-saves a DiagnosticRecord to H2 DB
```

### Case History & Trend Engine

`case_history_check` compares the current live readings against historical `DiagnosticRecord` snapshots:

| Trend detected | Output |
|---------------|--------|
| Signal dBm dropped >5 dBm since last case | 📉 Signal degrading: −82 → −88 dBm |
| Station load grew >8% since last case | 📈 Station load increasing: 85% → 89% |
| Signal decline over 3+ cases | ⚠️ Long-term coverage degradation |
| Same issue type ≥2 times | 🚨 ESCALATE to Network Operations Team |

---

## Tech Stack

| Layer | Technology | Version | Notes |
|-------|-----------|---------|-------|
| Runtime | Java | 21 LTS | Virtual-thread ready |
| Framework | Spring Boot | 3.3.4 | Jakarta EE 10 |
| HTTP client | Spring WebFlux / WebClient | — | Reactive, non-blocking |
| Persistence | Spring Data JPA + H2 | — | In-memory, zero setup |
| Code gen | Lombok | 1.18.34 | Getters/setters/constructors |
| LLM (cloud) | OpenAI GPT-4o-mini | — | Function calling |
| LLM (local) | Ollama | any | llama3.1, qwen2.5 |
| Build | Apache Maven | 3.9.16 | |
| UI framework | React | 18.3.1 | Functional + hooks |
| UI bundler | Vite | 5.4.1 | HMR dev server |
| Styling | Plain CSS custom properties | — | Dark theme, no framework |

---

## Project Structure

```
pia-agent/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/piagroup/agent/
│       ├── AgentApplication.java
│       ├── config/
│       │   └── CorsConfig.java
│       ├── controller/
│       │   └── ChatController.java          POST /api/chat
│       ├── data/
│       │   ├── TelecomDataStore.java         Mock CRM/OSS/BSS (customers, plans, network)
│       │   └── DataInitializer.java          Seeds demo history at startup
│       ├── dto/
│       │   ├── ChatRequest.java
│       │   ├── ChatResponse.java
│       │   └── ToolCallInfo.java
│       ├── model/
│       │   ├── Conversation.java
│       │   ├── Message.java
│       │   └── DiagnosticRecord.java        ← case history snapshots
│       ├── repository/
│       │   ├── ConversationRepository.java
│       │   ├── MessageRepository.java
│       │   └── DiagnosticRecordRepository.java
│       ├── service/
│       │   ├── AgentService.java            Agent loop + system prompt
│       │   ├── LlmClient.java               Provider interface
│       │   ├── OpenAiService.java           @ConditionalOnProperty(llm.provider=openai)
│       │   └── OllamaService.java           @ConditionalOnProperty(llm.provider=ollama)
│       └── tool/
│           ├── Tool.java                    Interface (name/description/schema/execute)
│           ├── ToolRegistry.java            Auto-collects all @Component tools
│           ├── IntentAnalysisTool.java      intent_analysis
│           ├── CustomerVerificationTool.java customer_verification
│           ├── SignalCheckTool.java          signal_check
│           ├── BaseStationLoadTool.java      base_station_load
│           ├── ActivePackageCheckTool.java   active_package_check
│           ├── CaseHistoryTool.java         case_history_check  ← trend engine
│           └── ResolutionTool.java          suggest_resolution  ← auto-saves record
│
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx                          Layout, state, timing
        ├── App.css                          Dark theme, all styles
        ├── api.js                           sendMessage() fetch wrapper
        └── components/
            ├── MessageBubble.jsx
            ├── InputBar.jsx
            ├── TypingIndicator.jsx
            ├── DiagnosticProgress.jsx       ← live loading animation
            └── AgentTrace.jsx               Intent badge, step cards, final decision, footer
```

---

## Mock Customer Data

Pre-loaded at startup — use these phone numbers to trigger different diagnostic scenarios:

| Phone | Name | Account | Plan | Region | Demo Scenario |
|-------|------|---------|------|--------|---------------|
| `05325551234` | David Wilson | ACTIVE | 5G Pro Unlimited | Istanbul Asian | **Escalation** — 3 seeded SLOW_DATA cases with worsening signal (−80→−86 dBm) and rising load (68%→85%→89%) |
| `05329876543` | Bob Smith | ACTIVE | 4G Basic 10 GB | Ankara Central | **Quota exhaustion** — 9.8 GB of 10 GB used; previous case shows same issue |
| `05321111111` | Carol Davis | **SUSPENDED** | 4G Standard 30 GB | Izmir Coastal | **Account suspended** — resolution immediately flags billing contact |
| `05321234567` | Alice Johnson | ACTIVE | 5G Pro Unlimited | Istanbul European | **Good signal** — 1 past CALL_DROPS case (resolved), clean diagnostics |
| `05330001122` | Emma Brown | ACTIVE | 4G Standard 30 GB | Bursa Central | **Moderate load** — stable network, no history |

---

## Setup & Running

### Prerequisites

| Tool | Minimum Version | Check |
|------|----------------|-------|
| Java JDK | 21 | `java -version` |
| Apache Maven | 3.9 | `mvn -version` |
| Node.js | 18 | `node -version` |
| OpenAI API Key | — | [platform.openai.com](https://platform.openai.com/api-keys) |

### 1. Clone

```bash
git clone https://github.com/your-org/pia-agent.git
cd pia-agent
```

### 2. Configure the API Key

> ⚠️ **Security**: Never put your API key in `application.properties`. Always use an environment variable.

**Windows (PowerShell)**
```powershell
$env:OPENAI_API_KEY = "sk-..."
```

**macOS / Linux**
```bash
export OPENAI_API_KEY="sk-..."
```

To make it persistent, add the export to your shell profile (`~/.zshrc`, `~/.bashrc`) or use a `.env` file that is already in `.gitignore`.

### 3. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at **http://localhost:8080**

| Endpoint | Description |
|---------|-------------|
| `POST /api/chat` | Main agent endpoint |
| `GET /h2-console` | H2 database browser (JDBC: `jdbc:h2:mem:agentdb`, user: `sa`, pass: *(empty)*) |

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend opens at **http://localhost:5173** — connects to the backend automatically.

---

## LLM Provider: OpenAI vs Ollama

Switch providers without changing any Java code — only one property line changes:

### OpenAI (default, recommended)

```properties
# backend/src/main/resources/application.properties
llm.provider=openai
openai.model=gpt-4o-mini
```

### Ollama (free, local, no API key)

```properties
llm.provider=ollama
ollama.model=llama3.1
```

```bash
# Install from https://ollama.com, then:
ollama pull llama3.1
ollama serve
```

> **Note**: Ollama's tool-calling reliability varies by model. `llama3.1` and `qwen2.5` work best. `OllamaService.java` normalises the format differences.

---

## Example Interaction

**User**: `My mobile data is very slow. My number is 05325551234.`

**Agent runs 7 tools, then responds:**

```
**Issue Identified** — SLOW_DATA · Medium severity

**Customer** — David Wilson · ACTIVE · 5G Pro Unlimited

**Diagnostic Findings**
• Signal: −88 dBm · FAIR · 5G (coverage 94.2%)
• Base Station IST-ASI-3318: 89% load · HIGH_LOAD · 3,180 devices
• Package: Unlimited data (45.2 GB used this cycle)

**Case History & Trends**
• 3 previous SLOW_DATA cases on record
• 📉 Signal degrading: −80 → −88 dBm over 4 cases
• 📈 Station load increasing: 68% → 89% over 4 cases
• ⚠️ Long-term coverage degradation detected
• 🚨 RECOMMENDATION: Escalate to Network Operations Team

**Root Cause** — Persistent high load on IST-ASI-3318 with degrading signal

**Resolution Steps**
1. Connect to Wi-Fi as an immediate workaround.
2. Network Operations Center has been notified — investigation in progress.
3. Estimated relief: 2–4 hours as load-balancing completes.

**Estimated Resolution** — 2–4 hours
```

**Agent Trace Panel (sequential animation, 300 ms per card):**
```
🔍 step 1 · intent_analysis         ✓
👤 step 2 · customer_verification    ✓
📶 step 3 · signal_check             ✓
🗼 step 4 · base_station_load        ✓
📦 step 5 · active_package_check     ✓
📋 step 6 · case_history_check       ✓
✅ step 7 · suggest_resolution       ✓
─────────────────────────────────────
✓  Diagnosis Complete
   root cause  Persistent high load on IST-ASI-3318...
   est. resolution  2–4 hours
─────────────────────────────────────
Intent [ SLOW_DATA ] [ MEDIUM ]
7 tools executed          4.21 s
```

---

## Extending the Agent

Adding a new diagnostic tool takes **one file**:

```java
// backend/src/main/java/com/piagroup/agent/tool/RoamingCheckTool.java
@Component                         // ← Spring registers it automatically
public class RoamingCheckTool implements Tool {

    @Override public String getName() { return "roaming_check"; }

    @Override public String getDescription() {
        return "Checks whether international roaming is enabled and what roaming plan is active.";
    }

    @Override public Map<String, Object> getParametersSchema() {
        return Map.of("type", "object",
            "properties", Map.of(
                "phone_number", Map.of("type", "string", "description", "Customer phone")
            ),
            "required", new String[]{"phone_number"});
    }

    @Override public String execute(Map<String, Object> args) {
        // implementation
        return "Roaming: ENABLED · Plan: EU Zone 1 · Daily cap: €2.50";
    }
}
```

`ToolRegistry` picks it up at startup — no other changes needed. Add it to the system prompt workflow in `AgentService.java` and the frontend icon map in `AgentTrace.jsx`.

---

## API Reference

### `POST /api/chat`

**Request**
```json
{
  "conversationId": null,
  "message": "My data is slow. Number: 05325551234."
}
```

**Response**
```json
{
  "conversationId": 1,
  "reply": "**Issue Identified** — SLOW_DATA...",
  "trace": [
    {
      "step": 1,
      "toolName": "intent_analysis",
      "arguments": "{\"message\":\"My data is slow...\"}",
      "result": "Intent Type: SLOW_DATA | Severity: MEDIUM | ..."
    },
    ...
  ]
}
```

---

## Security Notes

| Risk | Mitigation |
|------|-----------|
| API key in source code | Key read from `OPENAI_API_KEY` env var via `${OPENAI_API_KEY:}` — no secret in files |
| API key in git history | `.gitignore` blocks `*.env` and `application-local.properties`; check with `git log -p` |
| H2 console exposed | Enabled only in dev profile; disable for production with `spring.h2.console.enabled=false` |
| CORS too broad | `CorsConfig` restricts to `localhost:5173` and `localhost:3000` |
| No input validation | Add `@Valid` + `@NotBlank` to `ChatRequest` before production use |

---

## Ideas for Further Development

- [ ] **SSE streaming** — stream LLM tokens and tool events to the frontend in real time  
- [ ] **Real BSS/OSS integration** — replace `TelecomDataStore` with actual API calls to CRM and network management systems  
- [ ] **User authentication** — Spring Security + JWT so each agent has an operator identity  
- [ ] **SLA escalation rules** — auto-create tickets after N recurring cases within M days  
- [ ] **Multiple conversation tabs** — sidebar showing open cases per customer  
- [ ] **RAG** — embed operator runbooks and let the agent search them for resolution steps  
- [ ] **Persistent DB** — swap H2 for PostgreSQL for production-grade case history  
- [ ] **Metrics dashboard** — Grafana/Actuator showing MTTR, issue distribution, escalation rates  

---

<div align="center">

Built for **PIA Group AI Training** · Java 21 · Spring Boot 3 · React 18 · GPT-4o-mini

</div>


Developed as part of the PIA Group AI Training assignment.
A domain-specific **AI agent** that diagnoses and resolves telecommunications issues
through an automated, multi-step diagnostic pipeline — not a generic chatbot and not
an n8n-style pre-drawn workflow.

When a customer says _"My mobile data is very slow"_, the agent runs:

| Step | Tool | What it does |
|------|------|--------------|
| 1 | `intent_analysis` | Classifies issue type (SLOW_DATA) and severity |
| 2 | `customer_verification` | Looks up account status, region, and active plan |
| 3 | `signal_check` | Reads signal strength and quality for the customer's region |
| 4 | `base_station_load` | Checks cell tower load % and congestion status |
| 5 | `active_package_check` | Shows data usage, remaining quota, and plan details |
| 6 | `suggest_resolution` | Synthesises findings into a root-cause + action plan |

## Architecture

```
React (Vite)  <── REST/JSON ──>  Spring Boot  <── function calling ──>  OpenAI API
   :5173                              :8080                                  │
                                        │                                    │
                                   H2 (in-memory)                       gpt-4o-mini
                                  case history                  (tool/function calling)
                                        │
                                 TelecomDataStore
                          (mock CRM / OSS / BSS data)
```

**Agent loop (AgentService.java):**
1. Customer message + conversation history → sent to OpenAI with all tool definitions
2. Model calls tools sequentially per the diagnostic workflow
3. Each tool result is fed back to the model
4. After all diagnostics: model calls `suggest_resolution` and presents a structured report
5. Maximum 5 iterations prevents runaway loops

## Available Tools

| Tool | Name | Purpose |
|------|------|---------|
| `IntentAnalysisTool` | `intent_analysis` | Classifies issue type and severity from raw text |
| `CustomerVerificationTool` | `customer_verification` | Looks up CRM account by phone number |
| `SignalCheckTool` | `signal_check` | Returns signal dBm, quality, and technology for the customer's region |
| `BaseStationLoadTool` | `base_station_load` | Returns tower load %, status, and connected device count |
| `ActivePackageCheckTool` | `active_package_check` | Returns plan name, data quota used/remaining, voice minutes |
| `ResolutionTool` | `suggest_resolution` | Synthesises all findings into a root-cause report and action steps |

To add a new tool, implement `Tool` in the `tool/` package, annotate with `@Component`,
and Spring + `ToolRegistry` pick it up automatically.

## Mock Data (TelecomDataStore)

Test phone numbers you can use right now:

| Phone | Customer | Account | Plan | Region |
|-------|----------|---------|------|--------|
| `05321234567` | Alice Johnson | ACTIVE | 5G Pro Unlimited | Istanbul European |
| `05329876543` | Bob Smith | ACTIVE | 4G Basic 10 GB | Ankara Central |
| `05321111111` | Carol Davis | **SUSPENDED** | 4G Standard 30 GB | Izmir Coastal |
| `05325551234` | David Wilson | ACTIVE | 5G Pro Unlimited | Istanbul Asian ⚠️ high load |
| `05330001122` | Emma Brown | ACTIVE | 4G Standard 30 GB | Bursa Central |

## LLM Provider: OpenAI or Ollama

Controlled by `llm.provider` in `backend/src/main/resources/application.properties`.

**OpenAI** (default):
```properties
llm.provider=openai
openai.model=gpt-4o-mini
```

**Ollama** (local, free):
```properties
llm.provider=ollama
ollama.model=llama3.1
```
```bash
ollama pull llama3.1 && ollama serve
```

## Setup and Running

### Backend (Spring Boot — Java 21+)
```bash
cd backend
export OPENAI_API_KEY=sk-...   # only for llm.provider=openai
mvn spring-boot:run
```
Backend: `http://localhost:8080` · H2 console: `http://localhost:8080/h2-console`

### Frontend (React + Vite — Node 18+)
```bash
cd frontend
npm install && npm run dev
```
Frontend: `http://localhost:5173`

## Example Interaction

> **User**: My mobile data is very slow. My number is 05325551234.
>
> **TelecomOps AI** (after running all 6 diagnostic tools):
> - **Issue**: SLOW_DATA — Medium severity
> - **Customer**: David Wilson — ACTIVE — 5G Pro Unlimited
> - **Signal**: -88 dBm, FAIR quality, 5G
> - **Base Station IST-ASI-3318**: 89% load — HIGH_LOAD, 3,180 devices connected
> - **Package**: Unlimited data (45.2 GB used this cycle)
> - **Root Cause**: Base station congestion in Istanbul Asian region
> - **Resolution**: Connect to Wi-Fi as a workaround; NOC has been alerted; 2–4 hour ETA

## Notes for the Assignment Presentation

- **Domain specificity**: Generic agents are everywhere; a telecom operations agent with
  realistic diagnostic tools, mock CRM/OSS data, and a structured diagnostic protocol is
  genuinely differentiated.
- **Extensibility**: The Strategy/Registry pattern means adding a new diagnostic tool
  (e.g., `RoamingCheckTool`, `DeviceCompatibilityTool`) is a single annotated class.
- **Further development ideas**: Replace `TelecomDataStore` with real API calls to BSS/OSS;
  add SSE streaming for real-time tool-call visibility; add SLA escalation logic;
  integrate with ticketing systems.


Developed as part of the PIA Group AI Training assignment — this is **not** an n8n-style
visual/static workflow tool, but a code-based, real **AI agent**.

The difference: in n8n you draw the flow upfront (step A → B → C).
Here, the agent reads the user's message and **decides on its own which tool to call and
when** (via OpenAI function calling), executes the tool, re-evaluates the result, and calls
another tool if needed. This decision process is visualised step by step in the frontend's
**"Agent Trace"** panel.

## Architecture

```
React (Vite)  <-- REST/JSON -->  Spring Boot  <-- function calling -->  OpenAI API
   :5173                              :8080                                |
                                        |                                  |
                                   H2 (in-memory)                    gpt-4o-mini
                                conversation history
```

**Agent loop (AgentService.java):**
1. User message + history → sent to OpenAI together with available tool definitions
2. If the model wants to call a tool → the corresponding Java class runs, result returned to the model
3. Once the model no longer needs a tool, it produces the final answer
4. A maximum of 5 iterations prevents infinite loops

**Available tools:**
| Tool | What it does |
|---|---|
| `calculator` | Evaluates mathematical expressions |
| `get_current_datetime` | Returns the current date and time |
| `get_weather` | Current weather by city via Open-Meteo API (no key required) |
| `manage_notes` | Saves and lists notes during the conversation |

To add a new tool, simply add a new `@Component` class to the `tool/` package that
implements the `Tool` interface — `ToolRegistry` picks it up automatically.

## LLM Provider: OpenAI or Ollama

The project supports both providers; which one is used is controlled by the `llm.provider`
property in `backend/src/main/resources/application.properties`. Because `AgentService`
works through a common `LlmClient` interface, **no changes to the agent loop are needed**
when switching providers — only this one-line setting changes.

**To run with OpenAI** (default):
```properties
llm.provider=openai
openai.model=gpt-4o-mini
```
You need to provide the API key as an environment variable (see below).

**To run with Ollama** (local, free, no API key required):
```properties
llm.provider=ollama
ollama.model=llama3.1
```
First install Ollama and pull the model:
```bash
# Install Ollama from https://ollama.com, then:
ollama pull llama3.1
ollama serve   # usually starts automatically in the background at http://localhost:11434
```
Note: Ollama's tool-calling support depends on the model and is less mature than OpenAI's;
choose a model that supports tool calling, e.g. `llama3.1` or `qwen2.5`.
Normalisation code and explanatory comments can be found in `OllamaService.java`.

## Setup and Running

### 1) Backend (Spring Boot)

Requirements: Java 21+, Maven

```bash
cd backend
export OPENAI_API_KEY=sk-...   # only required when llm.provider=openai
mvn spring-boot:run
```

Backend starts at `http://localhost:8080`.
H2 console (optional, to inspect data): `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:agentdb`, user: `sa`, password: empty)

### 2) Frontend (React + Vite)

Requirements: Node.js 18+

```bash
cd frontend
npm install
npm run dev
```

Frontend opens at `http://localhost:5173` and connects to the backend.

## Notes for the assignment presentation

- "Not like n8n" requirement: there is no fixed visual flow here; the "Agent Trace" panel
  that shows the agent's decisions was added specifically to demonstrate this difference.
- The code is open for extension (Strategy/Registry pattern) — if extensibility is
  emphasised in class, you can use this as an example.
- Ideas for further development: real-time streaming via SSE/WebSocket, user authentication,
  multiple conversation tabs, RAG (question-answering over your own documents).

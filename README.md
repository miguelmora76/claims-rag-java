# claims-rag-java

Sample Spring Boot project showing a retrieval-augmented question-answering service with an eval suite and
token/cost tracking, calling Claude through the official Anthropic Java SDK.

This is a learning/portfolio project. It is not a production system, has no real users, and uses only synthetic data.

## What it demonstrates

| Area | Where |
|---|---|
| Calling an LLM API from Java (official SDK, typed errors, usage metadata) | `llm/ClaudeLlmClient` |
| Provider interface with an offline fake for tests and CI | `llm/LlmClient`, `llm/FakeLlmClient` |
| RAG: chunking, embeddings, vector search, grounded prompt with citations | `rag/*`, `service/AskService` |
| Prompt-injection posture: untrusted context, answer-only-from-context, abstain token | `AskService.SYSTEM` |
| Token and cost tracking per request and per model | `cost/CostTracker`, `GET /api/usage` |
| Eval suite: golden set, retrieval/citation/abstention metrics, optional LLM judge | `eval/*`, `src/main/resources/eval/golden.json` |
| Eval as a CI gate | `EvalSuiteTest`, `.github/workflows/ci.yml` |
| Pattern-based redaction before text leaves the service | `privacy/PhiRedactor` |

## The domain

Billing staff asking questions about payer claim policy (timely filing, denial codes, appeals, prior auth).
The seven documents in `src/main/resources/corpus/` are invented for this project. They are not real payer rules.

## Run it

Requires Java 21+ and Maven.

```bash
mvn verify                      # tests + offline eval gate
mvn spring-boot:run             # fake provider, http://localhost:8080
curl -s localhost:8080/api/ask -H 'content-type: application/json' \
  -d '{"question":"How many days do I have to file a first-level appeal?"}'
curl -s localhost:8080/api/usage
```

With real Claude:

```bash
export ANTHROPIC_API_KEY=...
# user-scoped keys (sk-ant-usr-...) also need: export ANTHROPIC_WORKSPACE_ID=wrkspc_...
mvn spring-boot:run -Dspring-boot.run.arguments="--assistant.provider=claude"
```

Model, effort, `top-k` and the price table live in `src/main/resources/application.yml`. The default model is
`claude-opus-5-5` with `effort: low`. Prices in the yml are copied from published rates at the time of writing; verify them.

## Evals

```bash
# offline pipeline eval (what CI runs)
mvn spring-boot:run -Dspring-boot.run.arguments="--assistant.eval=true --spring.main.web-application-type=none"

# live eval against Claude, adds an LLM-judge groundedness score (spends real money)
mvn spring-boot:run -Dspring-boot.run.arguments="--assistant.eval=true --assistant.provider=claude --spring.main.web-application-type=none"
```

The golden set has 15 cases: 12 answerable, 3 not (out of scope, plus a prompt-injection attempt). Metrics:
retrieval recall, required-fact coverage, citation validity, citation correctness, abstention accuracy, pass rate,
and (live only) judge score. The report is written to `eval-report/report.md`.

### Live run (Claude, one run, 2026-10-05)

First live run against `claude-opus-5-5` (effort low) with `claude-haiku-4-5` as judge, on the retrieval code as of that run:

| metric | value |
|---|---|
| retrieval recall (expected doc in top-k) | 100% |
| fact coverage | 85% |
| citation validity | 100% |
| citation correctness | 85% |
| abstain accuracy | 100% |
| case pass rate | 88% |
| judge groundedness (1-5) | 4.27 |
| cost for the whole run | about $0.05 |

Two cases failed. Both answered `NOT_IN_CONTEXT` although the right document was retrieved. That exposed a weak
metric: "expected doc in top-k" counts a hit even when the chunk holding the answer was not retrieved. I added
**context recall** (does the retrieved text contain the required facts), then fixed the causes it pointed at:
suffix stemming ("decided" vs "decides") and an exact-match boost for denial-code tokens like `CO-27`. Offline
retrieval and context recall are both 100% on the golden set after that. I have not re-run the live eval since those
changes, so the table above does not reflect them. It is a single run, a model's output varies between runs, and the
golden set is small, so treat the numbers as indicative only.

### What the offline eval does and does not tell you

The offline run uses a fake, extractive "model". It checks the plumbing: retrieval, prompt assembly, citation
parsing, abstention, cost accounting. The CI gate only asserts those. Fact coverage and pass rate are reported
but not gated offline, because the fake often picks the wrong sentence from correct context.
Answer quality needs the live run.

Retrieval changes along the way, each found by the eval: plain hashed term counts missed the CO-27 question;
IDF weighting fixed that; stemming fixed a "decided"/"decides" miss but exposed another; the code-token boost fixed that one.

## Known limits

- Embeddings are lexical (hashed unigrams and bigrams with IDF, crude stemming, code-token boost), not semantic. The `Embedder` interface is where a real embedding model would go.
- The vector store is brute force and in memory. Fine for hundreds of chunks only.
- The golden set is small and written by the same person who wrote the corpus, so scores are optimistic.
- `PhiRedactor` is regex-based and misses names and free text. It shows where redaction belongs; it is not HIPAA de-identification.
- No auth, rate limiting, streaming, persistence or prompt caching (the system prompt is below the minimum cacheable size).
- Server-side refusal fallbacks are not configured.

# 0001 – Board-game session timing estimator

## Status

Accepted

## Context

This project is a proof-of-concept (POC) mobile app plus backend.  
The goal is to help people decide whether they have enough time to **teach and play a specific board game with a specific group**.

Key characteristics:

- Input:
  - A scanned barcode from a supported board game box
  - A group profile (player count, familiarity, turn pace, analysis style, etc.)
- Output:
  - A **session timing estimate**, expressed in minutes:
    - `teachMinutes`
    - `playMinutes`
    - `totalMinutes = teachMinutes + playMinutes`
    - `confidence`
    - `playerCountFit`
    - `explanation`
    - `riskNotes`
- First version is deliberately narrow in scope:
  - Small seeded catalog of games and barcodes
  - No universal board-game database
  - No pricing, purchasing, or recommendation logic

There is a high risk that generic AI tools misinterpret the word **“estimate”** as a **cost/price** estimate rather than a **time** estimate.  
The architecture and documentation need to keep both humans and coding assistants anchored on the correct meaning.

The system must also:

- Be simple to deploy as a stateless container (e.g., on Fargate/App Runner).
- Be easy to test locally without external services (no real barcode services, catalogs, or LLMs required for unit tests).
- Demonstrate clean modularity (ports-and-adapters / hexagonal style) so components can be swapped out later.

## Decision

1. **Primary domain is session timing, not pricing**
   - In this codebase, “estimate” always means **time estimate in minutes** for a board-game session.
   - The core domain concepts are:
     - `teachMinutes`, `playMinutes`, `totalMinutes`
     - `confidence`
     - `playerCountFit`
     - `groupProfile`
   - The system will not model price, cost, or purchase decisions.

2. **Scope: “scan → group profile → timing estimate → feedback”**
   - The POC will focus on a single end-to-end flow:
     1. Scan barcode.
     2. Resolve to a seeded game record.
     3. Collect group profile.
     4. Produce a session timing estimate.
   - Anything beyond this flow (e.g., recommendations, collections, social features) is explicitly out of scope for ADR 0001.

3. **Seeded catalog + ports, not direct external dependencies**
   - Board-game metadata and barcode mappings will come from a **seeded local catalog** in the first version.
   - All catalog and barcode lookups sit behind ports (e.g., `GameCatalogPort`, `BarcodeResolutionPort`).
   - Later, external services can be integrated by adding new adapter implementations without changing the domain.

4. **Hybrid timing estimation: deterministic heuristics + optional LLM**
   - The first implementation of timing estimation will be a **deterministic heuristic engine** based on:
     - official play time
     - supported player counts
     - complexity
     - player-count suitability
     - group profile (familiarity, turn pace, analysis style, etc.)
   - The timing estimation use case will call a port (e.g., `TimingEstimateModelPort`), which may:
     - use only the heuristics, or
     - combine heuristics with an LLM-backed adapter for refinement, explanation, and risk notes.
   - If an LLM is used and its output is invalid or incomplete, the system will **fall back** to the deterministic result.

5. **Ports-and-adapters / hexagonal architecture**
   - The backend will be structured with a clear distinction between:
     - domain model and services
     - application/use-case layer
     - inbound adapters (REST controllers)
     - outbound adapters (catalog, model, persistence, cache, analytics)
   - All external concerns (LLM, persistence, cache, events) sit behind explicit ports/interfaces.

6. **Stateless API; state in adapters**
   - The Quarkus API layer will be stateless.
   - State (catalog data, estimates, feedback) will be managed in repositories or external stores behind ports.
   - Local/dev mode will use in-memory or file-backed implementations.

7. **Testability and local-first development**
   - Unit tests must be able to run with:
     - fake or in-memory catalog
     - fake model adapter
     - in-memory persistence
     - in-memory cache
     - in-memory event sink
   - No unit test may require network access, real LLM calls, or real barcode services.
   - Integration tests may exercise the REST endpoints, but still use local data and fake adapters.

8. **Documentation and agent context**
   - The repo will contain:
     - `CLAUDE.md` with constraints and vocabulary for coding assistants.
     - `docs/domain-context.md` with product/domain explanation, flow, and API semantics.
   - These documents will explicitly state that:
     - “estimate” = time estimate in minutes.
     - Pricing/purchasing/quote concepts are non-goals.

## Alternatives considered

1. **Treat “estimate” as generic and infer semantics per feature**
   - Let different features interpret “estimate” differently (e.g., timing vs. cost).
   - Rely on naming and context in each module to disambiguate.
   - Rejected because:
     - Increases cognitive load for humans.
     - Strongly increases confusion risk for AI coding tools.
     - Makes API semantics harder to reason about.

2. **Make it a general board-game assistant**
   - Broaden scope to include recommendations, pricing, and strategy tips.
   - Allow timing estimation to be one of many features.
   - Rejected because:
     - Dilutes the POC goal and makes architecture harder to review.
     - Encourages entangling unrelated concerns (commerce, preference modeling, strategy).
     - Makes it more likely that “estimate” is interpreted as cost/quote.

3. **Rely entirely on an LLM for timing estimation**
   - Skip deterministic heuristics; send metadata + group profile to an LLM and accept the output.
   - Rejected because:
     - Harder to test deterministically.
     - Failure modes (invalid JSON, hallucinated facts) are more disruptive.
     - The POC needs to demonstrate domain logic, not just prompt engineering.

4. **Direct integration with a live board-game database from day 1**
   - Resolve barcodes and metadata via external APIs from the start.
   - Rejected because:
     - Adds operational complexity and external dependencies to the POC.
     - Makes local, deterministic testing harder.
     - Catalog completeness is not required to prove the architecture.

## Consequences

### Positive

- **Clarity of purpose**  
  Everyone (and every tool) can treat the system as a **session duration** estimator, not a pricing or recommendation engine.

- **Simpler reasoning and review**  
  The POC is narrowly scoped, which makes the architecture and code easier to review and evolve.

- **Strong testability**  
  Core behavior (heuristics, orchestration, parsing) can be fully tested with fakes and in-memory implementations, without network or cloud dependencies.

- **Future extensibility**  
  Ports-and-adapters make it straightforward to:
  - swap in a real catalog
  - add a real model provider
  - move from in-memory to durable stores

- **Better behavior with coding assistants**  
  With `CLAUDE.md`, `docs/domain-context.md`, and this ADR, coding agents have clear guardrails about what “estimate” means and what the system is allowed to do.

### Negative

- **Deliberately limited feature set**  
  The POC does not (yet) answer natural follow-up questions like:
  - “Which game should we play?”
  - “Is this game worth the price?”
  - “What should we buy next?”

- **Seeded catalog only**  
  The demo depends on a small set of known barcodes and games. Users must pick from this narrow set for reliable behavior.

- **Additional documentation overhead**  
  Keeping `CLAUDE.md`, domain context, and ADRs up to date adds some maintenance cost, but this is acceptable given the clarity benefits.

### Follow-up actions

- Create and maintain:
  - `CLAUDE.md` (agent guardrails).
  - `docs/domain-context.md` (product/domain overview).
- Implement the initial set of ports and in-memory adapters.
- Implement deterministic timing heuristics and tests.
- Add an optional LLM adapter behind `TimingEstimateModelPort` once the deterministic path is stable.
- Revisit this ADR if the product direction expands beyond session timing into recommendations or commerce.

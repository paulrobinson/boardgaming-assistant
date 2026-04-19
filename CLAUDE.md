# CLAUDE.md

## Project overview
This repository contains a board-game session timing estimator demo.
The app scans a supported board game barcode, collects a group profile, and returns:
- teachMinutes
- playMinutes
- totalMinutes
- confidence
- explanation
- playerCountFit

## Domain rules
- “Estimate” always means a time estimate in minutes.
- Never interpret estimate as price, cost, budget, or quote.
- The app does not recommend games to buy.
- The app does not suggest optimal moves or strategy.
- totalMinutes must always equal teachMinutes + playMinutes.

## Architecture constraints
- Use ports-and-adapters / hexagonal architecture.
- Keep domain logic framework-independent where possible.
- All external integrations must sit behind ports.
- Prefer in-memory or fake adapters for local development and unit tests.

## Testing expectations
- Unit tests must not require network access or external services.
- Use fake adapters for catalog, model, cache, analytics, and persistence.
- Keep deterministic tests for prompt building, parsing, fallback logic, and cache behavior.

## Read first
- README.md
- docs/domain-context.md
- docs/adr/0001-session-timing-estimator.md

## Git Configuration

Always use the following git identity when committing:

- **Name:** Paul Robinson
- **Email:** paul.robinson@redhat.com

## Workflow Rules

- Always start a new change on a new branch which is branched from main
- Always check for upstream changes before branching
- Always propose new changes with a Pull Request
- Never push directly to main
- Add tests for all new features/changes
- When implementing a GitHub Issue, read the full issue and prioritise what's mentioned in the comments by Paul Robinson.
- Run the tests before you open the Pull Request. Check back on the pull request to see if it failed. If it failed, fix it.

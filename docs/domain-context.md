# Domain context: Board-game session timing estimator

## Overview

This project is a demo application for estimating **how long a board game session will take** for a particular group. The goal is to help users decide whether they have enough time to teach and play a game before starting.

The app is intentionally focused on **session duration** rather than purchasing, recommendations, pricing, strategy, or catalog completeness.

## Core user story

A user is preparing for game night. They scan the barcode on a supported board game box, enter details about the group, and receive a structured estimate of how long the session will likely take.

The estimate includes:

- `teachMinutes`: time to explain the rules and onboard the group
- `playMinutes`: time to play the session
- `totalMinutes`: total estimated session time
- `confidence`: confidence in the estimate
- `playerCountFit`: how well the game tends to work at different player counts
- `explanation`: short plain-English rationale
- `riskNotes`: caveats that may cause the session to run long

## Non-goals

This app does **not** do any of the following:

- estimate cost, price, or budget
- recommend which game to buy
- recommend which game a group will enjoy most
- suggest optimal moves or strategy
- predict resale value or price-performance
- provide universal support for all published board games in the first version

## User flow

1. The user opens the app.
2. The user scans the barcode on a board game box.
3. The app resolves the barcode to a game from a seeded local catalog.
4. The user enters a group profile.
5. The backend combines game metadata and group profile data.
6. The system produces a session timing estimate.
7. The estimate is displayed in the UI.

## Group profile

The group profile should capture the main factors that affect session length.

Suggested fields:

- `playerCount`: integer
- `groupFamiliarity`: `new`, `mixed`, or `experienced`
- `turnPace`: `fast`, `average`, or `slow`
- `analysisStyle`: `low`, `moderate`, or `high`
- `notes`: optional free text

These fields let the system adjust from official play time toward a group-specific timing estimate.

## Game metadata

For the POC, use a seeded local catalog. Each game entry should contain enough data to support timing estimation and player-count fit.

Suggested fields:

- `gameId`
- `barcode`
- `name`
- `minPlayers`
- `maxPlayers`
- `officialPlayTimeMinutes`
- `officialMinAge`
- `complexityWeight`
- `playerCountSummary`
- `notes`

The POC does not need a full global catalog. It only needs enough seeded games to demonstrate the complete workflow.

## Meaning of “estimate”

In this repository, the word **estimate** always means **time estimate in minutes**.

This rule is important because coding assistants may otherwise interpret “estimate” as financial estimation. That interpretation is always incorrect in this project.

### Correct interpretations

- teach time estimate
- play time estimate
- total session time estimate
- timing estimate for a specific group

### Incorrect interpretations

- price estimate
- cost estimate
- budget estimate
- quote generation
- financial forecasting

## Example scenario

Suppose a game has:

- official play time: 60 minutes
- supported players: 2 to 4
- complexity: medium
- best at 4 players

A group enters:

- 4 players
- mixed familiarity
- slow turn pace
- moderate analysis paralysis

A valid estimate might be:

- `teachMinutes = 20`
- `playMinutes = 85`
- `totalMinutes = 105`
- `confidence = medium`
- explanation stating that mixed familiarity and slow turns push the session beyond the official baseline

This is a session duration prediction. It is not a recommendation and not a financial estimate.

## Estimation approach

The most robust design for the POC is a hybrid approach.

### Step 1: grounded metadata

Start from seeded game data such as:

- official play time
- supported player counts
- complexity
- player-count fit summary

### Step 2: deterministic heuristics

Apply explicit timing adjustments, for example:

- increase teach time if the group is new or mixed
- increase play time if turn pace is slow
- increase risk notes when analysis paralysis is high
- lower confidence near edge player counts

### Step 3: optional LLM refinement

Use an LLM behind a port to:

- refine the explanation
- slightly adjust the estimate
- produce well-structured output
- add human-readable risk notes
- Adjust based on the LLM's knowedge of the actual play time vs the advertised play time. E.g. basesd on community feedback

The LLM should not invent core game facts. It should work from the metadata and group profile provided.

## API semantics

### Scan resolution

`POST /api/v1/scan/resolve`

Input:

```json
{
  "barcode": "1234567890123"
}
```

Output:

```json
{
  "gameId": "demo-game-1",
  "name": "Example Game",
  "officialPlayTimeMinutes": 60,
  "minPlayers": 2,
  "maxPlayers": 4,
  "supported": true
}
```

### Timing estimate

`POST /api/v1/estimates`

Input:

```json
{
  "gameId": "demo-game-1",
  "groupProfile": {
    "playerCount": 4,
    "groupFamiliarity": "mixed",
    "turnPace": "slow",
    "analysisStyle": "moderate",
    "childrenIncluded": false,
    "notes": "Two players are new"
  }
}
```

Output:

```json
{
  "estimateId": "est_123",
  "teachMinutes": 25,
  "playMinutes": 105,
  "totalMinutes": 130,
  "confidence": "medium",
  "playerCountFit": [
    {"playerCount": 2, "fit": "avoid"},
    {"playerCount": 3, "fit": "good"},
    {"playerCount": 4, "fit": "best"}
  ],
  "explanation": "This game scales well at four players, but mixed familiarity and slower turns usually add overhead.",
  "riskNotes": [
    "Rules reminders may increase downtime",
    "First play may run longer than the estimate"
  ]
}
```



## Architecture implications

The domain is intentionally simple enough to review but rich enough to exercise the architecture.

Important implications:

- Barcode resolution should sit behind a port.
- Catalog access should sit behind a port.
- Timing estimation should sit behind a port.
- Heuristic logic should be pure and easy to unit test.
- Persistence and analytics should be replaceable with in-memory adapters.
- The API layer should remain stateless for container deployment.

## Testing expectations

The project should be easy to test without external dependencies.

### Unit tests should cover

- timing heuristics
- validation rules
- prompt building
- model response parsing
- fallback behavior
- cache behavior

### Unit tests should not require

- network access
- real barcode lookups
- real LLM services
- cloud resources
- real databases

### Test doubles to provide

- fake catalog adapter
- fake barcode adapter
- fake timing-estimate model adapter
- in-memory repositories
- in-memory cache
- in-memory event sink
- deterministic clock and ID generator

## Guidance for coding assistants

When working in this repository, coding assistants should assume:

- estimate = time estimate in minutes
- totalMinutes must equal teachMinutes + playMinutes
- demo reliability is more important than catalog completeness
- seeded local data is preferred for the first version
- testability is a first-class concern
- architecture clarity matters more than feature breadth in the POC

## Suggested companion docs

This file works best alongside:

- `CLAUDE.md` for concise agent-facing guardrails
- `README.md` for setup and project overview
- `docs/adr/0001-session-timing-estimator.md` for the main architectural decision record

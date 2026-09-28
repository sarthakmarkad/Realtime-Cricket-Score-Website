# CricLive — Cricket Score Management System

A scalable, real-time cricket score management platform built with **Spring Boot** (REST APIs,
live match simulation, SSE streaming) and **React** (responsive dashboard). The backend simulates
T20 matches ball by ball — scores, wickets, extras, strike rotation, bowling changes, innings
breaks and results — and every delivery is pushed to connected clients the instant it is bowled.

## Tech Stack

| Layer      | Technology                                                            |
| ---------- | --------------------------------------------------------------------- |
| Backend    | Java 17, Spring Boot 3.3 (Web, Data JPA), H2 in-memory DB, Maven      |
| Real-time  | Server-Sent Events (`/api/stream`) with heartbeats and auto-reconnect |
| Frontend   | React 18, React Router 6, Vite 5, plain CSS (fully responsive)        |

## Features

- **Live match simulation** — a scheduled tick plays one delivery per tick for every live match:
  weighted run outcomes, phase-aware aggression in the last 5 overs, wickets with dismissal
  types, wides/no-balls, strike rotation, over changes with new bowlers (max 4 overs each),
  innings breaks, chases with target/required-rate logic and final results.
- **REST APIs** — matches list, match detail with live striker/bowler info, full scorecards,
  ball-by-ball commentary, match summaries with top performers, player leaderboards,
  player profiles with match-by-match stats, and team squads.
- **Real-time dashboard** — SSE-powered live cards (score, this over, last event, CRR/RRR,
  chase line), connection status pill, and automatic reconnection.
- **Player statistics** — tournament leaderboards (runs, strike rate, wickets, economy, best
  figures) and per-player profiles updated as matches progress.
- **Match management** — seed data on first boot (one completed match, one live mid-innings,
  one upcoming) plus `POST /api/matches/{id}/start` to kick off an upcoming match from the UI.
- **Restart-safe** — simulation state (strike, bowler, next batter) is rebuilt from the
  database if the app restarts mid-match.

## Running the project

### 1. Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

First run seeds 6 teams (full 11-player squads) and 3 matches. H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:cricketdb`, user `sa`).

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies `/api` to `localhost:8080`.

## API Reference

| Method | Endpoint                        | Description                                        |
| ------ | ------------------------------- | -------------------------------------------------- |
| GET    | `/api/matches`                  | All matches (live first) with score summaries      |
| GET    | `/api/matches/{id}`             | Match detail + innings + live striker/bowler block |
| GET    | `/api/matches/{id}/scorecard`   | Full batting & bowling cards for every innings     |
| GET    | `/api/matches/{id}/commentary`  | Ball-by-ball commentary, newest first (`?limit=`)  |
| GET    | `/api/matches/{id}/summary`     | Result + innings totals + top scorer/bowler        |
| POST   | `/api/matches/{id}/start`       | Start an upcoming match (simulation takes over)    |
| GET    | `/api/players/leaders`          | Top run scorers and top wicket takers              |
| GET    | `/api/players/{id}`             | Player profile with career aggregates              |
| GET    | `/api/teams`                    | Team squads                                        |
| GET    | `/api/stream`                   | SSE stream: `update` events per ball/status change |

## How the real-time layer works

1. `MatchSimulationService` runs a `@Scheduled` tick (every 3.5 s) and plays one delivery for
   each live/innings-break match, persisting `Innings`, `PlayerMatchStats` and `BallEvent` rows.
2. After each delivery it builds a compact `MatchSummary` DTO and publishes it through
   `ScoreBroadcastService`, which fans the JSON out to every `SseEmitter` registered at
   `GET /api/stream`. A heartbeat comment every 15 s keeps intermediaries from idling out.
3. The React app reads the stream with `EventSource`: the dashboard swaps in the updated match
   summary instantly, and the match detail page refetches scorecard/commentary for its match.
   If the connection drops, `EventSource` reconnects and the UI shows a "Reconnecting…" pill.

## Project structure

```
backend/
  src/main/java/com/cricket/scores/
    config/       DataSeeder (first-boot data), WebConfig (CORS)
    dto/          Dtos (API records), DtoMapper (entity -> DTO)
    exception/    ResourceNotFoundException
    model/        JPA entities: Match, Team, Player, Innings, PlayerMatchStats, BallEvent
    repository/   Spring Data repositories
    service/      CricketService (read-side queries & aggregation)
    simulation/   MatchSimulationService (engine), LiveStateRegistry
    sse/          ScoreBroadcastService (SSE hub)
    web/          Match/Player/Team/Stream controllers, error handler
frontend/
  src/
    components/   MatchCard, InningsScorecard, CommentaryFeed, Navbar, ...
    hooks/        useLiveScores, useMatchEvents (SSE)
    pages/        Dashboard, MatchDetail, Players, PlayerProfile
    api.js, format.js, styles.css
```

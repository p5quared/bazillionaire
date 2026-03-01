# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./mvnw compile              # Compile check
./mvnw test                 # Run all tests
./mvnw quarkus:dev          # Dev mode with live reload
just build                  # Package JAR
just deploy                 # Build and deploy to Fly.io
just verify                 # Full verification
just get-coverage           # Run coverage and open report
```

Run a single test class:
```bash
./mvnw test -Dtest=GameJoinTest
```

## Architecture

Quarkus 3.32.1 / Java 21 multiplayer stock trading game. Hexagonal architecture under `net.peterv.bazillionaire.game`.

### Game Domain (`game/domain/`)

Core state machine in `Game.java`: status flows PENDING → READY → FINISHED. All mutations (`join`, `placeOrder`, `tick`, `start`) happen on Game and produce `GameMessage` objects (event + audience). Messages are drained via `Game.drainMessages()`.

**Sealed types throughout:** `Order` (Buy/Sell), `JoinResult` (Joined/AllReady/GameInProgress/AlreadyReady/InvalidJoin), `OrderResult` (Filled/Rejected/InvalidOrder), `GameEvent` (8 variants), `Audience` (Everyone/Only).

**Value objects are records** with `.value()` accessor: `PlayerId`, `Symbol`, `GameId`, `Money` (cents).

### Ports & Services (`game/port/`, `game/service/`)

Use case interfaces in `port/in/` (JoinGameUseCase, PlaceOrderUseCase, etc.). Services are `@ApplicationScoped` CDI beans returning `UseCaseResult<T>` (result + messages list).

### Adapters (`game/adapter/`)

- `StockGameWebSocketAdapter` — `@WebSocket(path = "/game/{gameId}")`, handles JOIN/BUY/SELL messages
- `GameSessionRegistry` — tracks connections, game→connection mapping, player associations
- `GameTickScheduler` — `@Scheduled` drives tick loop
- `InMemoryGameRepository` — `ConcurrentHashMap` with `synchronized(game)` for thread safety

### Web Layer (`web/`)

Renarde controllers (`LobbyController`, `LoginController`, `UserController`, `GameController`) with Qute templates. JPA/Panache entities (`Lobby`, `LobbyMember`) with PostgreSQL (Dev Services auto-starts DB).

## Key Patterns

- **Renarde @Transactional + redirect**: For controller methods that write to DB and redirect, use a separate `@ApplicationScoped @Transactional` service for the write. The controller calls the service (which commits), then throws `RedirectException` outside any transaction. Read-only `@Transactional` is fine.
- **WebSocket wire format**: `{ "type": "BUY", "payload": { "ticker": "AAPL", "price": 10000 } }` (price in cents)
- **Java 21 sealed switch**: Cannot combine multiple type patterns with variables using `,` in a switch case — use `default` to capture remaining variants.
- **DB schema**: `drop-and-create` in dev/test profiles via `quarkus.hibernate-orm.database.generation`.

package net.peterv.bazillionaire.game.adapter.in;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import net.peterv.bazillionaire.game.adapter.out.GameEventDispatcher;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.powerup.GameContext;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.port.in.TickCommand;
import net.peterv.bazillionaire.game.port.in.TickProgress;
import net.peterv.bazillionaire.game.port.in.TickUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.out.GameFinishedSnapshot;
import net.peterv.bazillionaire.game.port.out.GameRepository;

@ApplicationScoped
public class GameTickScheduler {

  @Inject TickUseCase tickUseCase;

  @Inject GameSessionRegistry registry;

  @Inject StockGameWebSocketAdapter adapter;

  @Inject GameRepository gameRepository;

  @Inject GameEventDispatcher eventDispatcher;

  private static final int TICKS_PER_SECOND = 4;
  private static final long TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;

  @Scheduled(every = "10s")
  @Blocking
  void tick() {
    for (int i = 0; i < TICKS_PER_SECOND * 10; i++) {
      long elapsed = timeExecution(this::tickAllGames);
      if (i < (TICKS_PER_SECOND * 10) - 1) {
        try {
          sleepRemaining(elapsed);
        } catch (InterruptedException e) {
          Log.warn("GameTickScheduler interrupted during sleep; stopping tick loop");
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  private void tickAllGames() {
    for (String gameId : List.copyOf(registry.activeGameIds())) {
      UseCaseResult<TickProgress> result = tickUseCase.tick(new TickCommand(gameId));
      List<GameMessage> messages = result.messages();
      adapter.dispatchMessages(gameId, messages);
      eventDispatcher.dispatch(new GameId(gameId), messages);
      boolean finished =
          messages.stream().anyMatch(m -> m.event() instanceof GameEvent.GameFinished);
      if (finished) {
        GameContext finalState =
            gameRepository.withGame(new GameId(gameId), game -> game.snapshot());
        var snapshot = new GameFinishedSnapshot(finalState.players(), finalState.currentPrices());
        eventDispatcher.dispatchFinished(new GameId(gameId), snapshot);
        registry.deregisterGame(gameId);
        gameRepository.removeGame(new GameId(gameId));
      }
    }
  }

  private long timeExecution(Runnable task) {
    long start = System.currentTimeMillis();
    task.run();
    return System.currentTimeMillis() - start;
  }

  private void sleepRemaining(long elapsedMs) throws InterruptedException {
    long sleepTime = TICK_INTERVAL_MS - elapsedMs;
    if (sleepTime > 0) Thread.sleep(sleepTime);
  }
}

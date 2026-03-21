package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;
import net.peterv.bazillionaire.game.port.out.GameFinishedSnapshot;

@ApplicationScoped
public class GameResultListener implements GameEventListener {

  @Inject PlayerGameStatsService statsService;

  private final Map<GameId, Map<PlayerId, GameStatAccumulator>> activeGames =
      new ConcurrentHashMap<>();

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      // Future: switch on message.event() to accumulate mid-game stats
      // e.g. case PowerupActivated(var p, _) -> accumulator(gameId, p).powerupsActivated++;
    }
  }

  @Override
  public void onGameFinished(GameId gameId, GameFinishedSnapshot snapshot) {
    PlayerId winner = determineWinner(snapshot);
    for (var entry : snapshot.players().entrySet()) {
      PlayerId playerId = entry.getKey();
      boolean won = playerId.equals(winner);
      int finalCashCents = entry.getValue().cashBalance().cents();
      statsService.recordGame(playerId.value(), gameId.value(), won, finalCashCents);
    }
    activeGames.remove(gameId);
  }

  private GameStatAccumulator accumulator(GameId gameId, PlayerId playerId) {
    return activeGames
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(playerId, k -> new GameStatAccumulator());
  }

  private PlayerId determineWinner(GameFinishedSnapshot snapshot) {
    PlayerId winner = null;
    int highestCash = Integer.MIN_VALUE;
    for (var entry : snapshot.players().entrySet()) {
      int cash = entry.getValue().cashBalance().cents();
      if (cash > highestCash) {
        highestCash = cash;
        winner = entry.getKey();
      }
    }
    return winner;
  }
}

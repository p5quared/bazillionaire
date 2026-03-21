package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;

@ApplicationScoped
public class DividendStatsListener implements GameEventListener {

  @Inject PlayerDividendStatsService dividendStatsService;

  private final ConcurrentHashMap<GameId, ConcurrentHashMap<PlayerId, DividendAccumulator>>
      activeGames = new ConcurrentHashMap<>();

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      switch (message.event()) {
        case GameEvent.DividendPaid paid -> accumulate(gameId, paid);
        case GameEvent.GameFinished finished -> persistAndCleanup(gameId);
        default -> {}
      }
    }
  }

  private void accumulate(GameId gameId, GameEvent.DividendPaid paid) {
    activeGames
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(paid.playerId(), k -> new DividendAccumulator())
        .add(paid.amount().cents());
  }

  private void persistAndCleanup(GameId gameId) {
    var players = activeGames.remove(gameId);
    if (players == null) return;
    for (var entry : players.entrySet()) {
      var acc = entry.getValue();
      dividendStatsService.recordDividends(
          entry.getKey().value(), gameId.value(), acc.count(), acc.totalCents());
    }
  }

  private static class DividendAccumulator {
    private int count;
    private int totalCents;

    void add(int cents) {
      count++;
      totalCents += cents;
    }

    int count() {
      return count;
    }

    int totalCents() {
      return totalCents;
    }
  }
}

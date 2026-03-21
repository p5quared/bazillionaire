package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;

@ApplicationScoped
public class GameResultListener implements GameEventListener {

  @Inject PlayerGameStatsService statsService;

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      switch (message.event()) {
        case GameEvent.GameFinished finished -> recordResults(gameId, finished);
        default -> {}
      }
    }
  }

  private void recordResults(GameId gameId, GameEvent.GameFinished finished) {
    PlayerId winner = determineWinner(finished);
    for (var entry : finished.players().entrySet()) {
      PlayerId playerId = entry.getKey();
      boolean won = playerId.equals(winner);
      int finalCashCents = entry.getValue().cashBalance().cents();
      statsService.recordGame(playerId.value(), gameId.value(), won, finalCashCents);
    }
  }

  private PlayerId determineWinner(GameEvent.GameFinished finished) {
    PlayerId winner = null;
    int highestCash = Integer.MIN_VALUE;
    for (var entry : finished.players().entrySet()) {
      int cash = entry.getValue().cashBalance().cents();
      if (cash > highestCash) {
        highestCash = cash;
        winner = entry.getKey();
      }
    }
    return winner;
  }
}

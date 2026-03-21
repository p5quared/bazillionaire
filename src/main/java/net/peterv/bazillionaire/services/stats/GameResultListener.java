package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;
import net.peterv.bazillionaire.game.port.out.GameFinishedSnapshot;

@ApplicationScoped
public class GameResultListener implements GameEventListener {

  @Inject PlayerGameStatsService statsService;

  @Override
  public void onGameFinished(GameId gameId, GameFinishedSnapshot snapshot) {
    PlayerId winner = determineWinner(snapshot);
    for (PlayerId playerId : snapshot.players().keySet()) {
      boolean won = playerId.equals(winner);
      statsService.recordGame(playerId.value(), won);
    }
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

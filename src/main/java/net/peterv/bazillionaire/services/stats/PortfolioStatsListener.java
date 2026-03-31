package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;

@ApplicationScoped
public class PortfolioStatsListener implements GameEventListener {

  @Inject PlayerPortfolioStatsService portfolioStatsService;

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      switch (message.event()) {
        case GameEvent.GameFinished finished -> recordPortfolios(gameId, finished);
        default -> {}
      }
    }
  }

  private void recordPortfolios(GameId gameId, GameEvent.GameFinished finished) {
    for (var entry : finished.players().entrySet()) {
      var portfolio = entry.getValue();
      long valueCents =
          PortfolioValueCalculator.calculatePortfolioValueCents(portfolio, finished.finalPrices());
      int holdingsCount =
          (int) portfolio.holdings().entrySet().stream().filter(e -> e.getValue() > 0).count();
      portfolioStatsService.recordPortfolio(
          entry.getKey().value(), gameId.value(), valueCents, holdingsCount);
    }
  }
}

package net.peterv.bazillionaire.services.stats;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.out.GameEventListener;

@ApplicationScoped
public class TradeStatsListener implements GameEventListener {

  @Inject PlayerTradeStatsService tradeStatsService;

  private final ConcurrentHashMap<GameId, ConcurrentHashMap<PlayerId, TradeAccumulator>>
      activeGames = new ConcurrentHashMap<>();

  @Override
  public void onGameEvents(GameId gameId, List<GameMessage> messages) {
    for (var message : messages) {
      switch (message.event()) {
        case GameEvent.OrderFilled filled -> accumulateFill(gameId, filled);
        case GameEvent.OrderBlocked blocked -> accumulateBlocked(gameId, blocked);
        case GameEvent.GameFinished finished -> persistAndCleanup(gameId);
        default -> {}
      }
    }
  }

  private void accumulateFill(GameId gameId, GameEvent.OrderFilled filled) {
    activeGames
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(filled.playerId(), k -> new TradeAccumulator())
        .addFill(filled.order(), filled.fillPrice().cents());
  }

  private void accumulateBlocked(GameId gameId, GameEvent.OrderBlocked blocked) {
    activeGames
        .computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(blocked.playerId(), k -> new TradeAccumulator())
        .addBlocked();
  }

  private void persistAndCleanup(GameId gameId) {
    var players = activeGames.remove(gameId);
    if (players == null) return;
    for (var entry : players.entrySet()) {
      var acc = entry.getValue();
      tradeStatsService.recordTrades(
          entry.getKey().value(),
          gameId.value(),
          acc.buys(),
          acc.sells(),
          acc.fillsCents(),
          acc.blockedOrders());
    }
  }

  private static class TradeAccumulator {
    private int buys;
    private int sells;
    private long fillsCents;
    private int blockedOrders;

    void addFill(Order order, int priceCents) {
      switch (order) {
        case Order.Buy b -> buys++;
        case Order.Sell s -> sells++;
      }
      fillsCents += priceCents;
    }

    void addBlocked() {
      blockedOrders++;
    }

    int buys() {
      return buys;
    }

    int sells() {
      return sells;
    }

    long fillsCents() {
      return fillsCents;
    }

    int blockedOrders() {
      return blockedOrders;
    }
  }
}

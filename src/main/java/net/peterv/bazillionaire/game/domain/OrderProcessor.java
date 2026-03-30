package net.peterv.bazillionaire.game.domain;

import java.util.ArrayList;
import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderProcessingResult;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.DarkPoolPowerup;
import net.peterv.bazillionaire.game.domain.powerup.PowerupManager;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

class OrderProcessor {
  private final PowerupManager powerupManager;
  private final LiquidityProvider liquidityProvider;

  OrderProcessor(PowerupManager powerupManager, LiquidityProvider liquidityProvider) {
    this.powerupManager = powerupManager;
    this.liquidityProvider = liquidityProvider;
  }

  OrderProcessingResult process(
      Order order, PlayerId playerId, Portfolio player, Market market, int currentTick) {
    Ticker ticker = market.getTicker(order.symbol());
    if (ticker == null) {
      return OrderProcessingResult.of(
          new OrderResult.InvalidOrder("Unknown symbol: " + order.symbol().value()));
    }
    if (ticker.isDelisted()) {
      return OrderProcessingResult.of(new OrderResult.Rejected("Ticker has been delisted"));
    }

    OrderResult intercepted = powerupManager.checkInterceptors(order, playerId, ticker);
    if (intercepted != null) {
      String reason =
          switch (intercepted) {
            case OrderResult.Rejected r -> r.reason();
            case OrderResult.InvalidOrder i -> i.reason();
            case OrderResult.Filled f -> "Order intercepted";
          };
      return new OrderProcessingResult(
          intercepted,
          List.of(GameMessage.send(new GameEvent.OrderBlocked(playerId, order, reason), playerId)));
    }

    DarkPoolPowerup darkPool = powerupManager.getActiveDarkPool(playerId, order.symbol());

    if (darkPool == null) {
      if (!liquidityProvider.canFill(playerId, order.symbol(), currentTick)) {
        return OrderProcessingResult.of(new OrderResult.Rejected("Volume limit exceeded"));
      }
    }

    Money fillPrice = ticker.currentPrice();
    OrderResult result = player.fill(order, fillPrice);

    return switch (result) {
      case OrderResult.Rejected r -> OrderProcessingResult.of(r);
      case OrderResult.InvalidOrder i -> OrderProcessingResult.of(i);
      case OrderResult.Filled f -> {
        boolean isDarkPool = darkPool != null;
        if (isDarkPool) {
          darkPool.consumeToken();
        } else {
          liquidityProvider.recordFill(playerId, order.symbol(), currentTick);
          market.recordTrade(order.symbol(), currentTick, 1);
        }
        String side = order instanceof Order.Buy ? "BUY" : "SELL";
        List<GameMessage> messages = new ArrayList<>();
        messages.add(
            GameMessage.broadcast(
                new GameEvent.OrderActivity(order.symbol(), fillPrice, side, isDarkPool)));
        messages.add(
            GameMessage.send(
                new GameEvent.OrderFilled(order, playerId, f.fillPrice(), f.costBasis()),
                playerId));
        yield new OrderProcessingResult(f, messages);
      }
    };
  }
}

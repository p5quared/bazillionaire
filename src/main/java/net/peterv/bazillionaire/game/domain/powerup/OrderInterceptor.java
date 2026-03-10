package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

@FunctionalInterface
public interface OrderInterceptor {
  OrderResult intercept(Order order, PlayerId playerId, Ticker ticker);
}

package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;

@FunctionalInterface
public interface OrderInterceptor {
	OrderResult intercept(Order order, Ticker ticker);
}

package net.peterv.bazillionaire.game.domain.ticker.event;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;

public interface OrderMarketImpactPolicy {
	MarketEvent impactOf(Order order, Ticker ticker);
}

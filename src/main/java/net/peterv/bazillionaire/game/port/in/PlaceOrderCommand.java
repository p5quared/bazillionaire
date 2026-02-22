package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public record PlaceOrderCommand(String gameId, String playerId, String symbol, OrderSide side, int price) {
	public enum OrderSide {
		BUY, SELL
	}

	public Order toOrder() {
		return switch (this.side()) {
			case BUY -> new Order.Buy(new Symbol(this.symbol), new Money(this.price()));
			case SELL -> new Order.Sell(new Symbol(this.symbol), new Money(this.price()));
		};
	}

	public GameId toGameId() {
		return new GameId(gameId);
	}

	public PlayerId toPlayerId() {
		return new PlayerId(playerId);
	}
}

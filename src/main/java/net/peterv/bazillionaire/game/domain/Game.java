package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.ticker.event.OrderMarketImpactPolicy;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.domain.order.FillResult;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
	private final OrderMarketImpactPolicy impactPolicy;
	private final Map<PlayerId, Portfolio> players;
	private final Map<Symbol, Ticker> tickers;
	private final List<GameMessage> pendingMessages = new ArrayList<>();

	public Game(OrderMarketImpactPolicy policy, Map<PlayerId, Portfolio> players, Map<Symbol, Ticker> tickers) {
		this.impactPolicy = policy;
		this.players = new HashMap<>(players);
		this.tickers = new HashMap<>(tickers);
	}

	public OrderResult placeOrder(Order order, PlayerId playerId) {
		Portfolio player = players.get(playerId);
		if (player == null) {
			return new OrderResult.InvalidOrder("Unknown player: " + playerId.value());
		}

		Ticker ticker = tickers.get(order.symbol());
		if (ticker == null) {
			return new OrderResult.InvalidOrder("Unknown symbol: " + order.symbol().value());
		}

		if (!ticker.canFill(order)) {
			return new OrderResult.Rejected("Ticker cannot fill order");
		}

		FillResult result = player.fill(order);

		return switch (result) {
			case FillResult.Rejected r -> new OrderResult.Rejected(r.reason());
			case FillResult.Filled ignored -> {
				ticker.applyOrder(order, this.impactPolicy);
				emit(GameMessage.broadcast(
						new GameEvent.OrderFilled(order, playerId)));
				yield new OrderResult.Filled();
			}
		};
	}

	public List<GameMessage> drainMessages() {
		List<GameMessage> messages = List.copyOf(pendingMessages);
		pendingMessages.clear();
		return messages;
	}

	private void emit(GameMessage message) {
		pendingMessages.add(message);
	}
}

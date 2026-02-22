package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
	private final Map<PlayerId, Portfolio> players;
	private final Map<Symbol, Ticker> tickers;
	private final List<GameMessage> pendingMessages = new ArrayList<>();

	public Game(Map<PlayerId, Portfolio> players, Map<Symbol, Ticker> tickers) {
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

		OrderResult result = player.fill(order);

		return switch (result) {
			case OrderResult.Rejected r -> r;
			case OrderResult.InvalidOrder i -> i;
			case OrderResult.Filled f -> {
				emit(GameMessage.broadcast(
						new GameEvent.OrderFilled(order, playerId)));
				yield f;
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

package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

import java.util.List;
import java.util.Map;

public sealed interface GameEvent permits GameEvent.OrderFilled, GameEvent.TickerTicked, GameEvent.GameCreated,
		GameEvent.PlayerJoined, GameEvent.AllPlayersReady, GameEvent.GameState, GameEvent.PlayersState,
		GameEvent.GameFinished {
	record OrderFilled(Order order, PlayerId playerId) implements GameEvent {
	}

	record TickerTicked(Symbol symbol, Money price) implements GameEvent {
	}

	record GameCreated(List<Symbol> symbols) implements GameEvent {
	}

	record PlayerJoined(PlayerId playerId) implements GameEvent {
	}

	record AllPlayersReady() implements GameEvent {
	}

	record PlayerPortfolio(Money cashBalance, Map<Symbol, Integer> holdings) {
	}

	record GameState(List<Symbol> symbols, Map<Symbol, Money> prices, Map<PlayerId, PlayerPortfolio> players)
			implements GameEvent {
	}

	record PlayersState(Map<PlayerId, PlayerPortfolio> players) implements GameEvent {
	}

	record GameFinished() implements GameEvent {
	}
}

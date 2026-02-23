package net.peterv.bazillionaire.game.service;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

import java.util.List;

public sealed interface GameEvent permits GameEvent.OrderFilled, GameEvent.TickerTicked, GameEvent.GameCreated {
	record OrderFilled(Order order, PlayerId playerId) implements GameEvent {
	}

	record TickerTicked(Symbol symbol, Money price) implements GameEvent {
	}

	record GameCreated(List<Symbol> symbols) implements GameEvent {
	}
}

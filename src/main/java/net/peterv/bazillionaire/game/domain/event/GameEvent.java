package net.peterv.bazillionaire.game.domain.event;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

import java.util.List;
import java.util.Map;

public sealed interface GameEvent permits GameEvent.OrderFilled, GameEvent.TickerTicked, GameEvent.GameCreated,
		GameEvent.PlayerJoined, GameEvent.AllPlayersReady, GameEvent.GameState, GameEvent.PlayersState,
		GameEvent.GameFinished, GameEvent.GameTickProgressed, GameEvent.PowerupAwarded,
		GameEvent.FreezeStarted, GameEvent.FreezeExpired, GameEvent.PowerupActivated,
		GameEvent.DividendPaid {
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

	record GameTickProgressed(int tick, int ticksRemaining) implements GameEvent {
	}

	record PowerupAwarded(PlayerId recipient, String powerupName, String description, String usageType,
			String consumptionMode) implements GameEvent {
	}

	record FreezeStarted(PlayerId frozenPlayer, int duration) implements GameEvent {
	}

	record FreezeExpired(PlayerId frozenPlayer) implements GameEvent {
	}

	record PowerupActivated(PlayerId user, String powerupName) implements GameEvent {
	}

	record DividendPaid(PlayerId playerId, Symbol symbol, Money amount, String tierName) implements GameEvent {
	}
}

package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import org.junit.jupiter.api.Test;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class GameOrderTest {

	private static final PlayerId UNKNOWN = new PlayerId("unknown");

	@SafeVarargs
	private void assertOrder(Game game, Order order, PlayerId playerId,
			Class<? extends OrderResult> expectedResult,
			Class<? extends GameEvent>... expectedEvents) {
		var result = game.placeOrder(order, playerId);
		assertInstanceOf(expectedResult, result);
		var messages = game.drainMessages();
		assertEquals(expectedEvents.length, messages.size());
		for (int i = 0; i < expectedEvents.length; i++) {
			assertInstanceOf(expectedEvents[i], messages.get(i).event());
		}
	}

	@Test
	void invalidOrders() {
		var game = startedGame(PLAYER_1);
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Buy(symbol, INITIAL_PRICE), UNKNOWN, OrderResult.InvalidOrder.class);
		assertOrder(game, new Order.Buy(new Symbol("FAKE"), INITIAL_PRICE), PLAYER_1, OrderResult.InvalidOrder.class);
	}

	@Test
	void buyOrders() {
		var game = startedGame(PLAYER_1);
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Buy(symbol, new Money(INITIAL_PRICE.cents() - 1)), PLAYER_1,
				OrderResult.Rejected.class);
		assertOrder(game, new Order.Buy(symbol, new Money(INITIAL_BALANCE.cents() + 1)), PLAYER_1,
				OrderResult.Rejected.class);
		assertOrder(game, new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Filled.class,
				GameEvent.OrderFilled.class, GameEvent.PlayersState.class);
	}

	@Test
	void sellOrders() {
		var game = startedGame(PLAYER_1);
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Sell(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Rejected.class);

		game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
		game.drainMessages();

		assertOrder(game, new Order.Sell(symbol, new Money(INITIAL_PRICE.cents() + 1)), PLAYER_1,
				OrderResult.Rejected.class);
		assertOrder(game, new Order.Sell(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Filled.class,
				GameEvent.OrderFilled.class, GameEvent.PlayersState.class);
	}
}

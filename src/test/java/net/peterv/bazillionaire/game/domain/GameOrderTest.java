package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameOrderTest {

	private static final Money INITIAL_BALANCE = new Money(100_000_00);
	private static final Money INITIAL_PRICE = new Money(100_00);
	private static final int TOTAL_DURATION = 200;
	private static final int STRATEGY_DURATION = 50;
	private static final long SEED = 42L;

	private static final PlayerId PLAYER_1 = new PlayerId("player1");
	private static final PlayerId UNKNOWN = new PlayerId("unknown");

	private Game createReadyGame(List<PlayerId> players) {
		Game game = Game.create(players, 3, INITIAL_BALANCE, INITIAL_PRICE, TOTAL_DURATION, STRATEGY_DURATION,
				new Random(SEED));
		game.drainMessages();
		for (PlayerId player : players) {
			game.join(player);
		}
		game.start();
		game.drainMessages();
		return game;
	}

	private Symbol anySymbol(Game game) {
		return game.currentPrices().keySet().iterator().next();
	}

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
	void unknownPlayerOrderIsInvalid() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Buy(symbol, INITIAL_PRICE), UNKNOWN, OrderResult.InvalidOrder.class);
	}

	@Test
	void unknownSymbolOrderIsInvalid() {
		var game = createReadyGame(List.of(PLAYER_1));
		assertOrder(game, new Order.Buy(new Symbol("FAKE"), INITIAL_PRICE), PLAYER_1, OrderResult.InvalidOrder.class);
	}

	@Test
	void buyAtCurrentPriceFills() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Filled.class,
				GameEvent.OrderFilled.class, GameEvent.PlayersState.class);
	}

	@Test
	void buyBelowCurrentPriceIsRejected() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		var belowPrice = new Money(INITIAL_PRICE.cents() - 1);
		assertOrder(game, new Order.Buy(symbol, belowPrice), PLAYER_1, OrderResult.Rejected.class);
	}

	@Test
	void buyWithInsufficientFundsIsRejected() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		var unaffordablePrice = new Money(INITIAL_BALANCE.cents() + 1);
		assertOrder(game, new Order.Buy(symbol, unaffordablePrice), PLAYER_1, OrderResult.Rejected.class);
	}

	@Test
	void sellWithoutSharesIsRejected() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		assertOrder(game, new Order.Sell(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Rejected.class);
	}

	@Test
	void sellAfterBuyFills() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
		game.drainMessages();
		assertOrder(game, new Order.Sell(symbol, INITIAL_PRICE), PLAYER_1, OrderResult.Filled.class,
				GameEvent.OrderFilled.class, GameEvent.PlayersState.class);
	}

	@Test
	void sellAboveCurrentPriceIsRejected() {
		var game = createReadyGame(List.of(PLAYER_1));
		var symbol = anySymbol(game);
		game.placeOrder(new Order.Buy(symbol, INITIAL_PRICE), PLAYER_1);
		game.drainMessages();
		var abovePrice = new Money(INITIAL_PRICE.cents() + 1);
		assertOrder(game, new Order.Sell(symbol, abovePrice), PLAYER_1, OrderResult.Rejected.class);
	}
}

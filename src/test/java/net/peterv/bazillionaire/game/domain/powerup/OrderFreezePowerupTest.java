package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.Portfolio;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderFreezePowerupTest {

	@Test
	void blocksOrdersFromFrozenPlayerOnly() {
		PlayerId frozen = new PlayerId("p1");
		PlayerId other = new PlayerId("p2");
		OrderFreezePowerup powerup = new OrderFreezePowerup(frozen, 3);
		Order blockedOrder = new Order.Buy(new Symbol("ABC"), new Money(100_00));

		assertInstanceOf(OrderResult.Rejected.class, powerup.intercept(blockedOrder, frozen, null));
		assertNull(powerup.intercept(blockedOrder, other, null));
	}

	@Test
	void emitsFreezeLifecycleMessagesToFrozenPlayerOnly() {
		PlayerId frozen = new PlayerId("p1");
		Game game = new Game(Map.of(frozen, new Portfolio(new Money(1_000_00))), Map.of(), 10);
		OrderFreezePowerup powerup = new OrderFreezePowerup(frozen, 3);

		powerup.onActivate(game);
		powerup.onDeactivate(game);

		List<GameMessage> messages = game.drainMessages();
		assertEquals(2, messages.size());
		assertEquals(new Audience.Only(frozen.value()), messages.get(0).audience());
		assertEquals(new Audience.Only(frozen.value()), messages.get(1).audience());
		assertEquals(new GameEvent.FreezeStarted(frozen, 3), messages.get(0).event());
		assertEquals(new GameEvent.FreezeExpired(frozen), messages.get(1).event());
	}
}

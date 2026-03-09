package net.peterv.bazillionaire.game.domain.powerup;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderFreezePowerupTest {

	@Test
	void blocksOrdersFromFrozenPlayerOnly() {
		PlayerId frozen = new PlayerId("p1");
		PlayerId other = new PlayerId("p2");
		OrderFreezePowerup powerup = new OrderFreezePowerup(3);
		powerup.setTarget(frozen);
		Order blockedOrder = new Order.Buy(new Symbol("ABC"), new Money(100_00));

		assertInstanceOf(OrderResult.Rejected.class, powerup.intercept(blockedOrder, frozen, null));
		assertNull(powerup.intercept(blockedOrder, other, null));
	}

	@Test
	void emitsFreezeLifecycleEffectsToFrozenPlayerOnly() {
		PlayerId frozen = new PlayerId("p1");
		OrderFreezePowerup powerup = new OrderFreezePowerup(3);
		powerup.setTarget(frozen);

		List<PowerupEffect> activateEffects = powerup.onActivate();
		List<PowerupEffect> deactivateEffects = powerup.onDeactivate();

		assertEquals(1, activateEffects.size());
		assertEquals(1, deactivateEffects.size());

		GameMessage activateMsg = ((PowerupEffect.Emit) activateEffects.get(0)).message();
		GameMessage deactivateMsg = ((PowerupEffect.Emit) deactivateEffects.get(0)).message();

		assertEquals(new Audience.Only(frozen), activateMsg.audience());
		assertEquals(new Audience.Only(frozen), deactivateMsg.audience());
		assertEquals(new GameEvent.FreezeStarted(frozen, 3), activateMsg.event());
		assertEquals(new GameEvent.FreezeExpired(frozen), deactivateMsg.event());
	}

	@Test
	void usageTypeIsTargetPlayer() {
		OrderFreezePowerup powerup = new OrderFreezePowerup(3);
		assertEquals(PowerupUsageType.TARGET_PLAYER, powerup.usageType());
	}
}

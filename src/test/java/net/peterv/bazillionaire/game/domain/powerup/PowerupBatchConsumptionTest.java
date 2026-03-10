package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PowerupBatchConsumptionTest {

	private final PlayerId PLAYER = new PlayerId("p1");
	private final PlayerId TARGET = new PlayerId("p2");

	@Test
	void batchConsume3CashBoosts_removes3AndProduces3AddCashEffects() {
		var manager = new PowerupManager();
		for (int i = 0; i < 3; i++) {
			manager.collect(PLAYER, new CashBoostPowerup(PLAYER, new Money(500_00)));
		}

		UsePowerupResult result = manager.usePowerup(PLAYER, "Cash Boost", 3, null);

		assertInstanceOf(UsePowerupResult.Activated.class, result);
		var activated = (UsePowerupResult.Activated) result;

		long addCashCount = activated.effects().stream()
				.filter(PowerupEffect.AddCash.class::isInstance)
				.count();
		assertEquals(3, addCashCount);

		long activatedEvents = activated.effects().stream()
				.filter(PowerupEffect.Emit.class::isInstance)
				.map(e -> ((PowerupEffect.Emit) e).message().event())
				.filter(GameEvent.PowerupActivated.class::isInstance)
				.count();
		assertEquals(3, activatedEvents);

		assertEquals(0, manager.getInventory(PLAYER).size());
	}

	@Test
	void batchConsume5WhenOnly2Owned_consumes2() {
		var manager = new PowerupManager();
		manager.collect(PLAYER, new CashBoostPowerup(PLAYER, new Money(500_00)));
		manager.collect(PLAYER, new CashBoostPowerup(PLAYER, new Money(500_00)));

		UsePowerupResult result = manager.usePowerup(PLAYER, "Cash Boost", 5, null);

		assertInstanceOf(UsePowerupResult.Activated.class, result);
		var activated = (UsePowerupResult.Activated) result;

		long addCashCount = activated.effects().stream()
				.filter(PowerupEffect.AddCash.class::isInstance)
				.count();
		assertEquals(2, addCashCount);
		assertEquals(0, manager.getInventory(PLAYER).size());
	}

	@Test
	void batchConsume1OrderFreeze_singleConsumptionWithTarget() {
		var manager = new PowerupManager();
		manager.collect(PLAYER, new OrderFreezePowerup(5));

		UsePowerupResult result = manager.usePowerup(PLAYER, "Order Freeze", 1, TARGET);

		assertInstanceOf(UsePowerupResult.Activated.class, result);
		assertEquals(0, manager.getInventory(PLAYER).size());
	}

	@Test
	void batchConsume_targetedPowerup_requiresTarget() {
		var manager = new PowerupManager();
		manager.collect(PLAYER, new OrderFreezePowerup(5));

		UsePowerupResult result = manager.usePowerup(PLAYER, "Order Freeze", 1, null);

		assertInstanceOf(UsePowerupResult.InvalidTarget.class, result);
		assertEquals(1, manager.getInventory(PLAYER).size());
	}

	@Test
	void batchConsume_cannotTargetSelf() {
		var manager = new PowerupManager();
		manager.collect(PLAYER, new OrderFreezePowerup(5));

		UsePowerupResult result = manager.usePowerup(PLAYER, "Order Freeze", 1, PLAYER);

		assertInstanceOf(UsePowerupResult.InvalidTarget.class, result);
		assertEquals(1, manager.getInventory(PLAYER).size());
	}
}

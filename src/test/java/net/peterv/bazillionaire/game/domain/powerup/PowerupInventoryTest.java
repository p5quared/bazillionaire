package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PowerupInventoryTest {

	private Game createGame(PlayerId... players) {
		Game game = Game.create(List.of(players), 1, new Money(100_000_00), new Money(100_00), 200, new Random(42));
		for (PlayerId player : players) {
			game.join(player);
		}
		game.start();
		game.drainMessages();
		return game;
	}

	@Test
	void collect_addsToInventory() {
		var manager = new PowerupManager();
		var player = new PlayerId("p1");
		var powerup = new TrackingPowerup(5);

		manager.collect(player, powerup);

		assertEquals(1, manager.getInventory(player).size());
		assertSame(powerup, manager.getInventory(player).get(0));
	}

	@Test
	void use_removesFromInventory() {
		var player = new PlayerId("p1");
		Game game = createGame(player);
		var manager = new PowerupManager();
		manager.collect(player, new TrackingPowerup(5));

		manager.usePowerup(player, "tracking", game);

		assertEquals(0, manager.getInventory(player).size());
	}

	@Test
	void use_activatesPowerup() {
		var player = new PlayerId("p1");
		Game game = createGame(player);
		var manager = new PowerupManager();
		var powerup = new TrackingPowerup(5);
		manager.collect(player, powerup);

		manager.usePowerup(player, "tracking", game);

		assertEquals(1, powerup.activateCount);
	}

	@Test
	void use_powerupNotOwned_returnsNotOwned() {
		var player = new PlayerId("p1");
		Game game = createGame(player);
		var manager = new PowerupManager();

		UsePowerupResult result = manager.usePowerup(player, "tracking", game);

		assertInstanceOf(UsePowerupResult.NotOwned.class, result);
	}

	@Test
	void use_withMultiplePowerups_usesFirstMatch() {
		var player = new PlayerId("p1");
		Game game = createGame(player);
		var manager = new PowerupManager();
		var p1 = new TrackingPowerup(5);
		var p2 = new TrackingPowerup(5);
		manager.collect(player, p1);
		manager.collect(player, p2);

		manager.usePowerup(player, "tracking", game);

		assertEquals(1, manager.getInventory(player).size());
		assertEquals(1, p1.activateCount + p2.activateCount);
	}

	@Test
	void use_byName_onlyMatchingPowerup() {
		var player = new PlayerId("p1");
		Game game = createGame(player);
		var manager = new PowerupManager();

		var tracking = new TrackingPowerup(5);
		var other = new Powerup(3) {
			@Override
			public String name() {
				return "other";
			}

			@Override
			public void onActivate(Game g) {
			}

			@Override
			public void onTick(Game g) {
			}

			@Override
			public void onDeactivate(Game g) {
			}
		};

		manager.collect(player, tracking);
		manager.collect(player, other);

		UsePowerupResult result = manager.usePowerup(player, "other", game);

		assertInstanceOf(UsePowerupResult.Activated.class, result);
		assertEquals(0, tracking.activateCount);
		assertEquals(1, manager.getInventory(player).size());
		assertSame(tracking, manager.getInventory(player).get(0));
	}
}

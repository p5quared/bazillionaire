package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PowerupManagerTest {

	@Test
	void activate_callsOnActivate() {
		var manager = new PowerupManager();
		var p = new TrackingPowerup(5);
		manager.activate(p, null);
		assertEquals(1, p.activateCount);
	}

	@Test
	void tick_expiredPowerupIsDeactivatedAndRemoved() {
		var manager = new PowerupManager();
		var p = new TrackingPowerup(1);
		manager.activate(p, null);

		manager.tick(null); // 1→0, expired → deactivated and removed
		assertEquals(1, p.deactivateCount);

		manager.tick(null); // p is gone — no further ticks or deactivations
		assertEquals(0, p.tickCount);
		assertEquals(1, p.deactivateCount);
	}

	@Test
	void tick_onDeactivateCalledExactlyOnceAtExpiry() {
		var manager = new PowerupManager();
		var p = new TrackingPowerup(2);
		manager.activate(p, null);

		manager.tick(null); // 2→1, not expired
		assertEquals(0, p.deactivateCount);

		manager.tick(null); // 1→0, expired → deactivated
		assertEquals(1, p.deactivateCount);

		manager.tick(null); // already removed
		assertEquals(1, p.deactivateCount);
	}

	@Test
	void tick_permanentPowerupNeverDeactivates() {
		var manager = new PowerupManager();
		var p = new TrackingPowerup(-1);
		manager.activate(p, null);
		for (int i = 0; i < 10; i++)
			manager.tick(null);
		assertEquals(0, p.deactivateCount);
		assertEquals(10, p.tickCount);
	}

	@Test
	void trigger_awardedPowerupActivatedAndEventEmitted() {
		PlayerId player = new PlayerId("p1");
		Game game = Game.create(List.of(player), 1, new Money(100_000_00), new Money(100_00), 200, new Random(42));
		game.drainMessages();
		game.join(player);
		game.start();
		game.drainMessages();

		var manager = new PowerupManager();
		var awarded = new TrackingPowerup(5);
		manager.registerTrigger(context -> List.of(new AwardedPowerup(player, awarded)));

		manager.tick(game);
		List<GameMessage> messages = game.drainMessages();

		assertTrue(messages.stream()
				.map(GameMessage::event)
				.anyMatch(e -> e instanceof GameEvent.PowerupAwarded pa
						&& pa.recipient().equals(player)
						&& pa.powerupName().equals("tracking")));
		assertEquals(1, awarded.activateCount);
	}
}

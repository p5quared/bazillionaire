package net.peterv.bazillionaire.game.domain.powerup;

import org.junit.jupiter.api.Test;

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
}

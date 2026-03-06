package net.peterv.bazillionaire.game.domain.powerup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PowerupTest {

    @Test
    void duration0_isExpiredOnCreation() {
        var p = new TrackingPowerup(0);
        assertTrue(p.isExpired());
    }

    @Test
    void duration1_isNotExpiredOnCreation() {
        var p = new TrackingPowerup(1);
        assertFalse(p.isExpired());
    }

    @Test
    void durationNegative1_neverExpires() {
        var p = new TrackingPowerup(-1);
        for (int i = 0; i < 100; i++) p.tick(null);
        assertFalse(p.isExpired());
    }

    @Test
    void duration0_tickIsInertAndOnTickNeverFires() {
        // Already-expired powerup should be inert when ticked.
        var p = new TrackingPowerup(0);
        p.tick(null);
        assertEquals(0, p.remainingTicks);
        assertEquals(0, p.tickCount);
    }

    @Test
    void duration1_singleTickExpiresImmediatelyAndOnTickNeverFires() {
        // tick: 1→0 (expired). onTick is called only when NOT expired, so fires 0 times.
        var p = new TrackingPowerup(1);
        p.tick(null);
        assertTrue(p.isExpired());
        assertEquals(0, p.tickCount);
    }

    @Test
    void duration2_onTickFiresOnceOnFirstTickThenSkippedAtExpiry() {
        // tick 1: 2→1, not expired → onTick fires
        // tick 2: 1→0, expired   → onTick skipped
        var p = new TrackingPowerup(2);
        p.tick(null);
        assertEquals(1, p.tickCount);
        assertFalse(p.isExpired());

        p.tick(null);
        assertEquals(1, p.tickCount);
        assertTrue(p.isExpired());
    }

    @Test
    void durationNegative1_onTickFiresEveryTick() {
        var p = new TrackingPowerup(-1);
        for (int i = 0; i < 5; i++) p.tick(null);
        assertEquals(5, p.tickCount);
    }
}

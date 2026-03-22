package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import org.junit.jupiter.api.Test;

class PowerupManagerTest {

  private static final GameContext EMPTY_CONTEXT =
      new GameContext(0, Map.of(), Map.of(), List.of(), Set.of());

  @Test
  void activate_callsOnActivate() {
    var manager = new PowerupManager();
    var p = new TrackingPowerup(5);
    manager.activate(p);
    assertEquals(1, p.activateCount);
  }

  @Test
  void tick_expiredPowerupIsDeactivatedAndRemoved() {
    var manager = new PowerupManager();
    var p = new TrackingPowerup(1);
    manager.activate(p);

    manager.tick(EMPTY_CONTEXT); // 1→0, expired → deactivated and removed
    assertEquals(1, p.deactivateCount);

    manager.tick(EMPTY_CONTEXT); // p is gone — no further ticks or deactivations
    assertEquals(0, p.tickCount);
    assertEquals(1, p.deactivateCount);
  }

  @Test
  void tick_onDeactivateCalledExactlyOnceAtExpiry() {
    var manager = new PowerupManager();
    var p = new TrackingPowerup(2);
    manager.activate(p);

    manager.tick(EMPTY_CONTEXT); // 2→1, not expired
    assertEquals(0, p.deactivateCount);

    manager.tick(EMPTY_CONTEXT); // 1→0, expired → deactivated
    assertEquals(1, p.deactivateCount);

    manager.tick(EMPTY_CONTEXT); // already removed
    assertEquals(1, p.deactivateCount);
  }

  @Test
  void tick_permanentPowerupNeverDeactivates() {
    var manager = new PowerupManager();
    var p = new TrackingPowerup(-1);
    manager.activate(p);
    for (int i = 0; i < 10; i++) manager.tick(EMPTY_CONTEXT);
    assertEquals(0, p.deactivateCount);
    assertEquals(10, p.tickCount);
  }

  @Test
  void trigger_awardedPowerupCollectedAndEventEmitted() {
    PlayerId player = new PlayerId("p1");
    GameContext context =
        new GameContext(
            0,
            Map.of(player, new GameEvent.PlayerPortfolio(new Money(100_000_00), Map.of())),
            Map.of(),
            List.of(),
            Set.of());

    var manager = new PowerupManager();
    var awarded = new TrackingPowerup(5);
    manager.registerTrigger(ctx -> List.of(new AwardedPowerup(player, awarded)));

    List<PowerupEffect> effects = manager.tick(context);

    boolean hasAwardEvent =
        effects.stream()
            .filter(PowerupEffect.Emit.class::isInstance)
            .map(e -> ((PowerupEffect.Emit) e).message())
            .map(GameMessage::event)
            .anyMatch(
                e ->
                    e instanceof GameEvent.PowerupAwarded pa
                        && pa.recipient().equals(player)
                        && pa.powerupName().equals("tracking")
                        && pa.description().equals("test")
                        && pa.usageType().equals("instant"));
    assertTrue(hasAwardEvent);
    // Powerup is now collected into inventory, not immediately activated
    assertEquals(0, awarded.activateCount);
    assertEquals(1, manager.getInventory(player).size());
  }
}

package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class DividendTriggerTest {

  private static final PlayerId PLAYER_1 = new PlayerId("player1");
  private static final PlayerId PLAYER_2 = new PlayerId("player2");
  private static final Symbol AAPL = new Symbol("AAPL");
  private static final Symbol GOOG = new Symbol("GOOG");
  private static final Money INITIAL_PRICE = new Money(100_00);

  private DividendTrigger trigger = new DividendTrigger(20, INITIAL_PRICE);

  private GameContext contextWithHoldings(
      int tick, PlayerId playerId, Map<Symbol, Integer> holdings) {
    Map<PlayerId, GameEvent.PlayerPortfolio> players = new HashMap<>();
    players.put(playerId, new GameEvent.PlayerPortfolio(new Money(100_000_00), holdings));
    return new GameContext(
        tick, players, Map.of(AAPL, INITIAL_PRICE, GOOG, INITIAL_PRICE), List.of());
  }

  private GameContext contextWithTwoPlayers(
      int tick, Map<Symbol, Integer> p1Holdings, Map<Symbol, Integer> p2Holdings) {
    Map<PlayerId, GameEvent.PlayerPortfolio> players = new HashMap<>();
    players.put(PLAYER_1, new GameEvent.PlayerPortfolio(new Money(100_000_00), p1Holdings));
    players.put(PLAYER_2, new GameEvent.PlayerPortfolio(new Money(100_000_00), p2Holdings));
    return new GameContext(
        tick, players, Map.of(AAPL, INITIAL_PRICE, GOOG, INITIAL_PRICE), List.of());
  }

  @Test
  void noAwardOnNonIntervalTick() {
    trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 15)));
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(5, PLAYER_1, Map.of(AAPL, 15)));
    assertTrue(awards.isEmpty());
  }

  @Test
  void noAwardAtTickZero() {
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 15)));
    assertTrue(awards.isEmpty());
  }

  @Test
  void noAwardWithLessThanFiveShares() {
    for (int tick = 0; tick <= 400; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 4)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(400, PLAYER_1, Map.of(AAPL, 4)));
    assertTrue(awards.isEmpty());
  }

  @Test
  void tier1AwardAfter300TickHoldWithFiveShares() {
    // Start holding at tick 0
    trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 5)));
    // Advance to tick 300 (hold duration = 300, which meets tier 1's 300 requirement)
    for (int tick = 1; tick < 300; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(300, PLAYER_1, Map.of(AAPL, 5)));
    // tick 300 is not a payout interval (300 % 20 == 0), so should get an award
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals(PLAYER_1, powerup.recipient());
    assertEquals(AAPL, powerup.symbol());
    assertEquals("Tier 1", powerup.tierName());
  }

  @Test
  void tier2InstantWhenHoldTimeCarriesOver() {
    // Hold 5 shares starting at tick 0
    trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 5)));
    for (int tick = 1; tick < 100; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5)));
    }
    // At tick 100, buy up to 10 shares — hold duration is 100 (meets tier 2's 100 requirement)
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(100, PLAYER_1, Map.of(AAPL, 10)));
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals("Tier 2", powerup.tierName());
  }

  @Test
  void tier3InstantWithFifteenShares() {
    // Tier 3 requires 15 shares, 0 hold ticks — should qualify immediately on first payout tick
    trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 15)));
    for (int tick = 1; tick < 20; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 15)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(20, PLAYER_1, Map.of(AAPL, 15)));
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals("Tier 3", powerup.tierName());
  }

  @Test
  void holdTimerResetsWhenDroppingBelowFiveShares() {
    // Hold 5 shares from tick 0 to tick 200
    for (int tick = 0; tick <= 200; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5)));
    }
    // Drop below 5 at tick 201
    trigger.evaluate(contextWithHoldings(201, PLAYER_1, Map.of(AAPL, 3)));
    // Back to 5 at tick 202
    trigger.evaluate(contextWithHoldings(202, PLAYER_1, Map.of(AAPL, 5)));
    // At tick 300 (payout tick), hold duration from 202 is only 98 — not enough for tier 1 (needs
    // 300)
    for (int tick = 203; tick < 300; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(300, PLAYER_1, Map.of(AAPL, 5)));
    assertTrue(awards.isEmpty());
  }

  @Test
  void multipleSymbolsTrackedIndependently() {
    // Start holding AAPL at tick 0, GOOG at tick 0 with different share counts
    trigger.evaluate(contextWithHoldings(0, PLAYER_1, Map.of(AAPL, 15, GOOG, 5)));
    for (int tick = 1; tick < 20; tick++) {
      trigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 15, GOOG, 5)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithHoldings(20, PLAYER_1, Map.of(AAPL, 15, GOOG, 5)));
    // AAPL: 15 shares, hold 20 ticks — tier 3 (0 hold req)
    // GOOG: 5 shares, hold 20 ticks — no tier (needs 300 for tier 1)
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals(AAPL, powerup.symbol());
    assertEquals("Tier 3", powerup.tierName());
  }

  @Test
  void multiplePlayersGetIndependentAwards() {
    // Both players hold 15 shares of AAPL
    trigger.evaluate(contextWithTwoPlayers(0, Map.of(AAPL, 15), Map.of(AAPL, 15)));
    for (int tick = 1; tick < 20; tick++) {
      trigger.evaluate(contextWithTwoPlayers(tick, Map.of(AAPL, 15), Map.of(AAPL, 15)));
    }
    List<AwardedPowerup> awards =
        trigger.evaluate(contextWithTwoPlayers(20, Map.of(AAPL, 15), Map.of(AAPL, 15)));
    assertEquals(2, awards.size());
  }

  @Test
  void correctPayoutAmount() {
    // Tier 1: 500 basis points, initial price $1.00 (100 cents), 5 shares
    // payout = 500 * 100 * 5 / 10000 = 25 cents = $0.25
    Money cheapPrice = new Money(100);
    DividendTrigger cheapTrigger = new DividendTrigger(20, cheapPrice);
    GameContext ctx;
    for (int tick = 0; tick <= 300; tick++) {
      ctx = contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5));
      // Override initial price for this trigger
      cheapTrigger.evaluate(ctx);
    }
    // Use the main trigger with $100 initial price instead
    // Tier 1: 500 basis points, $100.00 (10000 cents), 5 shares
    // payout = 500 * 10000 * 5 / 10000 = 2500 cents = $25.00
    DividendTrigger mainTrigger = new DividendTrigger(20, INITIAL_PRICE);
    for (int tick = 0; tick < 300; tick++) {
      mainTrigger.evaluate(contextWithHoldings(tick, PLAYER_1, Map.of(AAPL, 5)));
    }
    List<AwardedPowerup> awards =
        mainTrigger.evaluate(contextWithHoldings(300, PLAYER_1, Map.of(AAPL, 5)));
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals(new Money(2500), powerup.payoutAmount());
  }

  @Test
  void payoutBlendsInitialAndCurrentPrice() {
    // Tier 1: 500 basis points, initial=$100 (10000c), current=$200 (20000c), 5 shares
    // blended = (10000 + 20000) / 2 = 15000
    // payout = 500 * 15000 * 5 / 10000 = 3750 cents = $37.50
    Money highPrice = new Money(200_00);
    DividendTrigger blendTrigger = new DividendTrigger(20, INITIAL_PRICE);
    Map<PlayerId, GameEvent.PlayerPortfolio> players = new HashMap<>();
    players.put(PLAYER_1, new GameEvent.PlayerPortfolio(new Money(100_000_00), Map.of(AAPL, 5)));
    for (int tick = 0; tick < 300; tick++) {
      blendTrigger.evaluate(
          new GameContext(tick, players, Map.of(AAPL, highPrice, GOOG, INITIAL_PRICE), List.of()));
    }
    List<AwardedPowerup> awards =
        blendTrigger.evaluate(
            new GameContext(300, players, Map.of(AAPL, highPrice, GOOG, INITIAL_PRICE), List.of()));
    assertEquals(1, awards.size());
    DividendPowerup powerup = (DividendPowerup) awards.get(0).powerup();
    assertEquals(new Money(3750), powerup.payoutAmount());
  }

  @Test
  void dividendPowerupActivationReturnsCorrectEffects() {
    DividendPowerup powerup = new DividendPowerup(PLAYER_1, new Money(250), AAPL, "Tier 1");
    List<PowerupEffect> effects = powerup.onActivate();
    assertEquals(2, effects.size());

    PowerupEffect.AddCash addCash = (PowerupEffect.AddCash) effects.get(0);
    assertEquals(PLAYER_1, addCash.player());
    assertEquals(new Money(250), addCash.amount());

    PowerupEffect.Emit emit = (PowerupEffect.Emit) effects.get(1);
    GameMessage message = emit.message();
    GameEvent.DividendPaid event = (GameEvent.DividendPaid) message.event();
    assertEquals(PLAYER_1, event.playerId());
    assertEquals(AAPL, event.symbol());
    assertEquals(new Money(250), event.amount());
    assertEquals("Tier 1", event.tierName());
  }
}

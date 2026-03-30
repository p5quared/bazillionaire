package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static net.peterv.bazillionaire.game.domain.GameTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.DarkPoolPowerup;
import net.peterv.bazillionaire.game.domain.powerup.DarkPoolTier;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class DarkPoolTest {

  /** Game with high balance so tests don't hit Insufficient funds before liquidity limits. */
  private static Game highBalanceGame() {
    Game game =
        GameFactory.create(
            List.of(PLAYER_1, PLAYER_2),
            TICKER_COUNT,
            new Money(1_000_000_00),
            TOTAL_DURATION,
            new Random(SEED));
    game.drainMessages();
    game.join(PLAYER_1);
    game.join(PLAYER_2);
    game.start();
    game.drainMessages();
    return game;
  }

  @Test
  void darkPoolTradesBypassBubbleTracking() {
    Game game = highBalanceGame();
    Symbol symbol = anySymbol(game);

    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    // Trade heavily through dark pool — buy+sell cycles to maximize bubble impact
    for (int i = 0; i < 6; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
      game.placeOrder(new Order.Sell(symbol), PLAYER_1);
      game.drainMessages();
    }

    // Tick to evaluate bubbles
    game.tick();
    List<GameMessage> messages = game.drainMessages();

    List<GameEvent.BubbleWarning> warnings =
        messages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.BubbleWarning.class::isInstance)
            .map(GameEvent.BubbleWarning.class::cast)
            .filter(w -> w.symbol().equals(symbol))
            .toList();

    assertTrue(warnings.isEmpty(), "Dark pool trades should not trigger bubble warnings");
  }

  @Test
  void darkPoolTradesBypassLiquidity() {
    Game game = highBalanceGame();
    Symbol symbol = anySymbol(game);

    // Exhaust normal liquidity (25 tokens)
    int normalFills = 0;
    while (game.placeOrder(new Order.Buy(symbol), PLAYER_1) instanceof OrderResult.Filled) {
      game.drainMessages();
      normalFills++;
    }
    game.drainMessages();

    assertTrue(normalFills > 0, "Should have filled at least one order normally");

    // Activate dark pool — should provide separate liquidity
    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    OrderResult darkPoolResult = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();

    assertInstanceOf(
        OrderResult.Filled.class,
        darkPoolResult,
        "Dark pool should bypass normal liquidity limits");
  }

  @Test
  void darkPoolTradesMarkedInOrderActivity() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    Symbol symbol = anySymbol(game);

    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    List<GameMessage> messages = game.drainMessages();

    List<GameEvent.OrderActivity> activities =
        messages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.OrderActivity.class::isInstance)
            .map(GameEvent.OrderActivity.class::cast)
            .toList();

    assertFalse(activities.isEmpty(), "Should have OrderActivity events");
    assertTrue(activities.get(0).darkPool(), "Dark pool trades should be marked as darkPool=true");
  }

  @Test
  void normalTradesNotMarkedAsDarkPool() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    Symbol symbol = anySymbol(game);

    game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    List<GameMessage> messages = game.drainMessages();

    List<GameEvent.OrderActivity> activities =
        messages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.OrderActivity.class::isInstance)
            .map(GameEvent.OrderActivity.class::cast)
            .toList();

    assertFalse(activities.isEmpty());
    assertFalse(activities.get(0).darkPool(), "Normal trades should not be marked as dark pool");
  }

  @Test
  void darkPoolActivationIsStealth() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    Symbol symbol = anySymbol(game);

    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    List<GameMessage> messages = game.drainMessages();

    // Should have DarkPoolActivated as private event to owner
    List<GameMessage> darkPoolEvents =
        messages.stream().filter(m -> m.event() instanceof GameEvent.DarkPoolActivated).toList();
    assertFalse(darkPoolEvents.isEmpty(), "DarkPoolActivated event should be emitted");
    assertEquals(
        new Audience.Only(PLAYER_1),
        darkPoolEvents.get(0).audience(),
        "DarkPoolActivated should be private to the owner");

    // Should NOT have a broadcast PowerupActivated
    List<GameMessage> broadcastActivations =
        messages.stream()
            .filter(m -> m.audience() instanceof Audience.Everyone)
            .filter(m -> m.event() instanceof GameEvent.PowerupActivated)
            .toList();
    assertTrue(
        broadcastActivations.isEmpty(),
        "Dark pool should NOT broadcast PowerupActivated (stealth)");
  }

  @Test
  void darkPoolExpiresWhenTokensExhausted() {
    Game game = highBalanceGame();
    Symbol symbol = anySymbol(game);

    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    // Consume all 12 tokens
    for (int i = 0; i < 12; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }

    // Tick to trigger expiry check
    game.tick();
    List<GameMessage> messages = game.drainMessages();

    boolean hasExpired =
        messages.stream()
            .map(GameMessage::event)
            .anyMatch(GameEvent.DarkPoolExpired.class::isInstance);

    assertTrue(hasExpired, "Dark pool should expire when all tokens are consumed");
  }

  @Test
  void darkPoolExpiresWhenTicksExhausted() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    Symbol symbol = anySymbol(game);

    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    // Tick through the full duration (30 ticks)
    boolean foundExpiry = false;
    for (int i = 0; i < 35; i++) {
      game.tick();
      List<GameMessage> messages = game.drainMessages();
      if (messages.stream()
          .map(GameMessage::event)
          .anyMatch(GameEvent.DarkPoolExpired.class::isInstance)) {
        foundExpiry = true;
        break;
      }
    }

    assertTrue(foundExpiry, "Dark pool should expire after tick duration");
  }

  @Test
  void normalLiquidityResumesAfterDarkPoolExpires() {
    Game game = highBalanceGame();
    Symbol symbol = anySymbol(game);

    // Activate dark pool and trade through it
    activateDarkPool(game, PLAYER_1, symbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    for (int i = 0; i < 12; i++) {
      game.placeOrder(new Order.Buy(symbol), PLAYER_1);
      game.drainMessages();
    }

    // Tick to expire dark pool
    game.tick();
    game.drainMessages();

    // Normal liquidity should be untouched — trade should succeed
    OrderResult result = game.placeOrder(new Order.Buy(symbol), PLAYER_1);
    game.drainMessages();

    assertInstanceOf(
        OrderResult.Filled.class,
        result,
        "Normal liquidity should be unaffected by dark pool trades");
  }

  @Test
  void premiumDarkPoolAppliesToAllSymbols() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    List<Symbol> symbols = List.copyOf(game.currentPrices().keySet());
    assertTrue(symbols.size() >= 2, "Need at least 2 symbols for this test");

    var darkPool = new DarkPoolPowerup(DarkPoolTier.PREMIUM, PLAYER_1);
    game.activatePowerup(darkPool);
    game.drainMessages();

    // Trade on different symbols — both should work through dark pool
    OrderResult result1 = game.placeOrder(new Order.Buy(symbols.get(0)), PLAYER_1);
    game.drainMessages();
    OrderResult result2 = game.placeOrder(new Order.Buy(symbols.get(1)), PLAYER_1);
    List<GameMessage> messages = game.drainMessages();

    assertInstanceOf(OrderResult.Filled.class, result1);
    assertInstanceOf(OrderResult.Filled.class, result2);

    List<GameEvent.OrderActivity> activities =
        messages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.OrderActivity.class::isInstance)
            .map(GameEvent.OrderActivity.class::cast)
            .toList();
    assertTrue(
        activities.stream().allMatch(GameEvent.OrderActivity::darkPool),
        "Premium dark pool trades should be marked on all symbols");
  }

  @Test
  void standardDarkPoolDoesNotApplyToOtherSymbols() {
    Game game = startedGame(PLAYER_1, PLAYER_2);
    List<Symbol> symbols = List.copyOf(game.currentPrices().keySet());
    assertTrue(symbols.size() >= 2, "Need at least 2 symbols");

    Symbol targetSymbol = symbols.get(0);
    Symbol otherSymbol = symbols.get(1);

    activateDarkPool(game, PLAYER_1, targetSymbol, DarkPoolTier.STANDARD);
    game.drainMessages();

    game.placeOrder(new Order.Buy(otherSymbol), PLAYER_1);
    List<GameMessage> messages = game.drainMessages();

    List<GameEvent.OrderActivity> activities =
        messages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.OrderActivity.class::isInstance)
            .map(GameEvent.OrderActivity.class::cast)
            .toList();

    assertFalse(activities.isEmpty());
    assertFalse(
        activities.get(0).darkPool(), "Standard dark pool should not apply to non-target symbols");
  }

  private void activateDarkPool(Game game, PlayerId player, Symbol symbol, DarkPoolTier tier) {
    var darkPool = new DarkPoolPowerup(tier, player);
    if (!tier.allSymbols()) {
      darkPool.setSymbolTarget(symbol);
    }
    game.activatePowerup(darkPool);
  }
}

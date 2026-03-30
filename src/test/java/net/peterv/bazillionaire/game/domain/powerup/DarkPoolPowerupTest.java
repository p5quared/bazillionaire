package net.peterv.bazillionaire.game.domain.powerup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DarkPoolPowerupTest {
  private static final PlayerId OWNER = new PlayerId("player1");
  private static final Symbol AAPL = new Symbol("AAPL");
  private static final Symbol GOOG = new Symbol("GOOG");

  @Test
  void onActivateReturnsDarkPoolActivatedPrivateToOwner() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    powerup.setSymbolTarget(AAPL);
    List<PowerupEffect> effects = powerup.onActivate();

    assertEquals(1, effects.size());
    assertInstanceOf(PowerupEffect.Emit.class, effects.get(0));

    GameMessage message = ((PowerupEffect.Emit) effects.get(0)).message();
    assertEquals(new Audience.Only(OWNER), message.audience());
    assertInstanceOf(GameEvent.DarkPoolActivated.class, message.event());

    var activated = (GameEvent.DarkPoolActivated) message.event();
    assertEquals(OWNER, activated.player());
    assertEquals("Dark Pool", activated.tierName());
    assertEquals(AAPL, activated.targetSymbol());
    assertEquals(12, activated.tokens());
    assertEquals(30, activated.ticks());
  }

  @Test
  void onDeactivateReturnsDarkPoolExpiredPrivateToOwner() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    List<PowerupEffect> effects = powerup.onDeactivate();

    assertEquals(1, effects.size());
    assertInstanceOf(PowerupEffect.Emit.class, effects.get(0));

    GameMessage message = ((PowerupEffect.Emit) effects.get(0)).message();
    assertEquals(new Audience.Only(OWNER), message.audience());
    assertEquals(new GameEvent.DarkPoolExpired(OWNER), message.event());
  }

  @Test
  void isExpiredWhenTicksExhausted() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    assertFalse(powerup.isExpired());

    for (int i = 0; i < 30; i++) {
      powerup.tick();
    }
    assertTrue(powerup.isExpired());
  }

  @Test
  void isExpiredWhenTokensExhausted() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    assertFalse(powerup.isExpired());

    for (int i = 0; i < 12; i++) {
      powerup.consumeToken();
    }
    assertTrue(powerup.isExpired());
  }

  @Test
  void notExpiredWhenBothRemain() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    powerup.tick();
    powerup.consumeToken();
    assertFalse(powerup.isExpired());
  }

  @Test
  void appliesToTargetSymbolForStandard() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    powerup.setSymbolTarget(AAPL);

    assertTrue(powerup.appliesTo(AAPL));
    assertFalse(powerup.appliesTo(GOOG));
  }

  @Test
  void appliesToAllSymbolsForPremium() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.PREMIUM, OWNER);

    assertTrue(powerup.appliesTo(AAPL));
    assertTrue(powerup.appliesTo(GOOG));
  }

  @Test
  void consumeTokenDecrementsCount() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    assertEquals(12, powerup.remainingTokens());

    powerup.consumeToken();
    assertEquals(11, powerup.remainingTokens());
  }

  @Test
  void hasTokensFalseWhenZero() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    for (int i = 0; i < 12; i++) {
      assertTrue(powerup.hasTokens());
      powerup.consumeToken();
    }
    assertFalse(powerup.hasTokens());
  }

  @Test
  void consumeTokenDoesNotGoBelowZero() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    for (int i = 0; i < 15; i++) {
      powerup.consumeToken();
    }
    assertEquals(0, powerup.remainingTokens());
  }

  @Test
  void usageTypeMatchesTier() {
    var standard = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    assertEquals(PowerupUsageType.TARGET_SYMBOL, standard.usageType());

    var premium = new DarkPoolPowerup(DarkPoolTier.PREMIUM, OWNER);
    assertEquals(PowerupUsageType.INSTANT, premium.usageType());
  }

  @Test
  void broadcastsActivationIsFalse() {
    var powerup = new DarkPoolPowerup(DarkPoolTier.STANDARD, OWNER);
    assertFalse(powerup.broadcastsActivation());
  }

  @ParameterizedTest
  @EnumSource(DarkPoolTier.class)
  void metadataMatchesTier(DarkPoolTier tier) {
    var powerup = new DarkPoolPowerup(tier, OWNER);
    assertEquals(tier.displayName(), powerup.name());
    assertEquals(tier.description(), powerup.description());
    assertEquals(ConsumptionMode.SINGLE, powerup.consumptionMode());
  }

  @ParameterizedTest
  @EnumSource(DarkPoolTier.class)
  void premiumActivatedEventHasNullTargetSymbol(DarkPoolTier tier) {
    var powerup = new DarkPoolPowerup(tier, OWNER);
    if (tier == DarkPoolTier.STANDARD) {
      powerup.setSymbolTarget(AAPL);
    }

    var effects = powerup.onActivate();
    var message = ((PowerupEffect.Emit) effects.get(0)).message();
    var activated = (GameEvent.DarkPoolActivated) message.event();

    if (tier == DarkPoolTier.PREMIUM) {
      assertNull(activated.targetSymbol());
    } else {
      assertEquals(AAPL, activated.targetSymbol());
    }
  }
}

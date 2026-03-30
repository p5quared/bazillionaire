package net.peterv.bazillionaire.game.domain;

import static net.peterv.bazillionaire.game.domain.GameTestDefaults.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.ticker.BubbleTracker;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.ticker.regime.DefaultRegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.InfluencedRegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.Test;

class GameBubbleTest {

  private static final Symbol SYMBOL = new Symbol("TEST");
  private static final int BUBBLE_THRESHOLD = 5;

  private Game createGameWithBubbleConfig() {
    BubbleTracker tracker = new BubbleTracker(30, BUBBLE_THRESHOLD);
    Random random = new Random(SEED);
    Ticker ticker =
        new Ticker(
            new InfluencedRegimeFactory(new DefaultRegimeFactory(random, MarketCap.MID_CAP)),
            MarketCap.MID_CAP.initialPrice(),
            MarketCap.MID_CAP,
            tracker,
            random);

    Map<Symbol, Ticker> tickers = new HashMap<>();
    tickers.put(SYMBOL, ticker);

    Map<PlayerId, Portfolio> players = new HashMap<>();
    players.put(PLAYER_1, new Portfolio(INITIAL_BALANCE));
    players.put(PLAYER_2, new Portfolio(INITIAL_BALANCE));

    LiquidityProvider liquidity = new TokenBucketLiquidityLimiter(100, 1);
    Game game = new Game(players, tickers, 200, liquidity);
    game.join(PLAYER_1);
    game.join(PLAYER_2);
    game.start();
    game.drainMessages();
    return game;
  }

  @Test
  void bubbleWarningEmittedWhenThresholdCrossed() {
    Game game = createGameWithBubbleConfig();

    for (int i = 0; i < BUBBLE_THRESHOLD; i++) {
      game.placeOrder(new Order.Buy(SYMBOL), PLAYER_1);
    }
    game.drainMessages();

    game.tick();
    List<GameMessage> messages = game.drainMessages();

    boolean hasBubbleWarning =
        messages.stream()
            .map(GameMessage::event)
            .anyMatch(e -> e instanceof GameEvent.BubbleWarning);

    assertTrue(hasBubbleWarning, "Should emit BubbleWarning when threshold crossed");
  }

  @Test
  void ordersRejectedAfterDelisting() {
    // Use a very low threshold so sustained trading keeps the bubble active
    BubbleTracker tracker = new BubbleTracker(30, 2);
    Random random = new Random(SEED);
    Ticker ticker =
        new Ticker(
            new InfluencedRegimeFactory(new DefaultRegimeFactory(random, MarketCap.STARTUP)),
            MarketCap.STARTUP.initialPrice(),
            MarketCap.STARTUP,
            tracker,
            random);

    Map<Symbol, Ticker> tickers = new HashMap<>();
    tickers.put(SYMBOL, ticker);

    Map<PlayerId, Portfolio> players = new HashMap<>();
    players.put(PLAYER_1, new Portfolio(new Money(999_999_99)));

    Game game = new Game(players, tickers, 2000, new TokenBucketLiquidityLimiter(100, 1));
    game.join(PLAYER_1);
    game.start();
    game.drainMessages();

    // Sustain bubble by continuously trading while ticking
    for (int i = 0; i < 2000; i++) {
      game.placeOrder(new Order.Buy(SYMBOL), PLAYER_1);
      game.placeOrder(new Order.Buy(SYMBOL), PLAYER_1);
      game.tick();
      game.drainMessages();
      if (game.currentPrices().get(SYMBOL).cents() <= 0) break;
    }

    OrderResult result = game.placeOrder(new Order.Buy(SYMBOL), PLAYER_1);
    assertTrue(
        result instanceof OrderResult.Rejected,
        "Orders should be rejected after delisting, got: " + result);
  }

  @Test
  void gameCanFinishWithDelistedTicker() {
    Map<PlayerId, Portfolio> players = new HashMap<>();
    players.put(PLAYER_1, new Portfolio(INITIAL_BALANCE));

    BubbleTracker tracker = new BubbleTracker(30, 3);
    Random random = new Random(SEED);
    Ticker ticker =
        new Ticker(
            new InfluencedRegimeFactory(new DefaultRegimeFactory(random, MarketCap.STARTUP)),
            MarketCap.STARTUP.initialPrice(),
            MarketCap.STARTUP,
            tracker,
            random);

    Map<Symbol, Ticker> tickers = new HashMap<>();
    tickers.put(SYMBOL, ticker);

    Game game = new Game(players, tickers, 50, new TokenBucketLiquidityLimiter(100, 1));
    game.join(PLAYER_1);
    game.start();
    game.drainMessages();

    for (int i = 0; i < 5; i++) {
      game.placeOrder(new Order.Buy(SYMBOL), PLAYER_1);
    }

    for (int i = 0; i < 50; i++) {
      game.tick();
    }

    List<GameMessage> messages = game.drainMessages();
    boolean hasGameFinished =
        messages.stream()
            .map(GameMessage::event)
            .anyMatch(e -> e instanceof GameEvent.GameFinished);

    assertTrue(hasGameFinished, "Game should still finish even with delisted tickers");
  }

  // @Test
  // void startupBubblesMoreEasilyThanBlueChip() {
  //   int startupThreshold = MarketCap.STARTUP.createBubbleTracker().threshold();
  //   int blueChipThreshold = MarketCap.BLUE_CHIP.createBubbleTracker().threshold();
  //   assertTrue(
  //       startupThreshold < blueChipThreshold,
  //       "STARTUP threshold (%d) should be lower than BLUE_CHIP (%d)"
  //           .formatted(startupThreshold, blueChipThreshold));
  // }
}

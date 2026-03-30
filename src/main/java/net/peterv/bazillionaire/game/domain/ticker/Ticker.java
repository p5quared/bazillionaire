package net.peterv.bazillionaire.game.domain.ticker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.ticker.regime.InfluencedRegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.MarketSentiment;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.RegimeStrategy;
import net.peterv.bazillionaire.game.domain.ticker.regime.SentimentInfluence;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class Ticker {
  private static final double BEAR_PROBABILITY = 0.80;

  private final RegimeFactory regimeFactory;
  private final MarketCap marketCap;
  private final BubbleTracker bubbleTracker;
  private final Random random;
  private List<RegimeStrategy> regimes = new ArrayList<>();
  private int cursor = 0;

  public Ticker(RegimeFactory regimeFactory, Money initialPrice, MarketCap marketCap) {
    this(regimeFactory, initialPrice, marketCap, marketCap.createBubbleTracker(), new Random());
  }

  public Ticker(
      RegimeFactory regimeFactory,
      Money initialPrice,
      MarketCap marketCap,
      BubbleTracker bubbleTracker,
      Random random) {
    this.regimeFactory = regimeFactory;
    this.marketCap = marketCap;
    this.bubbleTracker = bubbleTracker;
    this.random = random;
    this.regimes.add(regimeFactory.nextRegime(initialPrice));
  }

  public MarketCap marketCap() {
    return marketCap;
  }

  public Money currentPrice() {
    if (isDelisted()) {
      return new Money(0);
    }
    return regimes.getLast().prices().get(cursor);
  }

  public void tick() {
    if (isDelisted()) {
      return;
    }
    cursor++;
    if (cursor >= regimes.getLast().prices().size()) {
      Money lastPrice = regimes.getLast().prices().getLast();
      this.regimes.add(this.regimeFactory.nextRegime(lastPrice));
      cursor = 0;
    }
  }

  public void queueSentimentInfluence(SentimentInfluence influence) {
    if (regimeFactory instanceof InfluencedRegimeFactory influenced) {
      influenced.queueInfluence(influence);
    }
  }

  public void recordTrade(int tick, int points) {
    bubbleTracker.recordTrade(tick, points);
  }

  public List<GameEvent> evaluateBubble(int currentTick, Symbol symbol) {
    BubbleState previousState = bubbleTracker.state();
    bubbleTracker.onTick(currentTick);
    BubbleState currentState = bubbleTracker.state();

    List<GameEvent> events = new ArrayList<>();

    if (previousState == BubbleState.NORMAL && currentState == BubbleState.OVERHEATED) {
      events.add(
          new GameEvent.BubbleWarning(
              symbol, bubbleTracker.bubbleFactor(), bubbleTracker.threshold()));
    }

    if (currentState == BubbleState.OVERHEATED && random.nextDouble() < BEAR_PROBABILITY) {
      queueSentimentInfluence(new SentimentInfluence(MarketSentiment.BEAR, 0, 1));
    }

    if (currentPrice().cents() <= 1) {
      bubbleTracker.markDelisted();
      events.add(new GameEvent.TickerDelisted(symbol));
    }

    return events;
  }

  public boolean isDelisted() {
    return bubbleTracker.state() == BubbleState.DELISTED;
  }

  public BubbleState bubbleState() {
    return bubbleTracker.state();
  }

  public int bubbleFactor() {
    return bubbleTracker.bubbleFactor();
  }

  public int bubbleThreshold() {
    return bubbleTracker.threshold();
  }
}

package net.peterv.bazillionaire.game.domain.ticker.regime;

import java.util.ArrayDeque;
import java.util.Deque;
import net.peterv.bazillionaire.game.domain.types.Money;

public class InfluencedRegimeFactory implements RegimeFactory {

  private final RegimeFactory delegate;
  private final Deque<ActiveInfluence> queue = new ArrayDeque<>();

  public InfluencedRegimeFactory(RegimeFactory delegate) {
    this.delegate = delegate;
  }

  public void queueInfluence(SentimentInfluence influence) {
    queue.addLast(new ActiveInfluence(influence));
  }

  @Override
  public RegimeStrategy nextRegime(Money lastPrice) {
    if (queue.isEmpty()) {
      return delegate.nextRegime(lastPrice);
    }

    ActiveInfluence front = queue.peekFirst();
    if (front.delayRemaining > 0) {
      front.delayRemaining--;
      return delegate.nextRegime(lastPrice);
    }

    RegimeStrategy regime = delegate.nextRegime(lastPrice, front.sentiment);
    front.durationRemaining--;
    if (front.durationRemaining <= 0) {
      queue.pollFirst();
    }
    return regime;
  }

  @Override
  public RegimeStrategy nextRegime(Money lastPrice, MarketSentiment forcedSentiment) {
    return delegate.nextRegime(lastPrice, forcedSentiment);
  }

  private static class ActiveInfluence {
    final MarketSentiment sentiment;
    int delayRemaining;
    int durationRemaining;

    ActiveInfluence(SentimentInfluence influence) {
      this.sentiment = influence.forcedSentiment();
      this.delayRemaining = influence.delayRegimes();
      this.durationRemaining = influence.durationRegimes();
    }
  }
}

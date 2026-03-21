package net.peterv.bazillionaire.game.domain.ticker.regime;

public record SentimentInfluence(
    MarketSentiment forcedSentiment, int delayRegimes, int durationRegimes) {

  public SentimentInfluence {
    if (delayRegimes < 0) {
      throw new IllegalArgumentException("delayRegimes must be non-negative");
    }
    if (durationRegimes < 1) {
      throw new IllegalArgumentException("durationRegimes must be at least 1");
    }
  }
}

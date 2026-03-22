package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.ticker.regime.MarketSentiment;

public enum SentimentBoostTier {
  MINOR(
      "Sentiment Boost",
      "Boost a stock's sentiment for several regimes",
      MarketSentiment.BULL,
      1,
      1,
      2,
      1),
  MAJOR(
      "Sentiment Boost (Major)",
      "Strongly boost a stock's sentiment",
      MarketSentiment.STRONG_BULL,
      2,
      2,
      4,
      3);

  private final String displayName;
  private final String description;
  private final MarketSentiment sentiment;
  private final int minDelay;
  private final int delayRange;
  private final int minDuration;
  private final int durationRange;

  SentimentBoostTier(
      String displayName,
      String description,
      MarketSentiment sentiment,
      int minDelay,
      int delayRange,
      int minDuration,
      int durationRange) {
    this.displayName = displayName;
    this.description = description;
    this.sentiment = sentiment;
    this.minDelay = minDelay;
    this.delayRange = delayRange;
    this.minDuration = minDuration;
    this.durationRange = durationRange;
  }

  public String displayName() {
    return displayName;
  }

  public String description() {
    return description;
  }

  public MarketSentiment sentiment() {
    return sentiment;
  }

  public int minDelay() {
    return minDelay;
  }

  public int delayRange() {
    return delayRange;
  }

  public int minDuration() {
    return minDuration;
  }

  public int durationRange() {
    return durationRange;
  }
}

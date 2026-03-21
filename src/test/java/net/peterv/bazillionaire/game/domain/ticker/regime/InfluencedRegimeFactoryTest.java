package net.peterv.bazillionaire.game.domain.ticker.regime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.peterv.bazillionaire.game.domain.types.Money;
import org.junit.jupiter.api.Test;

class InfluencedRegimeFactoryTest {

  private static final Money PRICE = new Money(100_00);

  /**
   * A test factory that records which sentiment was used to create each regime. When no sentiment
   * is forced, records Optional.empty().
   */
  static class RecordingRegimeFactory implements RegimeFactory {
    final List<Optional<MarketSentiment>> sentimentsUsed = new ArrayList<>();

    @Override
    public RegimeStrategy nextRegime(Money lastPrice) {
      sentimentsUsed.add(Optional.empty());
      return new LinearRegimeStrategy(lastPrice, 1, 5, 1);
    }

    @Override
    public RegimeStrategy nextRegime(Money lastPrice, MarketSentiment forcedSentiment) {
      sentimentsUsed.add(Optional.of(forcedSentiment));
      return new LinearRegimeStrategy(lastPrice, 1, 5, 1);
    }
  }

  private static final Optional<MarketSentiment> NONE = Optional.empty();

  private static Optional<MarketSentiment> of(MarketSentiment s) {
    return Optional.of(s);
  }

  @Test
  void delegatesNormallyWithNoInfluences() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);

    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);

    assertEquals(List.of(NONE, NONE), recording.sentimentsUsed);
  }

  @Test
  void respectsDelayBeforeForcingSentiment() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);
    factory.queueInfluence(new SentimentInfluence(MarketSentiment.BULL, 2, 1));

    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);

    assertEquals(List.of(NONE, NONE, of(MarketSentiment.BULL), NONE), recording.sentimentsUsed);
  }

  @Test
  void forcesSentimentForFullDuration() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);
    factory.queueInfluence(new SentimentInfluence(MarketSentiment.BEAR, 0, 3));

    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);

    assertEquals(
        List.of(of(MarketSentiment.BEAR), of(MarketSentiment.BEAR), of(MarketSentiment.BEAR), NONE),
        recording.sentimentsUsed);
  }

  @Test
  void processesMultipleInfluencesInOrder() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);
    factory.queueInfluence(new SentimentInfluence(MarketSentiment.BULL, 0, 2));
    factory.queueInfluence(new SentimentInfluence(MarketSentiment.BEAR, 1, 1));

    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);
    factory.nextRegime(PRICE);

    assertEquals(
        List.of(
            of(MarketSentiment.BULL),
            of(MarketSentiment.BULL),
            NONE,
            of(MarketSentiment.BEAR),
            NONE),
        recording.sentimentsUsed);
  }

  @Test
  void producesValidRegimeStrategies() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);
    factory.queueInfluence(new SentimentInfluence(MarketSentiment.BULL, 0, 1));

    RegimeStrategy regime = factory.nextRegime(PRICE);
    assertNotEquals(0, regime.prices().size());
  }

  @Test
  void passesThrough_nextRegimeWithForcedSentiment() {
    var recording = new RecordingRegimeFactory();
    var factory = new InfluencedRegimeFactory(recording);

    factory.nextRegime(PRICE, MarketSentiment.FLAT);

    assertEquals(List.of(of(MarketSentiment.FLAT)), recording.sentimentsUsed);
  }
}

package net.peterv.bazillionaire.game.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.powerup.CatchUpFreezeTrigger;
import net.peterv.bazillionaire.game.domain.powerup.DividendTrigger;
import net.peterv.bazillionaire.game.domain.powerup.RandomTickTrigger;
import net.peterv.bazillionaire.game.domain.powerup.SentimentTier;
import net.peterv.bazillionaire.game.domain.powerup.SentimentTrigger;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.ticker.regime.DefaultRegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.InfluencedRegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class GameFactory {

  public static Game create(
      List<PlayerId> playerIds,
      int tickerCount,
      Money initialBalance,
      int totalDuration,
      Random random) {
    Map<PlayerId, Portfolio> players = new HashMap<>();
    for (PlayerId id : playerIds) {
      players.put(id, new Portfolio(initialBalance));
    }

    Map<Symbol, Ticker> tickers = new HashMap<>();
    Map<Symbol, Money> initialPrices = new HashMap<>();
    for (int i = 0; i < tickerCount; i++) {
      Symbol symbol;
      do {
        symbol = randomSymbol(random);
      } while (tickers.containsKey(symbol));
      MarketCap cap = MarketCap.pick(random);
      Money price = cap.initialPrice();
      initialPrices.put(symbol, price);
      tickers.put(
          symbol,
          new Ticker(
              new InfluencedRegimeFactory(new DefaultRegimeFactory(random, cap)),
              price,
              cap,
              cap.createBubbleTracker(),
              random));
    }

    Market market = new Market(tickers);
    Game game = new Game(players, market, totalDuration, new TokenBucketLiquidityLimiter());
    game.registerTrigger(new RandomTickTrigger(0.01, random));
    game.registerTrigger(new CatchUpFreezeTrigger(0.01, 15, random));
    game.registerTrigger(new DividendTrigger(20, initialPrices));
    game.registerTrigger(
        new SentimentTrigger(0.0075, random, SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR));
    game.registerTrigger(
        new SentimentTrigger(0.0075, random, SentimentTier.CRASH_MINOR, SentimentTier.CRASH_MAJOR));
    game.emit(
        GameMessage.broadcast(new GameEvent.GameCreated(market.symbols(), market.marketCaps())));
    return game;
  }

  private static Symbol randomSymbol(Random random) {
    int length = 3 + random.nextInt(2);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append((char) ('A' + random.nextInt(26)));
    }
    return new Symbol(sb.toString());
  }

  private GameFactory() {}
}

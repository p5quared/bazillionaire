package net.peterv.bazillionaire.game.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.ticker.regime.SentimentInfluence;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class Market {
  private final Map<Symbol, Ticker> tickers;

  public Market(Map<Symbol, Ticker> tickers) {
    this.tickers = new HashMap<>(tickers);
  }

  public Ticker getTicker(Symbol symbol) {
    return tickers.get(symbol);
  }

  public List<Symbol> symbols() {
    return List.copyOf(tickers.keySet());
  }

  public Map<Symbol, MarketCap> marketCaps() {
    Map<Symbol, MarketCap> caps = new HashMap<>();
    tickers.forEach((symbol, ticker) -> caps.put(symbol, ticker.marketCap()));
    return caps;
  }

  public Map<Symbol, Money> currentPrices() {
    Map<Symbol, Money> prices = new HashMap<>();
    tickers.forEach((symbol, ticker) -> prices.put(symbol, ticker.currentPrice()));
    return prices;
  }

  public Set<Symbol> delistedSymbols() {
    Set<Symbol> delisted = new HashSet<>();
    tickers.forEach(
        (symbol, ticker) -> {
          if (ticker.isDelisted()) {
            delisted.add(symbol);
          }
        });
    return delisted;
  }

  public Map<Symbol, Money> tickAll() {
    Map<Symbol, Money> prices = new HashMap<>();
    tickers.forEach(
        (symbol, ticker) -> {
          if (!ticker.isDelisted()) {
            ticker.tick();
            prices.put(symbol, ticker.currentPrice());
          }
        });
    return prices;
  }

  public void influenceSentiment(Symbol symbol, SentimentInfluence influence) {
    Ticker ticker = tickers.get(symbol);
    if (ticker != null) {
      ticker.queueSentimentInfluence(influence);
    }
  }

  public void recordTrade(Symbol symbol, int tick, int points) {
    Ticker ticker = tickers.get(symbol);
    if (ticker != null) {
      ticker.recordTrade(tick, points);
    }
  }

  public Map<Symbol, GameEvent.BubbleIndicator> bubbleIndicators() {
    Map<Symbol, GameEvent.BubbleIndicator> result = new HashMap<>();
    tickers.forEach(
        (symbol, ticker) -> {
          if (!ticker.isDelisted()) {
            result.put(
                symbol,
                new GameEvent.BubbleIndicator(ticker.bubbleFactor(), ticker.bubbleThreshold()));
          }
        });
    return result;
  }

  public List<GameMessage> evaluateBubbles(int currentTick) {
    List<GameMessage> messages = new ArrayList<>();
    tickers.forEach(
        (symbol, ticker) -> {
          if (!ticker.isDelisted()) {
            List<GameEvent> events = ticker.evaluateBubble(currentTick, symbol);
            for (GameEvent event : events) {
              messages.add(GameMessage.broadcast(event));
            }
          }
        });
    return messages;
  }
}

package net.peterv.bazillionaire.game.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public Map<Symbol, Money> currentPrices() {
    Map<Symbol, Money> prices = new HashMap<>();
    tickers.forEach((symbol, ticker) -> prices.put(symbol, ticker.currentPrice()));
    return prices;
  }

  public Map<Symbol, Money> tickAll() {
    Map<Symbol, Money> prices = new HashMap<>();
    tickers.forEach(
        (symbol, ticker) -> {
          ticker.tick();
          prices.put(symbol, ticker.currentPrice());
        });
    return prices;
  }

  public void influenceSentiment(Symbol symbol, SentimentInfluence influence) {
    Ticker ticker = tickers.get(symbol);
    if (ticker != null) {
      ticker.queueSentimentInfluence(influence);
    }
  }
}

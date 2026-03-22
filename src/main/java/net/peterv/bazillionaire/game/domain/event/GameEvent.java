package net.peterv.bazillionaire.game.domain.event;

import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public sealed interface GameEvent
    permits GameEvent.OrderFilled,
        GameEvent.TickerTicked,
        GameEvent.GameCreated,
        GameEvent.PlayerJoined,
        GameEvent.AllPlayersReady,
        GameEvent.GameState,
        GameEvent.PlayersState,
        GameEvent.GameFinished,
        GameEvent.GameTickProgressed,
        GameEvent.PowerupAwarded,
        GameEvent.FreezeStarted,
        GameEvent.FreezeExpired,
        GameEvent.PowerupActivated,
        GameEvent.DividendPaid,
        GameEvent.OrderBlocked,
        GameEvent.OrderActivity,
        GameEvent.SentimentBoostActivated,
        GameEvent.SentimentCrashActivated,
        GameEvent.BubbleWarning,
        GameEvent.TickerDelisted {
  record OrderFilled(Order order, PlayerId playerId, Money fillPrice, Money costBasis)
      implements GameEvent {}

  record TickerTicked(Symbol symbol, Money price, MarketCap marketCap) implements GameEvent {}

  record GameCreated(List<Symbol> symbols, Map<Symbol, MarketCap> marketCaps)
      implements GameEvent {}

  record PlayerJoined(PlayerId playerId) implements GameEvent {}

  record AllPlayersReady() implements GameEvent {}

  record PlayerPortfolio(Money cashBalance, Map<Symbol, Integer> holdings) {}

  record GameState(
      List<Symbol> symbols,
      Map<Symbol, Money> prices,
      Map<Symbol, MarketCap> marketCaps,
      Map<PlayerId, PlayerPortfolio> players)
      implements GameEvent {}

  record PlayersState(Map<PlayerId, PlayerPortfolio> players) implements GameEvent {}

  record GameFinished(Map<PlayerId, PlayerPortfolio> players, Map<Symbol, Money> finalPrices)
      implements GameEvent {}

  record GameTickProgressed(int tick, int ticksRemaining) implements GameEvent {}

  record PowerupAwarded(
      PlayerId recipient,
      String powerupName,
      String description,
      String usageType,
      String consumptionMode)
      implements GameEvent {}

  record FreezeStarted(PlayerId frozenPlayer, int duration) implements GameEvent {}

  record FreezeExpired(PlayerId frozenPlayer) implements GameEvent {}

  record PowerupActivated(PlayerId user, String powerupName) implements GameEvent {}

  record DividendPaid(PlayerId playerId, Symbol symbol, Money amount, String tierName)
      implements GameEvent {}

  record OrderBlocked(PlayerId playerId, Order order, String reason) implements GameEvent {}

  record OrderActivity(Symbol symbol, Money price, String side) implements GameEvent {}

  record SentimentBoostActivated(Symbol symbol, String tierName) implements GameEvent {}

  record SentimentCrashActivated(Symbol symbol, String tierName) implements GameEvent {}

  record BubbleWarning(Symbol symbol, int bubbleFactor, int threshold) implements GameEvent {}

  record TickerDelisted(Symbol symbol) implements GameEvent {}
}

package net.peterv.bazillionaire.game.adapter.in;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class GameEventSerializer {

  public ServerMessage serialize(GameEvent event) {
    return switch (event) {
      case GameEvent.TickerTicked tt ->
          new ServerMessage(
              "TICKER_TICKED",
              new TickerTickedData(tt.symbol().value(), tt.price().cents(), tt.marketCap().name()));
      case GameEvent.OrderFilled of -> {
        Order order = of.order();
        String side = order instanceof Order.Buy ? "BUY" : "SELL";
        yield new ServerMessage(
            "ORDER_FILLED",
            new OrderFilledData(
                order.symbol().value(),
                of.fillPrice().cents(),
                side,
                of.playerId().value(),
                of.costBasis().cents()));
      }
      case GameEvent.PlayerJoined pj ->
          new ServerMessage("PLAYER_JOINED", new PlayerJoinedData(pj.playerId().value()));
      case GameEvent.AllPlayersReady ignored ->
          new ServerMessage("ALL_PLAYERS_READY", new AllPlayersReadyData());
      case GameEvent.GameCreated gc -> {
        Map<String, String> caps = new LinkedHashMap<>();
        gc.marketCaps().forEach((sym, cap) -> caps.put(sym.value(), cap.name()));
        yield new ServerMessage(
            "GAME_CREATED",
            new GameCreatedData(gc.symbols().stream().map(Symbol::value).toList(), caps));
      }
      case GameEvent.PlayersState ps ->
          new ServerMessage("PLAYERS_STATE", new PlayersStateData(serializePlayers(ps.players())));
      case GameEvent.GameState gs -> {
        Map<String, Integer> prices = new LinkedHashMap<>();
        gs.prices().forEach((sym, money) -> prices.put(sym.value(), money.cents()));
        Map<String, String> caps = new LinkedHashMap<>();
        gs.marketCaps().forEach((sym, cap) -> caps.put(sym.value(), cap.name()));
        yield new ServerMessage(
            "GAME_STATE",
            new GameStateData(
                gs.symbols().stream().map(Symbol::value).toList(),
                prices,
                caps,
                serializePlayers(gs.players())));
      }
      case GameEvent.GameTickProgressed progress ->
          new ServerMessage(
              "GAME_TICK", new GameTickData(progress.tick(), progress.ticksRemaining()));
      case GameEvent.GameFinished ignored ->
          new ServerMessage("GAME_FINISHED", new GameFinishedData());
      case GameEvent.PowerupAwarded pa ->
          new ServerMessage(
              "POWERUP_AWARDED",
              new PowerupAwardedData(
                  pa.recipient().value(),
                  pa.powerupName(),
                  pa.description(),
                  pa.usageType(),
                  pa.consumptionMode()));
      case GameEvent.FreezeStarted fs ->
          new ServerMessage(
              "FREEZE_STARTED", new FreezeStartedData(fs.frozenPlayer().value(), fs.duration()));
      case GameEvent.FreezeExpired fe ->
          new ServerMessage("FREEZE_EXPIRED", new FreezeExpiredData(fe.frozenPlayer().value()));
      case GameEvent.PowerupActivated pa ->
          new ServerMessage(
              "POWERUP_ACTIVATED", new PowerupActivatedData(pa.user().value(), pa.powerupName()));
      case GameEvent.DividendPaid dp ->
          new ServerMessage(
              "DIVIDEND_PAID",
              new DividendPaidData(
                  dp.playerId().value(), dp.symbol().value(), dp.amount().cents(), dp.tierName()));
      case GameEvent.OrderActivity oa ->
          new ServerMessage(
              "ORDER_ACTIVITY",
              new OrderActivityData(oa.symbol().value(), oa.price().cents(), oa.side()));
      case GameEvent.OrderBlocked ob -> {
        Order order = ob.order();
        String side = order instanceof Order.Buy ? "BUY" : "SELL";
        yield new ServerMessage(
            "ORDER_BLOCKED",
            new OrderBlockedData(ob.playerId().value(), order.symbol().value(), side, ob.reason()));
      }
      case GameEvent.SentimentBoostActivated sba ->
          new ServerMessage(
              "SENTIMENT_BOOST_ACTIVATED",
              new SentimentBoostActivatedData(sba.symbol().value(), sba.tierName()));
      case GameEvent.SentimentCrashActivated sca ->
          new ServerMessage(
              "SENTIMENT_CRASH_ACTIVATED",
              new SentimentCrashActivatedData(sca.symbol().value(), sca.tierName()));
      case GameEvent.BubbleWarning bw ->
          new ServerMessage(
              "BUBBLE_WARNING",
              new BubbleWarningData(bw.symbol().value(), bw.bubbleFactor(), bw.threshold()));
      case GameEvent.TickerDelisted td ->
          new ServerMessage("TICKER_DELISTED", new TickerDelistedData(td.symbol().value()));
    };
  }

  Map<String, PlayerSnapshotData> serializePlayers(
      Map<PlayerId, GameEvent.PlayerPortfolio> players) {
    Map<String, PlayerSnapshotData> result = new LinkedHashMap<>();
    players.forEach(
        (pid, portfolio) -> {
          Map<String, Integer> holdings = new LinkedHashMap<>();
          portfolio.holdings().forEach((sym, qty) -> holdings.put(sym.value(), qty));
          result.put(
              pid.value(), new PlayerSnapshotData(portfolio.cashBalance().cents(), holdings));
        });
    return result;
  }

  // Wire format records
  record ServerMessage(String type, Object data) {}

  record TickerTickedData(String symbol, int price, String marketCap) {}

  record OrderFilledData(String symbol, int price, String side, String playerId, int costBasis) {}

  record PlayerJoinedData(String playerId) {}

  record AllPlayersReadyData() {}

  record GameCreatedData(List<String> symbols, Map<String, String> marketCaps) {}

  record PlayerSnapshotData(int cashBalance, Map<String, Integer> holdings) {}

  record PlayersStateData(Map<String, PlayerSnapshotData> players) {}

  record GameStateData(
      List<String> symbols,
      Map<String, Integer> prices,
      Map<String, String> marketCaps,
      Map<String, PlayerSnapshotData> players) {}

  record GameFinishedData() {}

  record GameTickData(int tick, int ticksRemaining) {}

  record PowerupAwardedData(
      String recipient,
      String powerupName,
      String description,
      String usageType,
      String consumptionMode) {}

  record FreezeStartedData(String frozenPlayer, int duration) {}

  record FreezeExpiredData(String frozenPlayer) {}

  record PowerupActivatedData(String user, String powerupName) {}

  record DividendPaidData(String playerId, String symbol, int amount, String tierName) {}

  record OrderBlockedData(String playerId, String symbol, String side, String reason) {}

  record OrderActivityData(String symbol, int price, String side) {}

  record SentimentBoostActivatedData(String symbol, String tierName) {}

  record SentimentCrashActivatedData(String symbol, String tierName) {}

  record BubbleWarningData(String symbol, int bubbleFactor, int threshold) {}

  record TickerDelistedData(String symbol) {}
}

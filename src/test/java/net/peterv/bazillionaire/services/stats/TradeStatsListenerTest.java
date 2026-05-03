package net.peterv.bazillionaire.services.stats;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.GameId;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeStatsListenerTest {

  private TradeStatsListener listener;
  private List<RecordedCall> recordedCalls;

  record RecordedCall(
      String username,
      String gameId,
      int totalBuys,
      int totalSells,
      long totalFillsCents,
      int totalBlockedOrders) {}

  @BeforeEach
  void setUp() {
    recordedCalls = new ArrayList<>();
    listener = new TradeStatsListener();
    listener.tradeStatsService =
        new PlayerTradeStatsService() {
          @Override
          public void recordTrades(
              String username,
              String gameId,
              int totalBuys,
              int totalSells,
              long totalFillsCents,
              int totalBlockedOrders) {
            recordedCalls.add(
                new RecordedCall(
                    username, gameId, totalBuys, totalSells, totalFillsCents, totalBlockedOrders));
          }
        };
  }

  @Test
  void accumulatesBuysAndSellsSeparately() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var aapl = new Symbol("AAPL");

    fireOrderFilled(gameId, alice, new Order.Buy(aapl), 100_00);
    fireOrderFilled(gameId, alice, new Order.Buy(aapl), 110_00);
    fireOrderFilled(gameId, alice, new Order.Sell(aapl), 120_00);
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.size());
    var call = recordedCalls.get(0);
    assertEquals(2, call.totalBuys());
    assertEquals(1, call.totalSells());
  }

  @Test
  void accumulatesFillsCentsAcrossOrders() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var aapl = new Symbol("AAPL");

    fireOrderFilled(gameId, alice, new Order.Buy(aapl), 100_00);
    fireOrderFilled(gameId, alice, new Order.Sell(aapl), 200_00);
    fireGameFinished(gameId);

    assertEquals(300_00, recordedCalls.get(0).totalFillsCents());
  }

  @Test
  void tracksBlockedOrders() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var aapl = new Symbol("AAPL");

    fireOrderBlocked(gameId, alice, new Order.Buy(aapl));
    fireOrderBlocked(gameId, alice, new Order.Sell(aapl));
    fireGameFinished(gameId);

    assertEquals(1, recordedCalls.size());
    assertEquals(2, recordedCalls.get(0).totalBlockedOrders());
  }

  @Test
  void tracksMultiplePlayersIndependently() {
    var gameId = new GameId("game1");
    var alice = new PlayerId("alice");
    var bob = new PlayerId("bob");
    var aapl = new Symbol("AAPL");

    fireOrderFilled(gameId, alice, new Order.Buy(aapl), 100_00);
    fireOrderFilled(gameId, bob, new Order.Buy(aapl), 200_00);
    fireOrderFilled(gameId, bob, new Order.Sell(aapl), 150_00);
    fireGameFinished(gameId);

    assertEquals(2, recordedCalls.size());
    var aliceCall =
        recordedCalls.stream().filter(c -> c.username().equals("alice")).findFirst().get();
    assertEquals(1, aliceCall.totalBuys());
    assertEquals(0, aliceCall.totalSells());

    var bobCall = recordedCalls.stream().filter(c -> c.username().equals("bob")).findFirst().get();
    assertEquals(1, bobCall.totalBuys());
    assertEquals(1, bobCall.totalSells());
  }

  @Test
  void cleansUpGameEntryAfterPersist() {
    var gameId = new GameId("game1");
    fireOrderFilled(gameId, new PlayerId("alice"), new Order.Buy(new Symbol("AAPL")), 100_00);
    fireGameFinished(gameId);

    recordedCalls.clear();
    fireGameFinished(gameId);
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void noTradesProducesNoRecords() {
    fireGameFinished(new GameId("game1"));
    assertTrue(recordedCalls.isEmpty());
  }

  @Test
  void tracksMultipleGamesIndependently() {
    var game1 = new GameId("game1");
    var game2 = new GameId("game2");
    var alice = new PlayerId("alice");
    var aapl = new Symbol("AAPL");

    fireOrderFilled(game1, alice, new Order.Buy(aapl), 100_00);
    fireOrderFilled(game2, alice, new Order.Buy(aapl), 200_00);
    fireOrderFilled(game2, alice, new Order.Buy(aapl), 300_00);
    fireGameFinished(game1);

    assertEquals(1, recordedCalls.size());
    assertEquals(100_00, recordedCalls.get(0).totalFillsCents());

    recordedCalls.clear();
    fireGameFinished(game2);

    assertEquals(1, recordedCalls.size());
    assertEquals(500_00, recordedCalls.get(0).totalFillsCents());
  }

  private void fireOrderFilled(GameId gameId, PlayerId playerId, Order order, int fillPriceCents) {
    var event = new GameEvent.OrderFilled(order, playerId, new Money(fillPriceCents), new Money(0));
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void fireOrderBlocked(GameId gameId, PlayerId playerId, Order order) {
    var event = new GameEvent.OrderBlocked(playerId, order, "frozen");
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }

  private void fireGameFinished(GameId gameId) {
    var players = new LinkedHashMap<PlayerId, GameEvent.PlayerPortfolio>();
    var event = new GameEvent.GameFinished(players, Map.of(), new Money(0));
    var message = new GameMessage(event, new Audience.Everyone());
    listener.onGameEvents(gameId, List.of(message));
  }
}

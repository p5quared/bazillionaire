package net.peterv.bazillionaire.game.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.adapter.out.InMemoryGameRepository;
import net.peterv.bazillionaire.game.domain.JoinResult;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;
import net.peterv.bazillionaire.game.port.in.JoinGameCommand;
import net.peterv.bazillionaire.game.port.in.JoinGameUseCase;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand.OrderSide;
import net.peterv.bazillionaire.game.port.in.PlaceOrderUseCase;
import net.peterv.bazillionaire.game.port.in.StartGameCommand;
import net.peterv.bazillionaire.game.port.in.StartGameUseCase;
import net.peterv.bazillionaire.game.port.in.TickCommand;
import net.peterv.bazillionaire.game.port.in.TickProgress;
import net.peterv.bazillionaire.game.port.in.TickUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.in.UsePowerupCommand;
import net.peterv.bazillionaire.game.port.in.UsePowerupUseCase;
import net.peterv.bazillionaire.game.service.CreateGameService;
import net.peterv.bazillionaire.game.service.JoinGameService;
import net.peterv.bazillionaire.game.service.PlaceOrderService;
import net.peterv.bazillionaire.game.service.StartGameService;
import net.peterv.bazillionaire.game.service.TickService;
import net.peterv.bazillionaire.game.service.UsePowerupService;

public class GameTestHarness {

  private static final String DEFAULT_GAME_ID = "test-game";
  private static final int DEFAULT_TICKER_COUNT = 3;
  private static final Money DEFAULT_INITIAL_BALANCE = new Money(1_000_00);
  private static final int DEFAULT_DURATION = 1200;
  private static final long DEFAULT_SEED = 42L;

  private final CreateGameUseCase createGame;
  private final JoinGameUseCase joinGame;
  private final StartGameUseCase startGame;
  private final PlaceOrderUseCase placeOrder;
  private final TickUseCase tick;
  private final UsePowerupUseCase usePowerup;

  private final String gameId;
  private final List<String> playerIds;
  private final List<GameMessage> allMessages = new ArrayList<>();
  private final List<String> symbols;

  private GameTestHarness(
      String gameId,
      List<String> playerIds,
      int tickerCount,
      Money initialBalance,
      int duration,
      long seed) {
    InMemoryGameRepository repository = new InMemoryGameRepository();
    this.createGame = new CreateGameService(repository);
    this.joinGame = new JoinGameService(repository);
    this.startGame = new StartGameService(repository);
    this.placeOrder = new PlaceOrderService(repository);
    this.tick = new TickService(repository);
    this.usePowerup = new UsePowerupService(repository);

    this.gameId = gameId;
    this.playerIds = List.copyOf(playerIds);

    UseCaseResult<Void> createResult =
        createGame.createGame(
            new CreateGameCommand(
                gameId, playerIds, tickerCount, initialBalance, duration, new Random(seed)));
    allMessages.addAll(createResult.messages());

    this.symbols =
        allMessages.stream()
            .map(GameMessage::event)
            .filter(GameEvent.GameCreated.class::isInstance)
            .map(GameEvent.GameCreated.class::cast)
            .findFirst()
            .map(e -> e.symbols().stream().map(Symbol::value).toList())
            .orElse(List.of());
  }

  // --- Lifecycle ---

  public UseCaseResult<JoinResult> join(String playerId) {
    UseCaseResult<JoinResult> result = joinGame.join(new JoinGameCommand(gameId, playerId));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<Void> start() {
    UseCaseResult<Void> result = startGame.startGame(new StartGameCommand(gameId));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<TickProgress> tick() {
    UseCaseResult<TickProgress> result = tick.tick(new TickCommand(gameId));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<TickProgress> tickN(int n) {
    UseCaseResult<TickProgress> last = null;
    for (int i = 0; i < n; i++) {
      last = tick();
    }
    return last;
  }

  // --- Player Actions ---

  public UseCaseResult<OrderResult> buy(String playerId, String symbol) {
    UseCaseResult<OrderResult> result =
        placeOrder.placeOrder(new PlaceOrderCommand(gameId, playerId, symbol, OrderSide.BUY));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<OrderResult> sell(String playerId, String symbol) {
    UseCaseResult<OrderResult> result =
        placeOrder.placeOrder(new PlaceOrderCommand(gameId, playerId, symbol, OrderSide.SELL));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<UsePowerupResult> usePowerup(
      String playerId, String powerupName, String targetPlayerId) {
    UseCaseResult<UsePowerupResult> result =
        usePowerup.usePowerup(new UsePowerupCommand(gameId, playerId, powerupName, targetPlayerId));
    allMessages.addAll(result.messages());
    return result;
  }

  public UseCaseResult<UsePowerupResult> usePowerup(
      String playerId,
      String powerupName,
      String targetPlayerId,
      String targetSymbol,
      int quantity) {
    UseCaseResult<UsePowerupResult> result =
        usePowerup.usePowerup(
            new UsePowerupCommand(
                gameId, playerId, powerupName, targetPlayerId, targetSymbol, quantity));
    allMessages.addAll(result.messages());
    return result;
  }

  // --- Composite Setup ---

  public GameTestHarness joinAll() {
    for (String playerId : playerIds) {
      join(playerId);
    }
    return this;
  }

  public GameTestHarness joinAllAndStart() {
    joinAll();
    start();
    return this;
  }

  // --- Drive-to-State Helpers ---

  public OrderResult buyUntilRejected(String playerId, String symbol) {
    for (int i = 0; i < 10_000; i++) {
      UseCaseResult<OrderResult> result = buy(playerId, symbol);
      if (!(result.result() instanceof OrderResult.Filled)) {
        return result.result();
      }
    }
    throw new AssertionError("Order was never rejected after 10,000 attempts");
  }

  public void tickUntilEvent(Class<? extends GameEvent> eventType, int maxTicks) {
    int checkpoint = allMessages.size();
    for (int i = 0; i < maxTicks; i++) {
      tick();
      boolean found =
          allMessages.subList(checkpoint, allMessages.size()).stream()
              .map(GameMessage::event)
              .anyMatch(eventType::isInstance);
      if (found) {
        return;
      }
    }
    throw new AssertionError(
        eventType.getSimpleName() + " was not emitted within " + maxTicks + " ticks");
  }

  public void tickUntilFinished(int maxTicks) {
    tickUntilEvent(GameEvent.GameFinished.class, maxTicks);
  }

  // --- Observation ---

  public List<GameMessage> messages() {
    return Collections.unmodifiableList(allMessages);
  }

  public List<GameMessage> messagesSince(int checkpoint) {
    return Collections.unmodifiableList(allMessages.subList(checkpoint, allMessages.size()));
  }

  public int messageCheckpoint() {
    return allMessages.size();
  }

  public <T extends GameEvent> List<T> eventsOfType(Class<T> type) {
    return allMessages.stream()
        .map(GameMessage::event)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public <T extends GameEvent> List<T> eventsOfTypeSince(Class<T> type, int checkpoint) {
    return allMessages.subList(checkpoint, allMessages.size()).stream()
        .map(GameMessage::event)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public <T extends GameEvent> List<T> broadcastEventsOfType(Class<T> type) {
    return allMessages.stream()
        .filter(m -> m.audience() instanceof Audience.Everyone)
        .map(GameMessage::event)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public <T extends GameEvent> List<T> privateEventsFor(String playerId, Class<T> type) {
    PlayerId pid = new PlayerId(playerId);
    return allMessages.stream()
        .filter(m -> m.audience() instanceof Audience.Only only && only.playerId().equals(pid))
        .map(GameMessage::event)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public List<String> symbols() {
    return symbols;
  }

  public List<String> playerIds() {
    return playerIds;
  }

  public String gameId() {
    return gameId;
  }

  // --- Builder ---

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String gameId = DEFAULT_GAME_ID;
    private List<String> playerIds = List.of("player1", "player2");
    private int tickerCount = DEFAULT_TICKER_COUNT;
    private Money initialBalance = DEFAULT_INITIAL_BALANCE;
    private int duration = DEFAULT_DURATION;
    private long seed = DEFAULT_SEED;

    public Builder gameId(String gameId) {
      this.gameId = gameId;
      return this;
    }

    public Builder players(String... playerIds) {
      this.playerIds = List.of(playerIds);
      return this;
    }

    public Builder tickerCount(int tickerCount) {
      this.tickerCount = tickerCount;
      return this;
    }

    public Builder initialBalance(Money initialBalance) {
      this.initialBalance = initialBalance;
      return this;
    }

    public Builder duration(int duration) {
      this.duration = duration;
      return this;
    }

    public Builder seed(long seed) {
      this.seed = seed;
      return this;
    }

    public GameTestHarness build() {
      return new GameTestHarness(gameId, playerIds, tickerCount, initialBalance, duration, seed);
    }
  }
}

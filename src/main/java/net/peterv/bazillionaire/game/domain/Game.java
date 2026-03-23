package net.peterv.bazillionaire.game.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.peterv.bazillionaire.game.domain.event.GameEvent;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderProcessingResult;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.CatchUpFreezeTrigger;
import net.peterv.bazillionaire.game.domain.powerup.DividendTrigger;
import net.peterv.bazillionaire.game.domain.powerup.GameContext;
import net.peterv.bazillionaire.game.domain.powerup.Powerup;
import net.peterv.bazillionaire.game.domain.powerup.PowerupEffect;
import net.peterv.bazillionaire.game.domain.powerup.PowerupManager;
import net.peterv.bazillionaire.game.domain.powerup.PowerupTrigger;
import net.peterv.bazillionaire.game.domain.powerup.RandomTickTrigger;
import net.peterv.bazillionaire.game.domain.powerup.SentimentTier;
import net.peterv.bazillionaire.game.domain.powerup.SentimentTrigger;
import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;
import net.peterv.bazillionaire.game.domain.ticker.MarketCap;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.ticker.regime.DefaultRegimeFactory;
import net.peterv.bazillionaire.game.domain.ticker.regime.InfluencedRegimeFactory;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;

public class Game {
  private final Map<PlayerId, Portfolio> players;
  private final Market market;
  private final List<GameMessage> pendingMessages = new ArrayList<>();
  private final PowerupManager powerupManager = new PowerupManager();
  private final LiquidityProvider liquidityProvider;
  private final OrderProcessor orderProcessor;
  private final Set<PlayerId> readyPlayers = new HashSet<>();
  private final int totalDuration;
  private int tickCount = 0;
  private GameStatus status = GameStatus.PENDING;

  public static Game create(
      List<PlayerId> playerIds,
      int tickerCount,
      Money initialBalance,
      Money initialPrice,
      int totalDuration,
      Random random) {
    Map<PlayerId, Portfolio> players = new HashMap<>();
    for (PlayerId id : playerIds) {
      players.put(id, new Portfolio(initialBalance));
    }

    Map<Symbol, Ticker> tickers = new HashMap<>();
    for (int i = 0; i < tickerCount; i++) {
      Symbol symbol;
      do {
        symbol = randomSymbol(random);
      } while (tickers.containsKey(symbol));
      MarketCap cap = MarketCap.pick(random);
      tickers.put(
          symbol,
          new Ticker(
              new InfluencedRegimeFactory(new DefaultRegimeFactory(random, cap)),
              initialPrice,
              cap,
              cap.createBubbleTracker(),
              random));
    }

    Market market = new Market(tickers);
    Game game = new Game(players, market, totalDuration, new TokenBucketLiquidityLimiter());
    game.registerTrigger(new RandomTickTrigger(0.01, random));
    game.registerTrigger(new CatchUpFreezeTrigger(0.02, 45, random));
    game.registerTrigger(new DividendTrigger(20, initialPrice));
    game.registerTrigger(
        new SentimentTrigger(0.08, random, SentimentTier.BOOST_MINOR, SentimentTier.BOOST_MAJOR));
    game.registerTrigger(
        new SentimentTrigger(0.08, random, SentimentTier.CRASH_MINOR, SentimentTier.CRASH_MAJOR));
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

  public Game(Map<PlayerId, Portfolio> players, Map<Symbol, Ticker> tickers, int totalDuration) {
    this(players, new Market(tickers), totalDuration, new TokenBucketLiquidityLimiter());
  }

  public Game(
      Map<PlayerId, Portfolio> players,
      Map<Symbol, Ticker> tickers,
      int totalDuration,
      LiquidityProvider liquidityProvider) {
    this(players, new Market(tickers), totalDuration, liquidityProvider);
  }

  private Game(
      Map<PlayerId, Portfolio> players,
      Market market,
      int totalDuration,
      LiquidityProvider liquidityProvider) {
    this.players = new HashMap<>(players);
    this.market = market;
    this.totalDuration = totalDuration;
    this.liquidityProvider = liquidityProvider;
    this.orderProcessor = new OrderProcessor(powerupManager, liquidityProvider);
  }

  public OrderResult placeOrder(Order order, PlayerId playerId) {
    Portfolio player = players.get(playerId);
    if (player == null) {
      return new OrderResult.InvalidOrder("Unknown player: " + playerId.value());
    }

    OrderProcessingResult result =
        orderProcessor.process(order, playerId, player, market, currentTick());
    result.messages().forEach(this::emit);
    if (result.orderResult() instanceof OrderResult.Filled) {
      emit(GameMessage.send(new GameEvent.PlayersState(playerPortfolios()), playerId));
    }
    return result.orderResult();
  }

  public void tick() {
    if (this.status == GameStatus.READY) {
      market
          .tickAll()
          .forEach(
              (symbol, price) ->
                  emit(
                      GameMessage.broadcast(
                          new GameEvent.TickerTicked(
                              symbol, price, market.getTicker(symbol).marketCap()))));
      market.evaluateBubbles(currentTick()).forEach(this::emit);
      applyEffects(powerupManager.tick(snapshot()));
      liquidityProvider.onTick(currentTick());
      tickCount++;
      emit(
          GameMessage.broadcast(new GameEvent.GameTickProgressed(currentTick(), ticksRemaining())));
      if (tickCount >= totalDuration) {
        status = GameStatus.FINISHED;
        emit(
            GameMessage.broadcast(new GameEvent.GameFinished(playerPortfolios(), currentPrices())));
      }
    }
  }

  public int currentTick() {
    return tickCount;
  }

  public int ticksRemaining() {
    return Math.max(totalDuration - tickCount, 0);
  }

  public List<GameMessage> drainMessages() {
    List<GameMessage> messages = List.copyOf(pendingMessages);
    pendingMessages.clear();
    return messages;
  }

  public JoinResult join(PlayerId playerId) {
    if (!players.containsKey(playerId)) {
      return new JoinResult.InvalidJoin("Unknown player: " + playerId.value());
    }

    if (status == GameStatus.READY) {
      emit(
          GameMessage.send(
              new GameEvent.GameState(
                  market.symbols(), currentPrices(), market.marketCaps(), playerPortfolios()),
              playerId));
      return new JoinResult.GameInProgress();
    }

    if (readyPlayers.contains(playerId)) {
      return new JoinResult.AlreadyReady();
    }

    readyPlayers.add(playerId);
    emit(GameMessage.broadcast(new GameEvent.PlayerJoined(playerId)));

    if (readyPlayers.size() == players.size()) {
      emit(GameMessage.broadcast(new GameEvent.AllPlayersReady()));
      return new JoinResult.AllReady();
    }

    return new JoinResult.Joined();
  }

  public void start() {
    status = GameStatus.READY;
    emit(
        GameMessage.broadcast(
            new GameEvent.GameState(
                market.symbols(), currentPrices(), market.marketCaps(), playerPortfolios())));
    emit(GameMessage.broadcast(new GameEvent.PlayersState(playerPortfolios())));
  }

  private Map<PlayerId, GameEvent.PlayerPortfolio> playerPortfolios() {
    Map<PlayerId, GameEvent.PlayerPortfolio> result = new HashMap<>();
    players.forEach(
        (id, p) ->
            result.put(
                id, new GameEvent.PlayerPortfolio(p.cashBalance(), Map.copyOf(p.holdings()))));
    return result;
  }

  public Map<Symbol, Money> currentPrices() {
    return market.currentPrices();
  }

  void addCashToPlayer(PlayerId playerId, Money amount) {
    Portfolio portfolio = players.get(playerId);
    if (portfolio != null) {
      portfolio.addCash(amount);
      emit(GameMessage.broadcast(new GameEvent.PlayersState(playerPortfolios())));
    }
  }

  public void registerTrigger(PowerupTrigger trigger) {
    powerupManager.registerTrigger(trigger);
  }

  public void activatePowerup(Powerup powerup) {
    applyEffects(powerupManager.activate(powerup));
  }

  public UsePowerupResult usePowerup(PlayerId playerId, String powerupName, PlayerId target) {
    return usePowerup(playerId, powerupName, 1, target);
  }

  public UsePowerupResult usePowerup(
      PlayerId playerId, String powerupName, int quantity, PlayerId target) {
    UsePowerupResult result = powerupManager.usePowerup(playerId, powerupName, quantity, target);
    if (result instanceof UsePowerupResult.Activated activated) {
      applyEffects(activated.effects());
    }
    return result;
  }

  public UsePowerupResult usePowerup(PlayerId playerId, String powerupName, Symbol targetSymbol) {
    UsePowerupResult result =
        powerupManager.usePowerup(playerId, powerupName, 1, null, targetSymbol);
    if (result instanceof UsePowerupResult.Activated activated) {
      applyEffects(activated.effects());
    }
    return result;
  }

  public List<Powerup> getInventory(PlayerId playerId) {
    return powerupManager.getInventory(playerId);
  }

  void emit(GameMessage message) {
    pendingMessages.add(message);
  }

  public GameContext snapshot() {
    List<GameEvent> recentEvents = pendingMessages.stream().map(GameMessage::event).toList();
    return new GameContext(
        currentTick(), playerPortfolios(), currentPrices(), recentEvents, market.delistedSymbols());
  }

  private void applyEffects(List<PowerupEffect> effects) {
    for (PowerupEffect effect : effects) {
      switch (effect) {
        case PowerupEffect.Emit e -> emit(e.message());
        case PowerupEffect.AddCash ac -> addCashToPlayer(ac.player(), ac.amount());
        case PowerupEffect.InfluenceSentiment is ->
            market.influenceSentiment(is.symbol(), is.influence());
      }
    }
  }
}

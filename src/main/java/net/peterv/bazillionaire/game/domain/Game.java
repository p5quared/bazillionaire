package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.powerup.CatchUpFreezeTrigger;
import net.peterv.bazillionaire.game.domain.powerup.GameContext;
import net.peterv.bazillionaire.game.domain.powerup.PowerupTrigger;
import net.peterv.bazillionaire.game.domain.powerup.RandomTickTrigger;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.Powerup;
import net.peterv.bazillionaire.game.domain.powerup.PowerupManager;
import net.peterv.bazillionaire.game.domain.ticker.Ticker;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.domain.types.Symbol;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Game {
	private final Map<PlayerId, Portfolio> players;
	private final Map<Symbol, Ticker> tickers;
	private final List<GameMessage> pendingMessages = new ArrayList<>();
	private final PowerupManager powerupManager = new PowerupManager();
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
			tickers.put(symbol, new Ticker(initialPrice, random));
		}

		Game game = new Game(players, tickers, totalDuration);
		game.registerTrigger(new RandomTickTrigger(0.05, new Money(500_00), random));
		game.registerTrigger(new CatchUpFreezeTrigger(0.02, 3, random));
		game.emit(GameMessage.broadcast(
				new GameEvent.GameCreated(List.copyOf(tickers.keySet()))));
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
		this.players = new HashMap<>(players);
		this.tickers = new HashMap<>(tickers);
		this.totalDuration = totalDuration;
	}

	public OrderResult placeOrder(Order order, PlayerId playerId) {
		Portfolio player = players.get(playerId);
		if (player == null) {
			return new OrderResult.InvalidOrder("Unknown player: " + playerId.value());
		}

		Ticker ticker = tickers.get(order.symbol());
		if (ticker == null) {
			return new OrderResult.InvalidOrder("Unknown symbol: " + order.symbol().value());
		}

		OrderResult intercepted = powerupManager.checkInterceptors(order, playerId, ticker);
		if (intercepted != null) {
			return intercepted;
		}

		if (!ticker.canFill(order)) {
			return new OrderResult.Rejected("Ticker cannot fill order");
		}

		OrderResult result = player.fill(order);

		return switch (result) {
			case OrderResult.Rejected r -> r;
			case OrderResult.InvalidOrder i -> i;
			case OrderResult.Filled f -> {
				emit(GameMessage.broadcast(
						new GameEvent.OrderFilled(order, playerId)));
				emit(GameMessage.broadcast(
						new GameEvent.PlayersState(playerPortfolios())));
				yield f;
			}
		};
	}

	public void tick() {
		if (this.status == GameStatus.READY) {
			tickers.forEach((symbol, ticker) -> {
				ticker.tick();
				emit(GameMessage.broadcast(
						new GameEvent.TickerTicked(symbol, ticker.currentPrice())));
			});
			powerupManager.tick(this);
			tickCount++;
			emit(GameMessage.broadcast(new GameEvent.GameTickProgressed(currentTick(), ticksRemaining())));
			if (tickCount >= totalDuration) {
				status = GameStatus.FINISHED;
				emit(GameMessage.broadcast(new GameEvent.GameFinished()));
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
			emit(GameMessage.send(
					new GameEvent.GameState(List.copyOf(tickers.keySet()), currentPrices(), playerPortfolios()),
					playerId.value()));
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
		emit(GameMessage.broadcast(new GameEvent.PlayersState(playerPortfolios())));
	}

	private Map<PlayerId, GameEvent.PlayerPortfolio> playerPortfolios() {
		Map<PlayerId, GameEvent.PlayerPortfolio> result = new HashMap<>();
		players
				.forEach((id, p) -> result.put(id, new GameEvent.PlayerPortfolio(p.cashBalance(), Map.copyOf(p.holdings()))));
		return result;
	}

	public Map<Symbol, Money> currentPrices() {
		Map<Symbol, Money> prices = new HashMap<>();
		tickers.forEach((symbol, ticker) -> prices.put(symbol, ticker.currentPrice()));
		return prices;
	}

	public void addCashToPlayer(PlayerId playerId, Money amount) {
		Portfolio portfolio = players.get(playerId);
		if (portfolio != null) {
			portfolio.addCash(amount);
		}
	}

	public void registerTrigger(PowerupTrigger trigger) {
		powerupManager.registerTrigger(trigger);
	}

	public void activatePowerup(Powerup powerup) {
		powerupManager.activate(powerup, this);
	}

	public void emit(GameMessage message) {
		pendingMessages.add(message);
	}

	public GameContext snapshot() {
		List<GameEvent> recentEvents = pendingMessages.stream()
				.map(GameMessage::event)
				.toList();
		return new GameContext(currentTick(), playerPortfolios(), currentPrices(), recentEvents);
	}
}

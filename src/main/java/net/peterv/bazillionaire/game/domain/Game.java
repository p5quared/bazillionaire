package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
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
	private final Set<PlayerId> readyPlayers = new HashSet<>();
	private GameStatus status = GameStatus.PENDING;

	public static Game create(
			List<PlayerId> playerIds,
			int tickerCount,
			Money initialBalance,
			Money initialPrice,
			int totalDuration,
			int strategyDuration,
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
			tickers.put(symbol, new Ticker(initialPrice, totalDuration, strategyDuration, random));
		}

		Game game = new Game(players, tickers);
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

	public Game(Map<PlayerId, Portfolio> players, Map<Symbol, Ticker> tickers) {
		this.players = new HashMap<>(players);
		this.tickers = new HashMap<>(tickers);
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
		}
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
					new GameEvent.GameState(List.copyOf(tickers.keySet()), currentPrices()),
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
	}

	public Map<Symbol, Money> currentPrices() {
		Map<Symbol, Money> prices = new HashMap<>();
		tickers.forEach((symbol, ticker) -> prices.put(symbol, ticker.currentPrice()));
		return prices;
	}

	private void emit(GameMessage message) {
		pendingMessages.add(message);
	}
}

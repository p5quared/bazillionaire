package net.peterv.bazillionaire.game.adapter.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.game.domain.JoinResult;
import net.peterv.bazillionaire.game.domain.order.Order;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.in.JoinGameCommand;
import net.peterv.bazillionaire.game.port.in.JoinGameUseCase;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand;
import net.peterv.bazillionaire.game.port.in.PlaceOrderUseCase;
import net.peterv.bazillionaire.game.port.in.StartGameCommand;
import net.peterv.bazillionaire.game.port.in.StartGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebSocket(path = "/game/{gameId}")
public class StockGameWebSocketAdapter {

	@Inject
	JoinGameUseCase joinGameUseCase;

	@Inject
	PlaceOrderUseCase placeOrderUseCase;

	@Inject
	StartGameUseCase startGameUseCase;

	@Inject
	GameSessionRegistry registry;

	@Inject
	ObjectMapper objectMapper;

	@OnOpen
	@Blocking
	public void onOpen(WebSocketConnection connection) {
		String gameId = connection.pathParam("gameId");
		registry.register(connection.id(), connection, gameId);
	}

	@OnClose
	public void onClose(WebSocketConnection connection) {
		registry.deregister(connection.id());
	}

	@OnTextMessage
	@Blocking
	public void onMessage(String rawMessage, WebSocketConnection connection) {
		String gameId = connection.pathParam("gameId");
		try {
			ClientMessage msg = objectMapper.readValue(rawMessage, ClientMessage.class);
			switch (msg.type()) {
				case "JOIN" -> handleJoin(connection, gameId, msg.payload());
				case "BUY" -> handleOrder(connection, gameId, msg.payload(), PlaceOrderCommand.OrderSide.BUY);
				case "SELL" -> handleOrder(connection, gameId, msg.payload(), PlaceOrderCommand.OrderSide.SELL);
				default -> sendError(connection, "UNKNOWN_TYPE", "Unknown message type: " + msg.type());
			}
		} catch (Exception e) {
			sendError(connection, "PARSE_ERROR", e.getMessage());
		}
	}

	private void handleJoin(WebSocketConnection connection, String gameId, Map<String, Object> payload) {
		String playerIdStr = (String) payload.get("playerId");
		UseCaseResult<JoinResult> result = joinGameUseCase.join(new JoinGameCommand(gameId, playerIdStr));
		switch (result.result()) {
			case JoinResult.AlreadyReady ignored -> sendError(connection, "ALREADY_JOINED", "Already joined");
			case JoinResult.InvalidJoin inv -> sendError(connection, "INVALID_JOIN", inv.reason());
			default -> {
				registry.associatePlayer(connection.id(), new PlayerId(playerIdStr));
				sendMessage(connection, new ServerMessage("JOINED", new JoinedData(playerIdStr)));
			}
		}
		dispatchMessages(gameId, result.messages());
		if (result.result() instanceof JoinResult.AllReady) {
			UseCaseResult<Void> startResult = startGameUseCase.startGame(new StartGameCommand(gameId));
			dispatchMessages(gameId, startResult.messages());
		}
	}

	private void handleOrder(WebSocketConnection connection, String gameId, Map<String, Object> payload,
			PlaceOrderCommand.OrderSide side) {
		String ticker = (String) payload.get("ticker");
		int price = ((Number) payload.get("price")).intValue();
		String playerIdStr = registry.findPlayer(connection.id())
				.map(PlayerId::value)
				.orElse(null);
		if (playerIdStr == null) {
			sendError(connection, "NOT_JOINED", "Must join game before placing orders");
			return;
		}
		UseCaseResult<OrderResult> result = placeOrderUseCase.placeOrder(
				new PlaceOrderCommand(gameId, playerIdStr, ticker, side, price));
		switch (result.result()) {
			case OrderResult.Filled ignored -> {
				String sideStr = side == PlaceOrderCommand.OrderSide.BUY ? "BUY" : "SELL";
				sendMessage(connection, new ServerMessage("ORDER_FILLED",
						new OrderFilledResponseData(ticker, price, sideStr)));
			}
			case OrderResult.Rejected rej -> sendError(connection, "ORDER_REJECTED", rej.reason());
			case OrderResult.InvalidOrder inv -> sendError(connection, "INVALID_ORDER", inv.reason());
		}
		dispatchMessages(gameId, result.messages());
	}

	public void dispatchMessages(String gameId, List<GameMessage> messages) {
		for (GameMessage message : messages) {
			ServerMessage serverMessage = toServerMessage(message.event());
			String json;
			try {
				json = objectMapper.writeValueAsString(serverMessage);
			} catch (Exception e) {
				continue;
			}
			switch (message.audience()) {
				case Audience.Everyone ignored ->
					registry.connectionsForGame(gameId).forEach(conn -> conn.sendTextAndAwait(json));
				case Audience.Only only -> {
					registry.connectionForPlayer(gameId, new PlayerId(only.playerId()))
							.ifPresent(conn -> conn.sendTextAndAwait(json));
				}
			}
		}
	}

	private ServerMessage toServerMessage(GameEvent event) {
		return switch (event) {
			case GameEvent.TickerTicked tt -> new ServerMessage("TICKER_TICKED",
					new TickerTickedData(tt.symbol().value(), tt.price().cents()));
			case GameEvent.OrderFilled of -> {
				Order order = of.order();
				String side = order instanceof Order.Buy ? "BUY" : "SELL";
				yield new ServerMessage("ORDER_FILLED",
						new OrderFilledData(order.symbol().value(), order.price().cents(), side, of.playerId().value()));
			}
			case GameEvent.PlayerJoined pj -> new ServerMessage("PLAYER_JOINED",
					new PlayerJoinedData(pj.playerId().value()));
			case GameEvent.AllPlayersReady ignored -> new ServerMessage("ALL_PLAYERS_READY",
					new AllPlayersReadyData());
			case GameEvent.GameCreated gc -> new ServerMessage("GAME_CREATED",
					new GameCreatedData(gc.symbols().stream().map(s -> s.value()).toList()));
			case GameEvent.GameState gs -> {
				Map<String, Integer> prices = new LinkedHashMap<>();
				gs.prices().forEach((sym, money) -> prices.put(sym.value(), money.cents()));
				yield new ServerMessage("GAME_STATE",
						new GameStateData(gs.symbols().stream().map(s -> s.value()).toList(), prices));
			}
		};
	}

	private void sendMessage(WebSocketConnection connection, ServerMessage message) {
		try {
			String json = objectMapper.writeValueAsString(message);
			connection.sendTextAndAwait(json);
		} catch (Exception e) {
			// best-effort
		}
	}

	private void sendError(WebSocketConnection connection, String code, String message) {
		sendMessage(connection, new ServerMessage("ERROR", new ErrorData(code, message)));
	}

	// Inbound message types
	record ClientMessage(String type, Map<String, Object> payload) {
	}

	// Outbound wire format
	private record ServerMessage(String type, Object data) {
	}

	private record TickerTickedData(String symbol, int price) {
	}

	private record OrderFilledData(String symbol, int price, String side, String playerId) {
	}

	private record PlayerJoinedData(String playerId) {
	}

	private record AllPlayersReadyData() {
	}

	private record GameCreatedData(List<String> symbols) {
	}

	private record GameStateData(List<String> symbols, Map<String, Integer> prices) {
	}

	private record JoinedData(String playerId) {
	}

	private record OrderFilledResponseData(String symbol, int price, String side) {
	}

	private record ErrorData(String code, String message) {
	}
}

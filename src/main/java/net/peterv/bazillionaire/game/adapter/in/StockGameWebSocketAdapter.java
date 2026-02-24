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
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.service.GameEvent;
import net.peterv.bazillionaire.game.service.GameMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebSocket(path = "/game/{gameId}")
public class StockGameWebSocketAdapter {

	record ClientMessage(String type, Map<String, Object> payload) {
	}

	record JoinPayload(String playerId) {
	}

	record OrderPayload(String ticker, int price) {
	}

	@Inject
	JoinGameUseCase joinGameUseCase;

	@Inject
	PlaceOrderUseCase placeOrderUseCase;

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
				sendMessage(connection, "JOINED", Map.of("playerId", playerIdStr));
			}
		}
		dispatchMessages(gameId, result.messages());
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
				sendMessage(connection, "ORDER_FILLED", Map.of("symbol", ticker, "price", price, "side", sideStr));
			}
			case OrderResult.Rejected rej -> sendError(connection, "ORDER_REJECTED", rej.reason());
			case OrderResult.InvalidOrder inv -> sendError(connection, "INVALID_ORDER", inv.reason());
		}
		dispatchMessages(gameId, result.messages());
	}

	public void dispatchMessages(String gameId, List<GameMessage> messages) {
		for (GameMessage message : messages) {
			Map<String, Object> serialized = serializeEvent(message.event());
			String json;
			try {
				json = objectMapper.writeValueAsString(serialized);
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

	private Map<String, Object> serializeEvent(GameEvent event) {
		return switch (event) {
			case GameEvent.TickerTicked tt -> Map.of(
					"type", "TICKER_TICKED",
					"data", Map.of("symbol", tt.symbol().value(), "price", tt.price().cents()));
			case GameEvent.OrderFilled of -> {
				Order order = of.order();
				String side = order instanceof Order.Buy ? "BUY" : "SELL";
				yield Map.of(
						"type", "ORDER_FILLED",
						"data", Map.of(
								"symbol", order.symbol().value(),
								"price", order.price().cents(),
								"side", side,
								"playerId", of.playerId().value()));
			}
			case GameEvent.PlayerJoined pj -> Map.of(
					"type", "PLAYER_JOINED",
					"data", Map.of("playerId", pj.playerId().value()));
			case GameEvent.AllPlayersReady ignored -> Map.of(
					"type", "ALL_PLAYERS_READY",
					"data", Map.of());
			case GameEvent.GameCreated gc -> Map.of(
					"type", "GAME_CREATED",
					"data", Map.of("symbols", gc.symbols().stream().map(s -> s.value()).toList()));
			case GameEvent.GameState gs -> {
				Map<String, Integer> prices = new LinkedHashMap<>();
				gs.prices().forEach((sym, money) -> prices.put(sym.value(), money.cents()));
				yield Map.of(
						"type", "GAME_STATE",
						"data", Map.of(
								"symbols", gs.symbols().stream().map(s -> s.value()).toList(),
								"prices", prices));
			}
		};
	}

	private void sendMessage(WebSocketConnection connection, String type, Object data) {
		try {
			String json = objectMapper.writeValueAsString(Map.of("type", type, "data", data));
			connection.sendTextAndAwait(json);
		} catch (Exception e) {
			// best-effort
		}
	}

	private void sendError(WebSocketConnection connection, String code, String message) {
		sendMessage(connection, "ERROR", Map.of("code", code, "message", message));
	}
}

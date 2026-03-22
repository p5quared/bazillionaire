package net.peterv.bazillionaire.game.adapter.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import net.peterv.bazillionaire.game.domain.JoinResult;
import net.peterv.bazillionaire.game.domain.event.GameMessage;
import net.peterv.bazillionaire.game.domain.order.OrderResult;
import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;
import net.peterv.bazillionaire.game.domain.types.Audience;
import net.peterv.bazillionaire.game.domain.types.PlayerId;
import net.peterv.bazillionaire.game.port.in.JoinGameCommand;
import net.peterv.bazillionaire.game.port.in.JoinGameUseCase;
import net.peterv.bazillionaire.game.port.in.PlaceOrderCommand;
import net.peterv.bazillionaire.game.port.in.PlaceOrderUseCase;
import net.peterv.bazillionaire.game.port.in.StartGameCommand;
import net.peterv.bazillionaire.game.port.in.StartGameUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;
import net.peterv.bazillionaire.game.port.in.UsePowerupCommand;
import net.peterv.bazillionaire.game.port.in.UsePowerupUseCase;

@WebSocket(path = "/game/{gameId}")
public class StockGameWebSocketAdapter {

  @Inject JoinGameUseCase joinGameUseCase;

  @Inject PlaceOrderUseCase placeOrderUseCase;

  @Inject StartGameUseCase startGameUseCase;

  @Inject UsePowerupUseCase usePowerupUseCase;

  @Inject GameSessionRegistry registry;

  @Inject ObjectMapper objectMapper;

  private final GameEventSerializer serializer = new GameEventSerializer();

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
        case "BUY" ->
            handleOrder(connection, gameId, msg.payload(), PlaceOrderCommand.OrderSide.BUY);
        case "SELL" ->
            handleOrder(connection, gameId, msg.payload(), PlaceOrderCommand.OrderSide.SELL);
        case "USE_POWERUP" -> handleUsePowerup(connection, gameId, msg.payload());
        default -> sendError(connection, "UNKNOWN_TYPE", "Unknown message type: " + msg.type());
      }
    } catch (Exception e) {
      sendError(connection, "PARSE_ERROR", e.getMessage());
    }
  }

  private void handleJoin(
      WebSocketConnection connection, String gameId, Map<String, Object> payload) {
    String playerIdStr = (String) payload.get("playerId");
    UseCaseResult<JoinResult> result =
        joinGameUseCase.join(new JoinGameCommand(gameId, playerIdStr));
    switch (result.result()) {
      case JoinResult.AlreadyReady ignored ->
          sendError(connection, "ALREADY_JOINED", "Already joined");
      case JoinResult.InvalidJoin inv -> sendError(connection, "INVALID_JOIN", inv.reason());
      default -> {
        registry.associatePlayer(connection.id(), new PlayerId(playerIdStr));
        sendJson(
            connection,
            new GameEventSerializer.ServerMessage("JOINED", new JoinedData(playerIdStr)));
      }
    }
    dispatchMessages(gameId, result.messages());
    if (result.result() instanceof JoinResult.AllReady) {
      UseCaseResult<Void> startResult = startGameUseCase.startGame(new StartGameCommand(gameId));
      dispatchMessages(gameId, startResult.messages());
    }
  }

  private void handleOrder(
      WebSocketConnection connection,
      String gameId,
      Map<String, Object> payload,
      PlaceOrderCommand.OrderSide side) {
    String ticker = (String) payload.get("ticker");
    String playerIdStr = registry.findPlayer(connection.id()).map(PlayerId::value).orElse(null);
    if (playerIdStr == null) {
      sendError(connection, "NOT_JOINED", "Must join game before placing orders");
      return;
    }
    UseCaseResult<OrderResult> result =
        placeOrderUseCase.placeOrder(new PlaceOrderCommand(gameId, playerIdStr, ticker, side));
    switch (result.result()) {
      case OrderResult.Filled filled -> {
        String sideStr = side == PlaceOrderCommand.OrderSide.BUY ? "BUY" : "SELL";
        sendJson(
            connection,
            new GameEventSerializer.ServerMessage(
                "ORDER_FILLED",
                new OrderFilledResponseData(
                    ticker, filled.fillPrice().cents(), sideStr, filled.costBasis().cents())));
      }
      case OrderResult.Rejected rej -> sendError(connection, "ORDER_REJECTED", rej.reason());
      case OrderResult.InvalidOrder inv -> sendError(connection, "INVALID_ORDER", inv.reason());
    }
    dispatchMessages(gameId, result.messages());
  }

  private void handleUsePowerup(
      WebSocketConnection connection, String gameId, Map<String, Object> payload) {
    String powerupName = (String) payload.get("powerupName");
    String targetPlayerId = (String) payload.get("targetPlayerId");
    String targetSymbol = (String) payload.get("targetSymbol");
    int quantity =
        payload.containsKey("quantity") ? ((Number) payload.get("quantity")).intValue() : 1;
    String playerIdStr = registry.findPlayer(connection.id()).map(PlayerId::value).orElse(null);
    if (playerIdStr == null) {
      sendError(connection, "NOT_JOINED", "Must join game before using powerups");
      return;
    }
    UseCaseResult<UsePowerupResult> result =
        usePowerupUseCase.usePowerup(
            new UsePowerupCommand(
                gameId, playerIdStr, powerupName, targetPlayerId, targetSymbol, quantity));
    switch (result.result()) {
      case UsePowerupResult.Activated ignored -> {}
      case UsePowerupResult.NotOwned ignored ->
          sendError(connection, "POWERUP_NOT_OWNED", "You do not own that powerup");
      case UsePowerupResult.InvalidTarget inv ->
          sendError(connection, "INVALID_TARGET", inv.reason());
    }
    dispatchMessages(gameId, result.messages());
  }

  public void dispatchMessages(String gameId, List<GameMessage> messages) {
    for (GameMessage message : messages) {
      GameEventSerializer.ServerMessage serverMessage = serializer.serialize(message.event());
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
          registry
              .connectionForPlayer(gameId, only.playerId())
              .ifPresent(conn -> conn.sendTextAndAwait(json));
        }
      }
    }
  }

  private void sendJson(WebSocketConnection connection, GameEventSerializer.ServerMessage message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      connection.sendTextAndAwait(json);
    } catch (Exception e) {
      // best-effort
    }
  }

  private void sendError(WebSocketConnection connection, String code, String message) {
    sendJson(
        connection, new GameEventSerializer.ServerMessage("ERROR", new ErrorData(code, message)));
  }

  // Inbound message types
  record ClientMessage(String type, Map<String, Object> payload) {}

  // Adapter-specific response records (not from domain events)
  private record JoinedData(String playerId) {}

  private record OrderFilledResponseData(String symbol, int price, String side, int costBasis) {}

  private record ErrorData(String code, String message) {}
}

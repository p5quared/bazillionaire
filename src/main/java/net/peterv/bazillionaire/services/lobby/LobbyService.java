package net.peterv.bazillionaire.services.lobby;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;
import net.peterv.bazillionaire.game.port.in.CreateGameUseCase;

@ApplicationScoped
public class LobbyService {

  @Inject CreateGameUseCase createGameUseCase;

  @Transactional
  public String createLobby(String name, int maxPlayers, String playerId) {
    String id = generateId();
    var lobby = new Lobby();
    lobby.id = id;
    lobby.name = name;
    lobby.status = Lobby.LobbyStatus.WAITING;
    lobby.maxPlayers = maxPlayers;
    lobby.createdAt = Instant.now();
    lobby.persist();
    lobby.addMember(playerId, playerId);
    return id;
  }

  @Transactional
  public void joinLobby(String lobbyId, String playerId) {
    Lobby lobby = findLobbyOrThrow(lobbyId);
    try {
      lobby.addMember(playerId, playerId);
    } catch (Lobby.AlreadyInLobbyException ignored) {
      // idempotent — already a member
    }
  }

  @Transactional
  public void leaveLobby(String lobbyId, String playerId) {
    Lobby lobby = findLobbyOrThrow(lobbyId);
    lobby.removeMember(playerId);
  }

  @Transactional
  public void updateSettings(
      String lobbyId,
      String actorId,
      int tickerCount,
      int initialBalanceCents,
      int initialPriceCents,
      int gameDurationSeconds) {
    Lobby lobby = findLobbyOrThrow(lobbyId);
    requireMember(lobby, actorId);
    lobby.updateSettings(tickerCount, initialBalanceCents, initialPriceCents, gameDurationSeconds);
  }

  @Transactional
  public void startLobby(String lobbyId, String actorId) {
    Lobby lobby = findLobbyOrThrow(lobbyId);
    requireMember(lobby, actorId);
    lobby.start();
    try {
      createGameUseCase.createGame(
          new CreateGameCommand(
              lobby.id,
              lobby.members.stream().map(m -> m.playerId).toList(),
              lobby.tickerCount,
              new Money(lobby.initialBalanceCents),
              new Money(lobby.initialPriceCents),
              lobby.gameDurationSeconds,
              new Random()));
    } catch (RuntimeException e) {
      throw new LobbyStartFailedException(lobbyId, e);
    }
  }

  @Transactional
  public void deleteLobby(String lobbyId, String actorId) {
    Lobby lobby = findLobbyOrThrow(lobbyId);
    requireMember(lobby, actorId);
    lobby.deleteIfOpen();
  }

  private String generateId() {
    String id;
    do {
      id = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    } while (Lobby.findById(id) != null);
    return id;
  }

  private Lobby findLobbyOrThrow(String lobbyId) {
    Lobby lobby = Lobby.findById(lobbyId);
    if (lobby == null) throw new LobbyNotFoundException(lobbyId);
    return lobby;
  }

  private void requireMember(Lobby lobby, String actorId) {
    if (!lobby.hasMember(actorId)) throw new LobbyMemberRequiredException(lobby.id, actorId);
  }
}

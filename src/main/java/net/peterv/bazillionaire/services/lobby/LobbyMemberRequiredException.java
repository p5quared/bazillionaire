package net.peterv.bazillionaire.services.lobby;

public class LobbyMemberRequiredException extends RuntimeException {
  public LobbyMemberRequiredException(String lobbyId, String playerId) {
    super("Player " + playerId + " is not a member of lobby " + lobbyId);
  }
}

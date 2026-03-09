package net.peterv.bazillionaire.services.lobby;

public class LobbyStartFailedException extends RuntimeException {
	public LobbyStartFailedException(String lobbyId, Throwable cause) {
		super("Failed to start lobby " + lobbyId, cause);
	}
}

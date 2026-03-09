package net.peterv.bazillionaire.services.lobby;

public class LobbyNotFoundException extends RuntimeException {
	public LobbyNotFoundException(String lobbyId) {
		super("Lobby not found: " + lobbyId);
	}
}

package net.peterv.bazillionaire.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import net.peterv.bazillionaire.game.port.in.CreateGameCommand;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class LobbyService {

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
		Lobby lobby = Lobby.findById(lobbyId);
		if (lobby == null)
			return;
		try {
			lobby.addMember(playerId, playerId);
		} catch (Lobby.AlreadyInLobbyException ignored) {
			// idempotent — already a member
		}
	}

	@Transactional
	public void leaveLobby(String lobbyId, String playerId) {
		Lobby lobby = Lobby.findById(lobbyId);
		if (lobby == null)
			return;
		lobby.removeMember(playerId);
	}

	@Transactional
	public CreateGameCommand startLobby(String lobbyId) {
		Lobby lobby = Lobby.findById(lobbyId);
		if (lobby == null)
			throw new NotFoundException("Lobby not found");
		lobby.start();
		var playerIds = lobby.members.stream().map(m -> m.playerId).toList();
		return new CreateGameCommand(lobbyId, playerIds, 3);
	}

	@Transactional
	public void deleteLobby(String lobbyId) {
		Lobby lobby = Lobby.findById(lobbyId);
		if (lobby == null)
			return;
		if (lobby.status != Lobby.LobbyStatus.WAITING)
			throw new Lobby.LobbyNotOpenException();
		lobby.delete();
	}

	private String generateId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
	}
}

package net.peterv.bazillionaire.game.adapter.in;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GameSessionRegistry {

	private final Map<String, WebSocketConnection> connectionById = new ConcurrentHashMap<>();
	private final Map<String, String> gameIdByConnection = new ConcurrentHashMap<>();
	private final Map<String, String> playerIdByConnection = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> connectionsByGame = new ConcurrentHashMap<>();

	public void register(String connectionId, WebSocketConnection connection, String gameId) {
		connectionById.put(connectionId, connection);
		gameIdByConnection.put(connectionId, gameId);
		connectionsByGame.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(connectionId);
	}

	public void deregister(String connectionId) {
		connectionById.remove(connectionId);
		String gameId = gameIdByConnection.remove(connectionId);
		playerIdByConnection.remove(connectionId);
		if (gameId != null) {
			Set<String> connections = connectionsByGame.get(gameId);
			if (connections != null) {
				connections.remove(connectionId);
				if (connections.isEmpty()) {
					connectionsByGame.remove(gameId);
				}
			}
		}
	}

	public void associatePlayer(String connectionId, PlayerId playerId) {
		playerIdByConnection.put(connectionId, playerId.value());
	}

	public Optional<PlayerId> findPlayer(String connectionId) {
		return Optional.ofNullable(playerIdByConnection.get(connectionId)).map(PlayerId::new);
	}

	public Collection<WebSocketConnection> connectionsForGame(String gameId) {
		Set<String> ids = connectionsByGame.getOrDefault(gameId, Collections.emptySet());
		return ids.stream()
				.map(connectionById::get)
				.filter(c -> c != null)
				.toList();
	}

	public Optional<WebSocketConnection> connectionForPlayer(String gameId, PlayerId playerId) {
		Set<String> ids = connectionsByGame.getOrDefault(gameId, Collections.emptySet());
		return ids.stream()
				.filter(id -> playerId.value().equals(playerIdByConnection.get(id)))
				.map(connectionById::get)
				.filter(c -> c != null)
				.findFirst();
	}

	public void deregisterGame(String gameId) {
		Set<String> connectionIds = connectionsByGame.remove(gameId);
		if (connectionIds != null) {
			for (String connectionId : connectionIds) {
				connectionById.remove(connectionId);
				gameIdByConnection.remove(connectionId);
				playerIdByConnection.remove(connectionId);
			}
		}
	}

	public Set<String> activeGameIds() {
		return Collections.unmodifiableSet(connectionsByGame.keySet());
	}
}

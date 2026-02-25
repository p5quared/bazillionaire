package net.peterv.bazillionaire.web;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies")
public class Lobby extends PanacheEntityBase {

	public static final int MIN_PLAYERS = 2;
	public static final int DEFAULT_MAX = 8;
	public static final int MAX_HARD_CAP = 16;

	@Id
	public String id;

	public String name;

	@Enumerated(EnumType.STRING)
	public LobbyStatus status;

	public int maxPlayers;

	public Instant createdAt;

	@OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("joinedAt ASC")
	public List<LobbyMember> members = new ArrayList<>();

	public enum LobbyStatus {
		WAITING, STARTED, CANCELLED
	}

	public boolean isFull() {
		return members.size() >= maxPlayers;
	}

	public boolean hasMember(String playerId) {
		return members.stream().anyMatch(m -> m.playerId.equals(playerId));
	}

	public void addMember(String playerId, String displayName) {
		if (status != LobbyStatus.WAITING)
			throw new LobbyNotOpenException();
		if (isFull())
			throw new LobbyFullException();
		if (hasMember(playerId))
			throw new AlreadyInLobbyException();

		LobbyMember member = new LobbyMember();
		member.lobby = this;
		member.playerId = playerId;
		member.displayName = displayName;
		member.joinedAt = Instant.now();
		members.add(member);
	}

	public void removeMember(String playerId) {
		members.removeIf(m -> m.playerId.equals(playerId));
	}

	public void start() {
		if (status != LobbyStatus.WAITING)
			throw new LobbyNotOpenException();
		if (members.size() < MIN_PLAYERS)
			throw new NotEnoughPlayersException();
		status = LobbyStatus.STARTED;
	}

	public static List<Lobby> findOpen() {
		return list("status", LobbyStatus.WAITING);
	}

	public static class LobbyFullException extends RuntimeException {
	}

	public static class LobbyNotOpenException extends RuntimeException {
	}

	public static class AlreadyInLobbyException extends RuntimeException {
	}

	public static class NotEnoughPlayersException extends RuntimeException {
	}
}

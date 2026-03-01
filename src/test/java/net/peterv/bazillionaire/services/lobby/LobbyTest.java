package net.peterv.bazillionaire.services.lobby;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LobbyTest {

	private Lobby lobby(int maxPlayers) {
		var l = new Lobby();
		l.id = "TEST01";
		l.name = "Test";
		l.status = Lobby.LobbyStatus.WAITING;
		l.maxPlayers = maxPlayers;
		l.createdAt = Instant.now();
		return l;
	}

	@Test
	void addMember_addsPlayerToList() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		assertEquals(1, lobby.members.size());
		assertEquals("alice", lobby.members.get(0).playerId);
	}

	@Test
	void addMember_throwsWhenFull() {
		var lobby = lobby(1);
		lobby.addMember("alice", "Alice");
		assertThrows(Lobby.LobbyFullException.class, () -> lobby.addMember("bob", "Bob"));
	}

	@Test
	void addMember_throwsWhenNotWaiting() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		lobby.addMember("bob", "Bob");
		lobby.start();
		assertThrows(Lobby.LobbyNotOpenException.class, () -> lobby.addMember("charlie", "Charlie"));
	}

	@Test
	void addMember_throwsWhenAlreadyIn() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		assertThrows(Lobby.AlreadyInLobbyException.class, () -> lobby.addMember("alice", "Alice"));
	}

	@Test
	void hasMember_returnsTrueForExistingMember() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		assertTrue(lobby.hasMember("alice"));
		assertFalse(lobby.hasMember("bob"));
	}

	@Test
	void isFull_returnsTrueWhenAtCapacity() {
		var lobby = lobby(2);
		lobby.addMember("alice", "Alice");
		assertFalse(lobby.isFull());
		lobby.addMember("bob", "Bob");
		assertTrue(lobby.isFull());
	}

	@Test
	void start_setsStatusToStarted() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		lobby.addMember("bob", "Bob");
		lobby.start();
		assertEquals(Lobby.LobbyStatus.STARTED, lobby.status);
	}

	@Test
	void start_throwsWhenTooFewPlayers() {
		var lobby = lobby(4);
		assertThrows(Lobby.NotEnoughPlayersException.class, lobby::start);
	}

	@Test
	void start_throwsWhenAlreadyStarted() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		lobby.addMember("bob", "Bob");
		lobby.start();
		assertThrows(Lobby.LobbyNotOpenException.class, lobby::start);
	}

	@Test
	void removeMember_removesCorrectPlayer() {
		var lobby = lobby(4);
		lobby.addMember("alice", "Alice");
		lobby.addMember("bob", "Bob");
		lobby.removeMember("alice");
		assertEquals(1, lobby.members.size());
		assertEquals("bob", lobby.members.get(0).playerId);
	}
}

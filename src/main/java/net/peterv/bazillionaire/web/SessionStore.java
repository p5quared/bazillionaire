package net.peterv.bazillionaire.web;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionStore {

	public sealed interface CreateSessionResult {
		record Success(String token) implements CreateSessionResult {
		}

		record UsernameTaken() implements CreateSessionResult {
		}
	}

	private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap.KeySetView<String, Boolean> activeUsernames = ConcurrentHashMap.newKeySet();

	public CreateSessionResult createSession(String username) {
		if (!activeUsernames.add(username)) {
			return new CreateSessionResult.UsernameTaken();
		}
		String token = UUID.randomUUID().toString();
		sessions.put(token, username);
		return new CreateSessionResult.Success(token);
	}

	public Optional<String> getUsername(String token) {
		return Optional.ofNullable(sessions.get(token));
	}

	public void removeSession(String token) {
		String username = sessions.remove(token);
		if (username != null) {
			activeUsernames.remove(username);
		}
	}
}

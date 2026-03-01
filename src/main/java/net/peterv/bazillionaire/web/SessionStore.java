package net.peterv.bazillionaire.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.peterv.bazillionaire.user.models.User;
import net.peterv.bazillionaire.user.models.UserSession;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SessionStore {

	public sealed interface CreateSessionResult {
		record Success(String token) implements CreateSessionResult {
		}

		record UsernameTaken() implements CreateSessionResult {
		}
	}

	public CreateSessionResult createSession(String username) {
		User user = User.findByUsername(username).orElseGet(() -> {
			User newUser = new User();
			newUser.username = username;
			newUser.persist();
			return newUser;
		});

		UserSession.delete("user = ?1 and expiresAt < ?2", user, LocalDateTime.now());

		if (UserSession.count("user", user) > 0) {
			return new CreateSessionResult.UsernameTaken();
		}

		UserSession session = new UserSession();
		session.user = user;
		session.token = UUID.randomUUID().toString();
		session.expiresAt = LocalDateTime.now().plusHours(24);
		session.persist();

		return new CreateSessionResult.Success(session.token);
	}

	public Optional<String> getUsername(String token) {
		return UserSession.findByToken(token)
				.filter(s -> !s.isExpired())
				.map(s -> s.user.username);
	}

	public void removeSession(String token) {
		UserSession.delete("token", token);
	}
}

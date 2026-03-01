package net.peterv.bazillionaire.services.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import net.peterv.bazillionaire.services.user.User;
import net.peterv.bazillionaire.services.user.UserSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class AuthService {

	private static final Duration SESSION_TTL = Duration.ofHours(24);

	public sealed interface CreateSessionResult {
		record Success(String token) implements CreateSessionResult {
		}

		record UsernameTaken() implements CreateSessionResult {
		}
	}

	@Transactional
	public CreateSessionResult createSession(String username) {
		LocalDateTime now = LocalDateTime.now();
		User user = User.findOrCreateByUsername(username);

		UserSession.deleteExpiredForUser(user, now);
		if (UserSession.existsForUser(user)) {
			return new CreateSessionResult.UsernameTaken();
		}

		UserSession created = UserSession.createForUser(user, now, SESSION_TTL);
		return new CreateSessionResult.Success(created.token);
	}

	public Optional<String> getUsername(String token) {
		return UserSession.findActiveByToken(token, LocalDateTime.now())
				.map(s -> s.user.username);
	}

	@Transactional
	public void removeSession(String token) {
		UserSession.deleteByToken(token);
	}
}

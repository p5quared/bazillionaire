package net.peterv.bazillionaire.services.user;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Entity
public class UserSession extends PanacheEntity {

	@ManyToOne
	@JoinColumn(nullable = false)
	public User user;

	@Column(nullable = false, unique = true)
	public String token;

	@Column(nullable = false)
	public LocalDateTime expiresAt;

	public static Optional<UserSession> findByToken(String token) {
		return find("token", token).firstResultOptional();
	}

	public static void deleteExpiredForUser(User user, LocalDateTime now) {
		delete("user = ?1 and expiresAt < ?2", user, now);
	}

	public static boolean existsForUser(User user) {
		return count("user", user) > 0;
	}

	public static UserSession createForUser(User user, LocalDateTime now, Duration ttl) {
		UserSession session = new UserSession();
		session.user = user;
		session.token = UUID.randomUUID().toString();
		session.expiresAt = now.plus(ttl);
		session.persist();
		return session;
	}

	public static Optional<UserSession> findActiveByToken(String token, LocalDateTime now) {
		return findByToken(token)
				.filter(session -> !session.isExpiredAt(now));
	}

	public static void deleteByToken(String token) {
		delete("token", token);
	}

	public boolean isExpired() {
		return isExpiredAt(LocalDateTime.now());
	}

	public boolean isExpiredAt(LocalDateTime now) {
		return now.isAfter(this.expiresAt);
	}
}

package net.peterv.bazillionaire.services.user;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;
import java.util.Optional;

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

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiresAt);
	}
}

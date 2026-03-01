package net.peterv.bazillionaire.user.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Optional;

@Entity
@Table(name = "app_user")
public class User extends PanacheEntity {

	@Column(nullable = false, unique = true)
	public String username;

	public static Optional<User> findByUsername(String username) {
		return find("username", username).firstResultOptional();
	}
}

package net.peterv.bazillionaire.web;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "lobby_members")
public class LobbyMember extends PanacheEntityBase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lobby_id")
	public Lobby lobby;

	public String playerId;

	public String displayName;

	public Instant joinedAt;
}

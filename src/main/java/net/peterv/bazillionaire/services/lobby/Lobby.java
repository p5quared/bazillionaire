package net.peterv.bazillionaire.services.lobby;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies")
public class Lobby extends PanacheEntityBase {

  public static final int MIN_PLAYERS = 1;
  public static final int DEFAULT_MAX = 8;
  public static final int MAX_HARD_CAP = 16;

  public static final int DEFAULT_TICKER_COUNT = 3;
  public static final int DEFAULT_INITIAL_BALANCE_CENTS = 100_000;
  public static final int DEFAULT_INITIAL_PRICE_CENTS = 10_000;
  public static final int DEFAULT_GAME_DURATION_SECONDS = 600;

  @Id public String id;

  public String name;

  @Enumerated(EnumType.STRING)
  public LobbyStatus status;

  public int maxPlayers;

  public Instant createdAt;

  public int tickerCount = DEFAULT_TICKER_COUNT;
  public int initialBalanceCents = DEFAULT_INITIAL_BALANCE_CENTS;
  public int initialPriceCents = DEFAULT_INITIAL_PRICE_CENTS;
  public int gameDurationSeconds = DEFAULT_GAME_DURATION_SECONDS;

  @OneToMany(
      mappedBy = "lobby",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("joinedAt ASC")
  public List<LobbyMember> members = new ArrayList<>();

  public enum LobbyStatus {
    WAITING,
    STARTED
  }

  public boolean isFull() {
    return members.size() >= maxPlayers;
  }

  public boolean hasMember(String playerId) {
    return members.stream().anyMatch(m -> m.playerId.equals(playerId));
  }

  public void addMember(String playerId, String displayName) {
    if (status != LobbyStatus.WAITING) throw new LobbyNotOpenException();
    if (hasMember(playerId)) throw new AlreadyInLobbyException();
    if (isFull()) throw new LobbyFullException();

    LobbyMember member = new LobbyMember();
    member.lobby = this;
    member.playerId = playerId;
    member.displayName = displayName;
    member.joinedAt = Instant.now();
    members.add(member);
  }

  public void removeMember(String playerId) {
    if (status != LobbyStatus.WAITING) throw new LobbyNotOpenException();
    members.removeIf(m -> m.playerId.equals(playerId));
  }

  public void deleteIfOpen() {
    if (status != LobbyStatus.WAITING) throw new LobbyNotOpenException();
    delete();
  }

  public int getInitialBalanceDollars() {
    return initialBalanceCents / 100;
  }

  public int getInitialPriceDollars() {
    return initialPriceCents / 100;
  }

  public void updateSettings(
      int tickerCount, int initialBalanceCents, int initialPriceCents, int gameDurationSeconds) {
    if (status != LobbyStatus.WAITING) throw new LobbyNotOpenException();
    this.tickerCount = tickerCount;
    this.initialBalanceCents = initialBalanceCents;
    this.initialPriceCents = initialPriceCents;
    this.gameDurationSeconds = gameDurationSeconds;
  }

  public void start() {
    if (status != LobbyStatus.WAITING) throw new LobbyNotOpenException();
    if (members.size() < MIN_PLAYERS) throw new NotEnoughPlayersException();
    status = LobbyStatus.STARTED;
  }

  public static List<Lobby> findOpen() {
    return list("status", LobbyStatus.WAITING);
  }

  public static class LobbyFullException extends RuntimeException {}

  public static class LobbyNotOpenException extends RuntimeException {}

  public static class AlreadyInLobbyException extends RuntimeException {}

  public static class NotEnoughPlayersException extends RuntimeException {}
}

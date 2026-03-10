package net.peterv.bazillionaire.game.domain.types;

public record GameId(String value) {
  public GameId {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("GameId cannot be blank");
  }
}

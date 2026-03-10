package net.peterv.bazillionaire.game.domain.types;

public record PlayerId(String value) {
  public PlayerId {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("PlayerId cannot be blank");
  }
}

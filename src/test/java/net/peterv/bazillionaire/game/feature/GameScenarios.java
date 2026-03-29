package net.peterv.bazillionaire.game.feature;

public final class GameScenarios {

  public static GameTestHarness twoPlayerGame() {
    return GameTestHarness.builder().players("player1", "player2").build();
  }

  public static GameTestHarness twoPlayerStarted() {
    return GameTestHarness.builder().players("player1", "player2").build().joinAllAndStart();
  }

  public static GameTestHarness singlePlayerStarted() {
    return GameTestHarness.builder().players("player1").build().joinAllAndStart();
  }

  public static GameTestHarness shortGame(int duration) {
    return GameTestHarness.builder().players("player1", "player2").duration(duration).build();
  }

  private GameScenarios() {}
}

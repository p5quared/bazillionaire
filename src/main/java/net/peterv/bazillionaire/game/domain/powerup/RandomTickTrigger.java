package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;
import java.util.Random;
import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public class RandomTickTrigger implements PowerupTrigger {
  private final double probability;
  private final Money boostAmount;
  private final Random random;

  public RandomTickTrigger(double probability, Money boostAmount, Random random) {
    if (probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
    }
    this.probability = probability;
    this.boostAmount = boostAmount;
    this.random = random;
  }

  @Override
  public List<AwardedPowerup> evaluate(GameContext context) {
    if (random.nextDouble() >= probability) {
      return List.of();
    }
    List<PlayerId> playerIds = List.copyOf(context.players().keySet());
    PlayerId recipient = playerIds.get(random.nextInt(playerIds.size()));
    return List.of(new AwardedPowerup(recipient, new CashBoostPowerup(recipient, boostAmount)));
  }
}

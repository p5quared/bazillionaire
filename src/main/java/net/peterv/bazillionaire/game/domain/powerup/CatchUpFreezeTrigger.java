package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

import java.util.List;
import java.util.Random;

public final class CatchUpFreezeTrigger implements PowerupTrigger {
	private final double probability;
	private final int freezeDuration;
	private final Random random;
	private final TrailingPlayerStrategy trailingStrategy;
	private final LeadingPlayerStrategy leadingStrategy;

	public CatchUpFreezeTrigger(double probability, int freezeDuration, Random random) {
		if (probability < 0.0 || probability > 1.0) {
			throw new IllegalArgumentException("Probability must be between 0.0 and 1.0");
		}
		if (freezeDuration <= 0) {
			throw new IllegalArgumentException("Freeze duration must be positive");
		}
		this.probability = probability;
		this.freezeDuration = freezeDuration;
		this.random = random;
		this.trailingStrategy = new TrailingPlayerStrategy();
		this.leadingStrategy = new LeadingPlayerStrategy();
	}

	@Override
	public List<AwardedPowerup> evaluate(GameContext context) {
		if (context.players().size() < 2) {
			return List.of();
		}

		if (random.nextDouble() >= probability) {
			return List.of();
		}

		PlayerId recipient = trailingStrategy.selectTarget(context);
		PlayerId victim = leadingStrategy.selectTarget(context);

		if (recipient == null || victim == null || recipient.equals(victim)) {
			return List.of();
		}

		return List.of(new AwardedPowerup(recipient, new OrderFreezePowerup(victim, freezeDuration)));
	}
}

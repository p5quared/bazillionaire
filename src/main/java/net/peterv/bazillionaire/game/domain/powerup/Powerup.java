package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;

public abstract class Powerup {
	public abstract String name();
	protected int remainingTicks;

	protected Powerup(int duration) {
		this.remainingTicks = duration;
	}

	public List<PowerupEffect> onActivate() {
		return List.of();
	}

	public List<PowerupEffect> onTick() {
		return List.of();
	}

	public List<PowerupEffect> onDeactivate() {
		return List.of();
	}

	public boolean isExpired() {
		return remainingTicks == 0;
	}

	public List<PowerupEffect> tick() {
		if (remainingTicks > 0) {
			remainingTicks--;
		}
		if (!isExpired()) {
			return onTick();
		}
		return List.of();
	}
}

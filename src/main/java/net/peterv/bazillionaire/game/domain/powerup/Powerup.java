package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;

public abstract class Powerup {
	protected int remainingTicks;

	protected Powerup(int duration) {
		this.remainingTicks = duration;
	}

	public void onActivate(Game game) {
	}

	public void onTick(Game game) {
	}

	public void onDeactivate(Game game) {
	}

	public boolean isExpired() {
		return remainingTicks == 0;
	}

	public void tick(Game game) {
		if (remainingTicks > 0) {
			remainingTicks--;
		}
		if (!isExpired()) {
			onTick(game);
		}
	}
}

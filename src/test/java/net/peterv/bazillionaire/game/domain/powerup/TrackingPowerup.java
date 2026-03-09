package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.Game;

class TrackingPowerup extends Powerup {
    int activateCount = 0;
    int tickCount = 0;
    int deactivateCount = 0;

    TrackingPowerup(int duration) {
        super(duration);
    }

    @Override
    public String name() { return "tracking"; }

    @Override
    public void onActivate(Game game) { activateCount++; }

    @Override
    public void onTick(Game game) { tickCount++; }

    @Override
    public void onDeactivate(Game game) { deactivateCount++; }
}

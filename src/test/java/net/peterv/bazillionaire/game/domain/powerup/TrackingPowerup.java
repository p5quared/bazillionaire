package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;

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
    public String description() { return "test"; }

    @Override
    public PowerupUsageType usageType() { return PowerupUsageType.INSTANT; }

    @Override
    public ConsumptionMode consumptionMode() { return ConsumptionMode.SINGLE; }

    @Override
    public List<PowerupEffect> onActivate() { activateCount++; return List.of(); }

    @Override
    public List<PowerupEffect> onTick() { tickCount++; return List.of(); }

    @Override
    public List<PowerupEffect> onDeactivate() { deactivateCount++; return List.of(); }
}

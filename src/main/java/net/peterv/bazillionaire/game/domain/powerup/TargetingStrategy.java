package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

public interface TargetingStrategy {
    PlayerId selectTarget(GameContext context);
}

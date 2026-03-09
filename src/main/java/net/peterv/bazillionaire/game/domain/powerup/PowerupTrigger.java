package net.peterv.bazillionaire.game.domain.powerup;

import java.util.List;

public interface PowerupTrigger {
    List<AwardedPowerup> evaluate(GameContext context);
}

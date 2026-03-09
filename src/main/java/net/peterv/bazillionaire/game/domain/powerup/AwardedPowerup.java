package net.peterv.bazillionaire.game.domain.powerup;

import net.peterv.bazillionaire.game.domain.types.PlayerId;

public record AwardedPowerup(PlayerId recipient, Powerup powerup) {
}

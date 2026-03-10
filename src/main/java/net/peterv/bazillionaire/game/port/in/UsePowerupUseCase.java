package net.peterv.bazillionaire.game.port.in;

import net.peterv.bazillionaire.game.domain.powerup.UsePowerupResult;

public interface UsePowerupUseCase {
  UseCaseResult<UsePowerupResult> usePowerup(UsePowerupCommand cmd);
}

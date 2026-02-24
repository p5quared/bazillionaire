package net.peterv.bazillionaire.game.adapter.in;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.peterv.bazillionaire.game.port.in.TickCommand;
import net.peterv.bazillionaire.game.port.in.TickUseCase;
import net.peterv.bazillionaire.game.port.in.UseCaseResult;

@ApplicationScoped
public class GameTickScheduler {

	@Inject
	TickUseCase tickUseCase;

	@Inject
	GameSessionRegistry registry;

	@Inject
	StockGameWebSocketAdapter adapter;

	@Scheduled(every = "1s")
	@Blocking
	void tick() {
		for (String gameId : registry.activeGameIds()) {
			UseCaseResult<Void> result = tickUseCase.tick(new TickCommand(gameId));
			adapter.dispatchMessages(gameId, result.messages());
		}
	}
}

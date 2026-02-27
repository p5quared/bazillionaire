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

	private static final int TICKS_PER_SECOND = 4;
	private static final long TICK_INTERVAL_MS = 1000 / TICKS_PER_SECOND;

	@Scheduled(every = "10s")
	@Blocking
	void tick() {
		for (int i = 0; i < TICKS_PER_SECOND * 10; i++) {
			long elapsed = timeExecution(this::tickAllGames);
			if (i < (TICKS_PER_SECOND * 10) - 1) {
				sleepRemaining(elapsed);
			}
		}
	}

	private void tickAllGames() {
		for (String gameId : registry.activeGameIds()) {
			UseCaseResult<Void> result = tickUseCase.tick(new TickCommand(gameId));
			adapter.dispatchMessages(gameId, result.messages());
		}
	}

	private long timeExecution(Runnable task) {
		long start = System.currentTimeMillis();
		task.run();
		return System.currentTimeMillis() - start;
	}

	private void sleepRemaining(long elapsedMs) {
		long sleepTime = TICK_INTERVAL_MS - elapsedMs;
		if (sleepTime <= 0)
			return;
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}

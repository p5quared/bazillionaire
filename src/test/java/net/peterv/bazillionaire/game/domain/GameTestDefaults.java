package net.peterv.bazillionaire.game.domain;

import net.peterv.bazillionaire.game.domain.types.Money;
import net.peterv.bazillionaire.game.domain.types.PlayerId;

public final class GameTestDefaults {
	public static final Money INITIAL_BALANCE = new Money(100_000_00);
	public static final Money INITIAL_PRICE = new Money(100_00);
	public static final int TOTAL_DURATION = 1200;
	public static final long SEED = 42L;
	public static final int TICKER_COUNT = 3;

	public static final PlayerId PLAYER_1 = new PlayerId("player1");
	public static final PlayerId PLAYER_2 = new PlayerId("player2");

	private GameTestDefaults() {
	}
}

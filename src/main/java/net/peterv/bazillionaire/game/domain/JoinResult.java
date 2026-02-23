package net.peterv.bazillionaire.game.domain;

public sealed interface JoinResult {
	record AlreadyReady() implements JoinResult {
	}

	record Joined() implements JoinResult {
	}

	record AllReady() implements JoinResult {
	}

	record GameInProgress() implements JoinResult {
	}

	record InvalidJoin(String reason) implements JoinResult {
	}
}

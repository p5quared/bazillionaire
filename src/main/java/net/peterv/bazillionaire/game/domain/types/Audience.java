package net.peterv.bazillionaire.game.domain.types;

public sealed interface Audience {
	record Everyone() implements Audience {
	}

	record Only(String playerId) implements Audience {
	}
}

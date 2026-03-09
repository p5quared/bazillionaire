package net.peterv.bazillionaire.game.domain.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerIdTest {

	@Test
	void validPlayerIdConstructs() {
		assertEquals("alice", new PlayerId("alice").value());
	}

	@Test
	void nullThrows() {
		assertThrows(IllegalArgumentException.class, () -> new PlayerId(null));
	}

	@Test
	void blankThrows() {
		assertThrows(IllegalArgumentException.class, () -> new PlayerId(""));
		assertThrows(IllegalArgumentException.class, () -> new PlayerId("   "));
	}
}

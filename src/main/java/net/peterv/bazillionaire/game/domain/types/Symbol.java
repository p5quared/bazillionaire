package net.peterv.bazillionaire.game.domain.types;

public record Symbol(String value) {
	public Symbol {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException("Symbol cannot be blank");
	}
}

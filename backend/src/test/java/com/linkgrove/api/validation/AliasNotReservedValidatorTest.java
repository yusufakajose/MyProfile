package com.linkgrove.api.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AliasNotReservedValidatorTest {

	private final AliasNotReservedValidator validator = new AliasNotReservedValidator();

	@Test
	void allowsNullOrBlank() {
		assertTrue(validator.isValid(null, null));
		assertTrue(validator.isValid("", null));
		assertTrue(validator.isValid("   ", null));
	}

	@Test
	void rejectsReservedAliases() {
		assertFalse(validator.isValid("admin", null));
		assertFalse(validator.isValid("LOGIN", null));
		assertFalse(validator.isValid("Api", null));
	}

	@Test
	void acceptsNonReservedAliases() {
		assertTrue(validator.isValid("my-alias", null));
		assertTrue(validator.isValid("user123", null));
	}
}



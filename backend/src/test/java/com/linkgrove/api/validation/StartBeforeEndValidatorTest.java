package com.linkgrove.api.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StartBeforeEndValidatorTest {

	private static class DummyRequest {
		public LocalDateTime startAt;
		public LocalDateTime endAt;
		DummyRequest(LocalDateTime s, LocalDateTime e) { this.startAt = s; this.endAt = e; }
	}

	@Test
	void validWhenEitherNull() {
		StartBeforeEndValidator v = new StartBeforeEndValidator();
		assertTrue(v.isValid(new DummyRequest(null, null), null));
		assertTrue(v.isValid(new DummyRequest(LocalDateTime.now(), null), null));
		assertTrue(v.isValid(new DummyRequest(null, LocalDateTime.now()), null));
	}

	@Test
	void validWhenStartBeforeEnd() {
		StartBeforeEndValidator v = new StartBeforeEndValidator();
		LocalDateTime s = LocalDateTime.now();
		LocalDateTime e = s.plusHours(1);
		assertTrue(v.isValid(new DummyRequest(s, e), null));
	}

	@Test
	void invalidWhenStartAfterOrEqualEnd() {
		StartBeforeEndValidator v = new StartBeforeEndValidator();
		LocalDateTime s = LocalDateTime.now();
		assertFalse(v.isValid(new DummyRequest(s.plusMinutes(1), s), null));
		assertFalse(v.isValid(new DummyRequest(s, s), null));
	}
}



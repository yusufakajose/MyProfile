package com.linkgrove.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil();
		// 32+ bytes secret for HS256
		ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ012345");
		ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L); // 60s
	}

	@Test
	void generateAndValidateToken() {
		String token = jwtUtil.generateToken("alice");
		assertNotNull(token);
		assertEquals("alice", jwtUtil.extractUsername(token));
		Date exp = jwtUtil.extractExpiration(token);
		assertTrue(exp.after(new Date()));
		assertTrue(jwtUtil.validateToken(token, "alice"));
		assertFalse(jwtUtil.validateToken(token, "bob"));
	}
}



package com.campania.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTests {

	@Test
	void allowsCapacitorOriginsAlongsideConfiguredOrigins() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
		CorsConfiguration configuration = new CorsConfig()
				.corsConfigurationSource("https://web.example.com,http://localhost:3000")
				.getCorsConfiguration(request);

		assertEquals("https://localhost", configuration.checkOrigin("https://localhost"));
		assertEquals("http://localhost", configuration.checkOrigin("http://localhost"));
		assertEquals("capacitor://localhost", configuration.checkOrigin("capacitor://localhost"));
		assertEquals("https://web.example.com", configuration.checkOrigin("https://web.example.com"));
		assertEquals("http://localhost:3000", configuration.checkOrigin("http://localhost:3000"));
		assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
		assertNotNull(configuration.checkHeaders(List.of("Content-Type", "Authorization")));
	}
}

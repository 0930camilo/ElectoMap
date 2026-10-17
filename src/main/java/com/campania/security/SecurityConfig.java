package com.campania.security;

import com.campania.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
		return new ProviderManager(authenticationProvider);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			DaoAuthenticationProvider authenticationProvider
	) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> {})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/login", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							objectMapper.writeValue(response.getOutputStream(),
									ApiResponse.error("Se requiere autenticación."));
						})
						.accessDeniedHandler((request, response, exception) -> {
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							objectMapper.writeValue(response.getOutputStream(),
									ApiResponse.error("No tiene permisos para realizar esta operación."));
						}))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}

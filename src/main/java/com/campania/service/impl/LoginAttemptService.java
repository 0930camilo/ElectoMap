package com.campania.service.impl;

import com.campania.config.LoginAttemptLockInitializer;
import com.campania.entity.LoginAttempt;
import com.campania.repository.LoginAttemptRepository;
import com.campania.security.LoginBlockedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptService.class);
	private static final Duration WINDOW = Duration.ofMinutes(15);
	private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

	private final LoginAttemptRepository loginAttemptRepository;

	@Value("${app.auth.login.max-attempts:5}")
	private int maxAttempts;

	@Value("${security.jwt.secret}")
	private String hashSecret;

	@Transactional
	public void assertAllowed(String username, String clientIp) {
		loginAttemptRepository.findByKeyForUpdate(LoginAttemptLockInitializer.LOCK_KEY)
				.orElseThrow(() -> new IllegalStateException("No existe el bloqueo transaccional de autenticación."));

		Instant now = Instant.now();
		String key = createKey(username);
		Optional<LoginAttempt> storedAttempt = loginAttemptRepository.findByKeyForUpdate(key);
		if (storedAttempt.isPresent() && isLocked(storedAttempt.get(), now)) {
			long retryAfter = Math.max(1, Duration.between(now, storedAttempt.get().getLockedUntil()).toSeconds());
			LOGGER.warn("login_blocked username_key={} ip={} retry_after_seconds={}", key, clientIp, retryAfter);
			throw new LoginBlockedException(retryAfter);
		}
	}

	@Transactional
	public int recordFailure(String username, String clientIp) {
		loginAttemptRepository.findByKeyForUpdate(LoginAttemptLockInitializer.LOCK_KEY)
				.orElseThrow(() -> new IllegalStateException("No existe el bloqueo transaccional de autenticación."));

		Instant now = Instant.now();
		String key = createKey(username);
		LoginAttempt attempt = loginAttemptRepository.findByKeyForUpdate(key)
				.orElseGet(() -> new LoginAttempt(key, now));
		if (attempt.getWindowStartedAt() == null
				|| !now.isBefore(attempt.getWindowStartedAt().plus(WINDOW))) {
			attempt.setFailedAttempts(0);
			attempt.setWindowStartedAt(now);
			attempt.setLockedUntil(null);
		}
		attempt.setFailedAttempts(attempt.getFailedAttempts() + 1);
		if (attempt.getFailedAttempts() >= maxAttempts) {
			attempt.setLockedUntil(now.plus(LOCK_DURATION));
		}
		loginAttemptRepository.save(attempt);
		LOGGER.warn("login_failed username_key={} ip={} attempt={} reason=credentials_rejected",
				key, clientIp, attempt.getFailedAttempts());
		return Math.max(0, maxAttempts - attempt.getFailedAttempts());
	}

	@Transactional
	public int reset(String username) {
		loginAttemptRepository.findByKeyForUpdate(LoginAttemptLockInitializer.LOCK_KEY)
				.orElseThrow(() -> new IllegalStateException("No existe el bloqueo transaccional de autenticación."));
		loginAttemptRepository.deleteById(createKey(username));
		return maxAttempts;
	}

	private boolean isLocked(LoginAttempt attempt, Instant now) {
		if (attempt.getLockedUntil() != null && now.isBefore(attempt.getLockedUntil())) {
			return true;
		}
		if (attempt.getWindowStartedAt() == null
				|| !now.isBefore(attempt.getWindowStartedAt().plus(WINDOW))) {
			attempt.setFailedAttempts(0);
			attempt.setWindowStartedAt(now);
			attempt.setLockedUntil(null);
			loginAttemptRepository.save(attempt);
		}
		return false;
	}

	private String createKey(String username) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(username.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("No se pudo generar la clave de intentos de inicio.", exception);
		}
	}

}

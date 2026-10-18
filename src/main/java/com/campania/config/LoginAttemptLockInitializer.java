package com.campania.config;

import com.campania.entity.LoginAttempt;
import com.campania.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LoginAttemptLockInitializer implements ApplicationRunner {

	public static final String LOCK_KEY = "0000000000000000000000000000000000000000000000000000000000000000";

	private final LoginAttemptRepository loginAttemptRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (loginAttemptRepository.existsById(LOCK_KEY)) {
			return;
		}
		try {
			loginAttemptRepository.saveAndFlush(new LoginAttempt(LOCK_KEY, Instant.now()));
		} catch (DataIntegrityViolationException exception) {
			if (!loginAttemptRepository.existsById(LOCK_KEY)) {
				throw exception;
			}
		}
	}
}

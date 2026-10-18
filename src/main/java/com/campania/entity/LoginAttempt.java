package com.campania.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "login_attempt")
public class LoginAttempt {

	@Id
	@Column(name = "attempt_key", nullable = false, length = 64)
	private String key;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "window_started_at")
	private Instant windowStartedAt;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	public LoginAttempt(String key, Instant now) {
		this.key = key;
		this.failedAttempts = 0;
		this.windowStartedAt = now;
	}
}

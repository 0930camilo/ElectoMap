package com.campania.repository;

import com.campania.entity.LoginAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attempt from LoginAttempt attempt where attempt.key = :key")
	Optional<LoginAttempt> findByKeyForUpdate(@Param("key") String key);
}

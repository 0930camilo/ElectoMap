package com.campania.exception;

import com.campania.dto.ApiResponse;
import com.campania.security.LoginAttemptHeaders;
import com.campania.security.LoginBlockedException;
import com.campania.security.LoginFailedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error("No tiene permisos para realizar esta operación."));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed() {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(ApiResponse.error("El método HTTP no está permitido para esta ruta."));
	}

	@ExceptionHandler({BadCredentialsException.class, DisabledException.class})
	public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailure() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("Credenciales inválidas."));
	}

	@ExceptionHandler(LoginFailedException.class)
	public ResponseEntity<ApiResponse<LoginAttemptInfo>> handleLoginFailed(LoginFailedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.header(LoginAttemptHeaders.REMAINING, Integer.toString(exception.getRemainingAttempts()))
				.body(new ApiResponse<>(false, "Credenciales inválidas.",
						new LoginAttemptInfo(exception.getRemainingAttempts())));
	}

	@ExceptionHandler(LoginBlockedException.class)
	public ResponseEntity<ApiResponse<LoginBlockedInfo>> handleLoginBlocked(LoginBlockedException exception) {
		long retryAfterSeconds = exception.getRetryAfterSeconds();
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
				.header(LoginAttemptHeaders.REMAINING, "0")
				.body(new ApiResponse<>(false, "Demasiados intentos. Intente nuevamente más tarde.",
						new LoginBlockedInfo(0, retryAfterSeconds, retryAfterSeconds / 60,
								retryAfterSeconds % 60)));
	}

	public record LoginAttemptInfo(int remainingAttempts) {
	}

	public record LoginBlockedInfo(
			int remainingAttempts,
			long retryAfterTotalSeconds,
			long retryAfterMinutes,
			long retryAfterSeconds
	) {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage() == null ? "Dato inválido." : error.getDefaultMessage())
				.distinct()
				.collect(Collectors.joining(" "));
		return ResponseEntity.badRequest().body(ApiResponse.error(message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.error("Los datos enviados no son válidos."));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(exception.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
	}

	@ExceptionHandler(OcrUnavailableException.class)
	public ResponseEntity<ApiResponse<Void>> handleOcrUnavailable(OcrUnavailableException exception) {
		LOGGER.error("El servicio OCR no está disponible.", exception);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponse.error(exception.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataConflict(DataIntegrityViolationException exception) {
		if (contieneRestriccion(exception, "uk_votante_admin_documento")) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ApiResponse.error("Ya existe un votante con ese documento en su organización."));
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("La operación entra en conflicto con un registro existente."));
	}

	private boolean contieneRestriccion(Throwable exception, String nombre) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause.getMessage() != null && cause.getMessage().contains(nombre)) {
				return true;
			}
		}
		return false;
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
		LOGGER.error("Error inesperado procesando la solicitud.", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Ocurrió un error interno al procesar la solicitud."));
	}
}

package com.campania;

import com.campania.entity.EstadoUsuario;
import com.campania.entity.LoginAttempt;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.config.LoginAttemptLockInitializer;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.AuditoriaRepository;
import com.campania.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuditoriaRepository auditoriaRepository;

	@Autowired
	private LoginAttemptRepository loginAttemptRepository;

	@BeforeEach
	void prepararUsuario() {
		auditoriaRepository.deleteAll();
		loginAttemptRepository.deleteAll();
		usuarioRepository.deleteAll();
		loginAttemptRepository.saveAndFlush(
				new LoginAttempt(LoginAttemptLockInitializer.LOCK_KEY, java.time.Instant.now()));
		Usuario usuario = new Usuario();
		usuario.setNombres("Admin");
		usuario.setApellidos("Prueba");
		usuario.setUsername("admin-test");
		usuario.setPasswordHash(passwordEncoder.encode("ClaveSegura123!"));
		usuario.setRol(Rol.ADMIN);
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepository.save(usuario);
	}

	@Test
	void loginExitosoDevuelveTokenYUsuario() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin-test","password":"ClaveSegura123!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.type").value("Bearer"))
				.andExpect(jsonPath("$.data.user.rol").value("ADMIN"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("X-Login-Attempts-Remaining", "5"));
	}

	@Test
	void loginIncorrectoDevuelve401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin-test","password":"incorrecta"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Credenciales inválidas."))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("X-Login-Attempts-Remaining", "4"))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void loginBloqueaDespuesDeCincoIntentosFallidos() throws Exception {
		String username = "inexistente-" + java.util.UUID.randomUUID();
		String body = "{\"username\":\"" + username + "\",\"password\":\"incorrecta\"}";

		for (int attempt = 0; attempt < 5; attempt++) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.message").value("Credenciales inválidas."))
					.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
							.string("X-Login-Attempts-Remaining", Integer.toString(4 - attempt)));
		}

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value("Demasiados intentos. Intente nuevamente más tarde."))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("X-Login-Attempts-Remaining", "0"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.exists("Retry-After"));
	}

	@Test
	void inicioExitosoRestableceElContadorDeIntentos() throws Exception {
		for (int attempt = 0; attempt < 4; attempt++) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"username":"admin-test","password":"incorrecta"}
									"""))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin-test","password":"ClaveSegura123!"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin-test","password":"incorrecta"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Credenciales inválidas."));
	}

	@Test
	void loginValidaCamposObligatorios() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"","password":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void administradorPuedeEditarSuPropioPerfilYClaveConValidacionDeClaveActual() throws Exception {
		mockMvc.perform(put("/api/perfil/me")
						.with(user("admin-test").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombres":"Admin Editado",
								  "apellidos":"Perfil Actualizado",
								  "username":"admin-renovado",
								  "email":"admin@example.com",
								  "nuevaClave":"NuevaClaveSegura123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value("admin-renovado"))
				.andExpect(jsonPath("$.data.rol").value("ADMIN"))
				.andExpect(jsonPath("$.data.nuevaClave").doesNotExist());

		Usuario actualizado = usuarioRepository.findByUsername("admin-renovado").orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(
				passwordEncoder.matches("NuevaClaveSegura123!", actualizado.getPasswordHash()));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin-renovado","password":"NuevaClaveSegura123!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.user.username").value("admin-renovado"));
	}

	@Test
	void cambioDeClaveNoRequiereClaveActualConSesionAutenticada() throws Exception {
		mockMvc.perform(put("/api/perfil/me")
						.with(user("admin-test").roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombres":"Admin",
								  "apellidos":"Prueba",
								  "username":"admin-test",
								  "nuevaClave":"NuevaClaveSegura123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value("admin-test"));

		Usuario actualizado = usuarioRepository.findByUsername("admin-test").orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(
				passwordEncoder.matches("NuevaClaveSegura123!", actualizado.getPasswordHash()));
	}
}

package com.campania;

import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.AuditoriaRepository;
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

	@BeforeEach
	void prepararUsuario() {
		auditoriaRepository.deleteAll();
		usuarioRepository.deleteAll();
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
				.andExpect(jsonPath("$.data.user.rol").value("ADMIN"));
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
				.andExpect(jsonPath("$.data").doesNotExist());
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

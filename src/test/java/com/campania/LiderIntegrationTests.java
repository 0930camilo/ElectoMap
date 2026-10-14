package com.campania;

import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Cabecera;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.AuditoriaRepository;
import com.campania.repository.HistorialAsignacionRepository;
import com.campania.repository.VotanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiderIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CabeceraRepository cabeceraRepository;

	@Autowired
	private LiderRepository liderRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AuditoriaRepository auditoriaRepository;

	@Autowired
	private HistorialAsignacionRepository historialRepository;

	@Autowired
	private VotanteRepository votanteRepository;

	private Long cabeceraId;

	@BeforeEach
	void prepararCabecera() {
		auditoriaRepository.deleteAll();
		historialRepository.deleteAll();
		votanteRepository.deleteAll();
		liderRepository.deleteAll();
		cabeceraRepository.deleteAll();
		usuarioRepository.deleteAll();
		Usuario admin = new Usuario();
		admin.setNombres("Admin");
		admin.setApellidos("Prueba");
		admin.setUsername("test-admin");
		admin.setPasswordHash("not-used-in-this-test");
		admin.setRol(Rol.ADMIN);
		admin.setEstado(EstadoUsuario.ACTIVO);
		admin = usuarioRepository.save(admin);

		Cabecera cabecera = new Cabecera();
		cabecera.setNombre("Centro");
		cabecera.setEstado(EstadoCabecera.ACTIVA);
		cabecera.setUsuario(admin);
		cabeceraId = cabeceraRepository.save(cabecera).getId();
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminPuedeCrearLiderConUsuarioProtegido() throws Exception {
		mockMvc.perform(post("/api/lideres")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombres":"Ana",
								  "apellidos":"Pérez",
								  "documento":"123456789",
								  "telefono":"3001234567",
								  "email":"ana@example.com",
								  "username":"ana.lider",
								  "password":"ClaveSegura123!",
								  "cabeceraId":%d
								}
								""".formatted(cabeceraId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.username").value("ana.lider"))
				.andExpect(jsonPath("$.data.cabeceraNombre").value("Centro"))
				.andExpect(jsonPath("$.data.password").doesNotExist());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void cabeceraPropiaPermiteVariosLideres() throws Exception {
		for (int index = 1; index <= 2; index++) {
			mockMvc.perform(post("/api/lideres")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "nombres":"Líder",
									  "apellidos":"Número %d",
									  "documento":"DOC00000%d",
									  "username":"lider.numero%d",
									  "password":"ClaveSegura123!",
									  "cabeceraId":%d
									}
									""".formatted(index, index, index, cabeceraId)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.cabeceraId").value(cabeceraId));
		}
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.get("/api/lideres?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminNoPuedeAsignarLiderACabeceraDeOtroAdministrador() throws Exception {
		Usuario otroAdmin = new Usuario();
		otroAdmin.setNombres("Otro");
		otroAdmin.setApellidos("Administrador");
		otroAdmin.setUsername("otro-admin");
		otroAdmin.setPasswordHash("not-used-in-this-test");
		otroAdmin.setRol(Rol.ADMIN);
		otroAdmin.setEstado(EstadoUsuario.ACTIVO);
		otroAdmin = usuarioRepository.save(otroAdmin);

		Cabecera cabeceraAjena = new Cabecera();
		cabeceraAjena.setNombre("Ajena");
		cabeceraAjena.setEstado(EstadoCabecera.ACTIVA);
		cabeceraAjena.setUsuario(otroAdmin);
		Long cabeceraAjenaId = cabeceraRepository.save(cabeceraAjena).getId();

		mockMvc.perform(post("/api/lideres")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombres":"Ana",
								  "apellidos":"Pérez",
								  "documento":"DOC-AJENO",
								  "username":"ana.ajena",
								  "password":"ClaveSegura123!",
								  "cabeceraId":%d
								}
								""".formatted(cabeceraAjenaId)))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void noPermiteCrearLiderEnCabeceraInactiva() throws Exception {
		Cabecera cabecera = cabeceraRepository.findById(cabeceraId).orElseThrow();
		cabecera.setEstado(EstadoCabecera.INACTIVA);
		cabeceraRepository.save(cabecera);

		mockMvc.perform(post("/api/lideres")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "nombres":"Ana",
								  "apellidos":"Pérez",
								  "documento":"123456789",
								  "username":"ana.lider",
								  "password":"ClaveSegura123!",
								  "cabeceraId":%d
								}
								""".formatted(cabeceraId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}
}

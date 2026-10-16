package com.campania;

import com.campania.repository.CabeceraRepository;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.AuditoriaRepository;
import com.campania.repository.HistorialAsignacionRepository;
import com.campania.repository.LiderRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CabeceraIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CabeceraRepository cabeceraRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private AuditoriaRepository auditoriaRepository;

	@Autowired
	private HistorialAsignacionRepository historialRepository;

	@Autowired
	private VotanteRepository votanteRepository;

	@Autowired
	private LiderRepository liderRepository;

	@BeforeEach
	void limpiarCabeceras() {
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
		usuarioRepository.save(admin);
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminPuedeCrearYConsultarCabecera() throws Exception {
		mockMvc.perform(post("/api/cabeceras")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"Centro","descripcion":"Zona central","zona":"Urbana"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.nombre").value("Centro"))
				.andExpect(jsonPath("$.data.administradorUsername").value("test-admin"))
				.andExpect(jsonPath("$.data.estado").value("ACTIVA"));

		mockMvc.perform(get("/api/cabeceras?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1));

		mockMvc.perform(post("/api/cabeceras")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":"Norte","descripcion":"Zona norte","zona":"Rural"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.administradorUsername").value("test-admin"));
		mockMvc.perform(get("/api/cabeceras?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void administradorPuedeFiltrarCabecerasPorNombreParcialSinDistinguirMayusculas() throws Exception {
		for (String nombre : new String[]{"Centro Norte", "Centro Sur", "Occidente"}) {
			mockMvc.perform(post("/api/cabeceras")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"nombre":"%s","descripcion":"Zona","zona":"Urbana"}
									""".formatted(nombre)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/cabeceras?page=0&size=10&nombre=centro"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.content[0].nombre").value("Centro Norte"))
				.andExpect(jsonPath("$.data.content[1].nombre").value("Centro Sur"));

		mockMvc.perform(get("/api/cabeceras?page=0&size=10").param("nombre", "Norte"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].nombre").value("Centro Norte"));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void administradorSoloConsultaSusCabeceras() throws Exception {
		Usuario otroAdmin = new Usuario();
		otroAdmin.setNombres("Otro");
		otroAdmin.setApellidos("Admin");
		otroAdmin.setUsername("otro-admin");
		otroAdmin.setPasswordHash("not-used-in-this-test");
		otroAdmin.setRol(Rol.ADMIN);
		otroAdmin.setEstado(EstadoUsuario.ACTIVO);
		otroAdmin = usuarioRepository.save(otroAdmin);

		com.campania.entity.Cabecera ajena = new com.campania.entity.Cabecera();
		ajena.setNombre("Cabecera ajena");
		ajena.setEstado(com.campania.entity.EstadoCabecera.ACTIVA);
		ajena.setUsuario(otroAdmin);
		Long cabeceraAjenaId = cabeceraRepository.save(ajena).getId();

		mockMvc.perform(get("/api/cabeceras?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(0));
		mockMvc.perform(get("/api/cabeceras/{id}", cabeceraAjenaId))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "LIDER")
	void liderNoPuedeAdministrarCabeceras() throws Exception {
		mockMvc.perform(get("/api/cabeceras"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void crearCabeceraValidaElNombre() throws Exception {
		mockMvc.perform(post("/api/cabeceras")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombre":" ","descripcion":"","zona":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
	}
}

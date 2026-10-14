package com.campania;

import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.repository.AuditoriaRepository;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.HistorialAsignacionRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.VotanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VotanteIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CabeceraRepository cabeceraRepository;

	@Autowired
	private LiderRepository liderRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private VotanteRepository votanteRepository;

	@Autowired
	private HistorialAsignacionRepository historialRepository;

	@Autowired
	private AuditoriaRepository auditoriaRepository;

	private Long primerLiderId;
	private Long segundoLiderId;

	@BeforeEach
	void prepararDatos() {
		historialRepository.deleteAll();
		auditoriaRepository.deleteAll();
		votanteRepository.deleteAll();
		liderRepository.deleteAll();
		cabeceraRepository.deleteAll();
		usuarioRepository.deleteAll();

		Cabecera cabecera = new Cabecera();
		cabecera.setNombre("Centro");
		cabecera.setEstado(EstadoCabecera.ACTIVA);
		Usuario admin = nuevoUsuario("test-admin", Rol.ADMIN);
		admin = usuarioRepository.save(admin);
		cabecera.setUsuario(admin);
		cabecera = cabeceraRepository.save(cabecera);
		Usuario usuarioLiderUno = usuarioRepository.save(nuevoUsuario("test-leader", Rol.LIDER));
		Usuario usuarioLiderDos = usuarioRepository.save(nuevoUsuario("other-leader", Rol.LIDER));
		primerLiderId = liderRepository.save(nuevoLider(usuarioLiderUno, cabecera, "100000001")).getId();
		segundoLiderId = liderRepository.save(nuevoLider(usuarioLiderDos, cabecera, "100000002")).getId();
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void registraVotanteAsociadoAutomaticamenteAlLiderAutenticado() throws Exception {
		mockMvc.perform(post("/api/votantes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "tipoDocumento":"CC",
								  "numeroDocumento":"1234567890",
								  "nombres":"Juan",
								  "apellidos":"Pérez",
								  "mesaVotacion":"12",
								  "estado":"INTERESADO"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.numeroDocumento").value("1234567890"))
				.andExpect(jsonPath("$.data.mesaVotacion").value("12"))
				.andExpect(jsonPath("$.data.liderId").value(primerLiderId));
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void rechazaDocumentoDuplicadoConConflictoInclusoEntreLideres() throws Exception {
		crearVotante("88888888");

		mockMvc.perform(post("/api/votantes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tipoDocumento":"CC","numeroDocumento":"88888888","nombres":"Otra","apellidos":"Persona","mesaVotacion":"12"}
								""")
						.with(user("other-leader").roles("LIDER")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value(
						"Esta persona ya se encuentra registrada en el sistema y está asignada a otro líder."));
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void liderNoPuedeConsultarRegistrosDeOtroLider() throws Exception {
		Long votanteId = crearVotante("77777777");

		mockMvc.perform(get("/api/votantes/{id}", votanteId)
						.with(user("other-leader").roles("LIDER")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminPuedeTransferirVotanteYQuedaHistorialAuditado() throws Exception {
		Long votanteId = crearVotante("66666666");

		mockMvc.perform(put("/api/votantes/{id}/lider", votanteId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"liderId":%d,"motivo":"Reorganización territorial"}
								""".formatted(segundoLiderId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.liderId").value(segundoLiderId));

		org.junit.jupiter.api.Assertions.assertEquals(1, historialRepository.count());
		org.junit.jupiter.api.Assertions.assertTrue(auditoriaRepository.count() >= 2);
		mockMvc.perform(get("/api/votantes/{id}/historial-asignaciones", votanteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].liderAnteriorId").value(primerLiderId))
				.andExpect(jsonPath("$.data[0].liderNuevoId").value(segundoLiderId));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminEliminaLogicamenteVotanteYDejaDeEstarDisponible() throws Exception {
		Long votanteId = crearVotante("10101010");
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/api/votantes/{id}", votanteId))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/votantes/{id}", votanteId))
				.andExpect(status().isNotFound());
		org.junit.jupiter.api.Assertions.assertEquals(1, votanteRepository.count());
		org.junit.jupiter.api.Assertions.assertEquals(0, votanteRepository.contarVigentes(null));
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void liderNoPuedeTransferirVotante() throws Exception {
		Long votanteId = crearVotante("55555555");
		mockMvc.perform(put("/api/votantes/{id}/lider", votanteId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"liderId":%d,"motivo":"Intento"}
								""".formatted(segundoLiderId)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void dashboardAdminIncluyeEstadisticasGlobales() throws Exception {
		crearVotante("44444444");
		mockMvc.perform(get("/api/dashboard/admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCabeceras").value(1))
				.andExpect(jsonPath("$.data.totalLideres").value(2))
				.andExpect(jsonPath("$.data.totalVotantes").value(1))
				.andExpect(jsonPath("$.data.registrosHoy").value(1))
				.andExpect(jsonPath("$.data.porCabecera[0].total").value(1));
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void dashboardLiderSoloIncluyeSusPropiasEstadisticas() throws Exception {
		crearVotante("33333333");
		mockMvc.perform(get("/api/dashboard/lider"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalVotantes").value(1))
				.andExpect(jsonPath("$.data.liderId").value(primerLiderId))
				.andExpect(jsonPath("$.data.cabeceraNombre").value("Centro"));
		mockMvc.perform(get("/api/dashboard/admin"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void auditoriaEsSoloLecturaYRestringidaAAdmin() throws Exception {
		crearVotante("22222222");
		mockMvc.perform(get("/api/auditoria?page=0&size=10&entidad=VOTANTE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].accion").value("CREAR"));
		mockMvc.perform(post("/api/auditoria")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isMethodNotAllowed());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void previewImportacionDetectaDuplicadosAntesDeGuardar() throws Exception {
		MockMultipartFile archivo = crearArchivoCsv("""
				tipoDocumento,numeroDocumento,nombres,apellidos,mesaVotacion
				CC,12345678,Ana,Gómez,12
				CC,12345678,Otra,Persona,12
				""");

		mockMvc.perform(multipart("/api/votantes/importaciones/validar").file(archivo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalFilas").value(2))
				.andExpect(jsonPath("$.data.filasValidas").value(1))
				.andExpect(jsonPath("$.data.filasRechazadas").value(1))
				.andExpect(jsonPath("$.data.errores[0].fila").value(3));
		org.junit.jupiter.api.Assertions.assertEquals(0, votanteRepository.count());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void descargaReporteCsvDeFilasRechazadas() throws Exception {
		MockMultipartFile archivo = crearArchivoCsv("""
				tipoDocumento,numeroDocumento,nombres,apellidos,mesaVotacion
				CC,12345678,Ana,Gómez,12
				CC,12345678,Otra,Persona,12
				""");

		mockMvc.perform(multipart("/api/votantes/importaciones/reporte-errores").file(archivo))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.content().contentTypeCompatibleWith("text/csv"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.header().string("Content-Disposition", "attachment; filename=\"errores-importacion.csv\""))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
						.content().string(org.hamcrest.Matchers.containsString("Documento duplicado dentro del archivo.")));
		org.junit.jupiter.api.Assertions.assertEquals(0, votanteRepository.count());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminImportaFilasValidasYDevuelveErroresRechazados() throws Exception {
		MockMultipartFile archivo = crearArchivoCsv("""
				tipoDocumento,numeroDocumento,nombres,apellidos,mesaVotacion
				CC,12121212,Ana,Gómez,12
				CC,12121212,Otra,Persona,12
				""");

		mockMvc.perform(multipart("/api/votantes/importaciones")
						.file(archivo)
						.param("liderId", primerLiderId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalFilas").value(2))
				.andExpect(jsonPath("$.data.importados").value(1))
				.andExpect(jsonPath("$.data.rechazados").value(1));
		org.junit.jupiter.api.Assertions.assertEquals(1, votanteRepository.count());
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void liderNoPuedeImportarVotantes() throws Exception {
		mockMvc.perform(multipart("/api/votantes/importaciones/validar")
						.file(crearArchivoCsv("""
								tipoDocumento,numeroDocumento,nombres,apellidos,mesaVotacion
								CC,88888888,Ana,Gómez,12
								""")))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminPuedeCrearYConsultarOtroAdministradorSinExponerHash() throws Exception {
		String response = mockMvc.perform(post("/api/usuarios/administradores")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nombres":"Nueva","apellidos":"Administradora","username":"admin-nueva",
								 "password":"UnaClaveSegura123","email":"admin@example.com"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.rol").value("ADMIN"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		Long usuarioId = new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(response).path("data").path("id").asLong();

		mockMvc.perform(get("/api/usuarios?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(4));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.patch("/api/usuarios/{id}/estado", usuarioId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.estado").value("INACTIVO"));
	}

	private Long crearVotante(String documento) throws Exception {
		String response = mockMvc.perform(post("/api/votantes")
						.with(user("test-leader").roles("LIDER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tipoDocumento":"CC","numeroDocumento":"%s","nombres":"Juan","apellidos":"Pérez","mesaVotacion":"12"}
								""".formatted(documento)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(response).path("data").path("id").asText();
		return Long.valueOf(id);
	}

	private MockMultipartFile crearArchivoCsv(String contenido) {
		return new MockMultipartFile(
				"archivo",
				"votantes.csv",
				"text/csv",
				contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private Usuario nuevoUsuario(String username, Rol rol) {
		Usuario usuario = new Usuario();
		usuario.setNombres("Usuario");
		usuario.setApellidos("Prueba");
		usuario.setUsername(username);
		usuario.setPasswordHash("not-used-in-mock-authentication");
		usuario.setRol(rol);
		usuario.setEstado(EstadoUsuario.ACTIVO);
		return usuario;
	}

	private Lider nuevoLider(Usuario usuario, Cabecera cabecera, String documento) {
		Lider lider = new Lider();
		lider.setUsuario(usuario);
		lider.setCabecera(cabecera);
		lider.setNombres("Lider");
		lider.setApellidos("Prueba");
		lider.setDocumento(documento);
		lider.setEstado(EstadoLider.ACTIVO);
		return lider;
	}
}

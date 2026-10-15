package com.campania;

import com.campania.entity.Cabecera;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.EstadoUsuario;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.TipoDocumento;
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
								  "departamentoVotacion":"Córdoba",
								  "municipioVotacion":"Moñitos",
								  "puestoVotacion":"I.E. OBDULIO MAYO SCARPETA",
								  "direccion":"CALLE 22B N° 9-17",
								  "mesaVotacion":"12",
								  "estado":"INTERESADO"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.numeroDocumento").value("1234567890"))
				.andExpect(jsonPath("$.data.departamentoVotacion").value("Córdoba"))
				.andExpect(jsonPath("$.data.municipioVotacion").value("Moñitos"))
				.andExpect(jsonPath("$.data.puestoVotacion").value("I.E. OBDULIO MAYO SCARPETA"))
				.andExpect(jsonPath("$.data.direccion").value("CALLE 22B N° 9-17"))
				.andExpect(jsonPath("$.data.email").doesNotExist())
				.andExpect(jsonPath("$.data.mesaVotacion").value("12"))
				.andExpect(jsonPath("$.data.liderId").value(primerLiderId));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void administradorCreaVotanteYLoAsignaPorDocumentoDelLider() throws Exception {
		mockMvc.perform(post("/api/votantes/administrador")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "liderDocumento":"100000001",
								  "votante":{
								    "tipoDocumento":"CC",
								    "numeroDocumento":"12004567",
								    "nombres":"Ana",
								    "apellidos":"Pérez",
								    "departamentoVotacion":"Córdoba",
								    "municipioVotacion":"Moñitos",
								    "puestoVotacion":"I.E. OBDULIO MAYO SCARPETA",
								    "direccion":"CALLE 22B N° 9-17",
								    "mesaVotacion":"15"
								  }
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.numeroDocumento").value("12004567"))
				.andExpect(jsonPath("$.data.liderId").value(primerLiderId))
				.andExpect(jsonPath("$.data.liderNombre").value("Lider Prueba"))
				.andExpect(jsonPath("$.data.puestoVotacion").value("I.E. OBDULIO MAYO SCARPETA"));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void administradorNoPuedeAsignarVotanteAUnLiderDeOtraOrganizacion() throws Exception {
		crearLiderDeOtroAdministrador("lider-ajeno-asignacion");

		mockMvc.perform(post("/api/votantes/administrador")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "liderDocumento":"100000099",
								  "votante":{
								    "tipoDocumento":"CC",
								    "numeroDocumento":"12004568",
								    "nombres":"Otra",
								    "apellidos":"Persona",
								    "mesaVotacion":"15"
								  }
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
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
						"Ya existe un votante con ese documento en su organización."));
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void documentosDeVotantesSeAislanPorAdministradorEnConsultasYDuplicidad() throws Exception {
		Long votantePropioId = crearVotante("81818181");
		Long liderAjenoId = crearLiderDeOtroAdministrador("lider-ajeno-duplicado");
		String respuestaAjena = mockMvc.perform(post("/api/votantes")
						.with(user("lider-ajeno-duplicado").roles("LIDER"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tipoDocumento":"CC","numeroDocumento":"81818181","nombres":"Persona",
								 "apellidos":"De otra organización","mesaVotacion":"20"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.liderId").value(liderAjenoId))
				.andReturn().getResponse().getContentAsString();
		Long votanteAjenoId = new com.fasterxml.jackson.databind.ObjectMapper()
				.readTree(respuestaAjena).path("data").path("id").asLong();

		mockMvc.perform(get("/api/votantes?page=0&size=10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].id").value(votantePropioId));
		mockMvc.perform(get("/api/votantes/{id}", votanteAjenoId))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/votantes/documento/{documento}", "81818181"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(votantePropioId));
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
								{"liderDocumento":"100000002","motivo":"Reorganización territorial"}
								"""))
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
	void administradorNoPuedeReasignarAVotanteAUnLiderDeOtraOrganizacionPorDocumento() throws Exception {
		Long votanteId = crearVotante("66660000");
		crearLiderDeOtroAdministrador("lider-ajeno-transferencia");

		mockMvc.perform(put("/api/votantes/{id}/lider", votanteId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"liderDocumento":"100000099","motivo":"Cambio de responsable"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
		org.junit.jupiter.api.Assertions.assertEquals(primerLiderId,
				votanteRepository.findById(votanteId).orElseThrow().getLider().getId());
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
								{"liderDocumento":"100000002","motivo":"Intento"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void dashboardAdminSoloIncluyeDatosDeSusCabeceras() throws Exception {
		crearVotante("44444444");
		crearOrganizacionDeOtroAdministradorConVotante("99990000");
		mockMvc.perform(get("/api/dashboard/admin"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCabeceras").value(1))
				.andExpect(jsonPath("$.data.totalLideres").value(2))
				.andExpect(jsonPath("$.data.totalVotantes").value(1))
				.andExpect(jsonPath("$.data.registrosHoy").value(1))
				.andExpect(jsonPath("$.data.porCabecera.length()").value(1))
				.andExpect(jsonPath("$.data.porCabecera[0].nombre").value("Centro"))
				.andExpect(jsonPath("$.data.porCabecera[0].total").value(1))
				.andExpect(jsonPath("$.data.porLider.length()").value(2));
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
		Usuario otroAdmin = usuarioRepository.save(nuevoUsuario("admin-auditoria-ajena", Rol.ADMIN));
		com.campania.entity.Auditoria auditoriaAjena = new com.campania.entity.Auditoria();
		auditoriaAjena.setUsuario(otroAdmin);
		auditoriaAjena.setAccion(com.campania.entity.AccionAuditoria.CREAR);
		auditoriaAjena.setEntidad("VOTANTE");
		auditoriaAjena.setEntidadId(987654L);
		auditoriaAjena.setDescripcion("Registro de otra organización.");
		auditoriaAjena.setFecha(java.time.LocalDateTime.now());
		auditoriaRepository.save(auditoriaAjena);
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

		mockMvc.perform(multipart("/api/votantes/importaciones/validar")
						.file(archivo)
						.param("liderId", primerLiderId.toString()))
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

		mockMvc.perform(multipart("/api/votantes/importaciones/reporte-errores")
						.file(archivo)
						.param("liderId", primerLiderId.toString()))
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
				tipoDocumento,numeroDocumento,nombres,apellidos,departamento,municipio,puesto,direccion,mesaVotacion
				CC,12121212,Ana,Gómez,Córdoba,Moñitos,I.E. OBDULIO MAYO SCARPETA,CALLE 22B N° 9-17,12
				CC,12121212,Otra,Persona,Córdoba,Moñitos,I.E. OBDULIO MAYO SCARPETA,CALLE 22B N° 9-17,12
				""");

		mockMvc.perform(multipart("/api/votantes/importaciones")
						.file(archivo)
						.param("liderId", primerLiderId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalFilas").value(2))
				.andExpect(jsonPath("$.data.importados").value(1))
				.andExpect(jsonPath("$.data.rechazados").value(1));
		org.junit.jupiter.api.Assertions.assertEquals(1, votanteRepository.count());
		com.campania.entity.Votante importado = votanteRepository.findAll().get(0);
		org.junit.jupiter.api.Assertions.assertEquals("Córdoba", importado.getDepartamentoVotacion());
		org.junit.jupiter.api.Assertions.assertEquals("Moñitos", importado.getMunicipioVotacion());
		org.junit.jupiter.api.Assertions.assertEquals("I.E. OBDULIO MAYO SCARPETA", importado.getPuestoVotacion());
		org.junit.jupiter.api.Assertions.assertEquals("CALLE 22B N° 9-17", importado.getDireccion());
	}

	@Test
	@WithMockUser(username = "test-leader", roles = "LIDER")
	void liderNoPuedeImportarVotantes() throws Exception {
		mockMvc.perform(multipart("/api/votantes/importaciones/validar")
						.file(crearArchivoCsv("""
								tipoDocumento,numeroDocumento,nombres,apellidos,mesaVotacion
								CC,88888888,Ana,Gómez,12
								"""))
						.param("liderId", primerLiderId.toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "test-admin", roles = "ADMIN")
	void adminPuedeCrearOtroAdministradorPeroNoConsultarNiAdministrarSuCuenta() throws Exception {
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
				.andExpect(jsonPath("$.data.totalElements").value(3));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.patch("/api/usuarios/{id}/estado", usuarioId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"INACTIVO\"}"))
				.andExpect(status().isNotFound());
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

	private Long crearOrganizacionDeOtroAdministradorConVotante(String documento) {
		Usuario otroAdmin = usuarioRepository.save(nuevoUsuario("otro-admin-dashboard", Rol.ADMIN));
		Cabecera otraCabecera = new Cabecera();
		otraCabecera.setNombre("Cabecera ajena");
		otraCabecera.setEstado(EstadoCabecera.ACTIVA);
		otraCabecera.setUsuario(otroAdmin);
		otraCabecera = cabeceraRepository.save(otraCabecera);

		Usuario usuarioOtroLider = usuarioRepository.save(nuevoUsuario("lider-ajeno-dashboard", Rol.LIDER));
		Lider otroLider = nuevoLider(usuarioOtroLider, otraCabecera, "100000003");
		otroLider = liderRepository.save(otroLider);

		com.campania.entity.Votante otroVotante = new com.campania.entity.Votante();
		otroVotante.setTipoDocumento(TipoDocumento.CC);
		otroVotante.setNumeroDocumento(documento);
		otroVotante.setNombres("Votante");
		otroVotante.setApellidos("Ajeno");
		otroVotante.setMesaVotacion("20");
		otroVotante.setEstado(com.campania.entity.EstadoVotante.CONFIRMADO);
		otroVotante.setLider(otroLider);
		otroVotante.setAdministradorId(otroAdmin.getId());
		return votanteRepository.save(otroVotante).getId();
	}

	private Long crearLiderDeOtroAdministrador(String username) {
		Usuario otroAdmin = usuarioRepository.save(nuevoUsuario(username + "-admin", Rol.ADMIN));
		Cabecera otraCabecera = new Cabecera();
		otraCabecera.setNombre("Cabecera " + username);
		otraCabecera.setEstado(EstadoCabecera.ACTIVA);
		otraCabecera.setUsuario(otroAdmin);
		otraCabecera = cabeceraRepository.save(otraCabecera);
		Usuario usuarioOtroLider = usuarioRepository.save(nuevoUsuario(username, Rol.LIDER));
		return liderRepository.save(nuevoLider(usuarioOtroLider, otraCabecera, "100000099")).getId();
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
		lider.setAdministradorId(cabecera.getUsuario().getId());
		lider.setNombres("Lider");
		lider.setApellidos("Prueba");
		lider.setDocumento(documento);
		lider.setEstado(EstadoLider.ACTIVO);
		return lider;
	}
}

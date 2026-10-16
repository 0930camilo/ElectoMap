package com.campania.service.impl;

import com.campania.service.impl.OcrCensoServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrCensoMapperTests {

	@Test
	void asignaPalabrasDeLaFilaALasColumnasDelEncabezado() {
		List<OcrCensoServiceImpl.OcrToken> tokens = List.of(
				token("NUIP", 20, 10, 50, 15),
				token("DEPARTAMENTO", 120, 10, 100, 15),
				token("MUNICIPIO", 250, 10, 90, 15),
				token("PUESTO", 365, 10, 80, 15),
				token("DIRECCIÓN", 465, 10, 80, 15),
				token("MESA", 570, 10, 50, 15),
				token("1003100736", 20, 50, 50, 15),
				token("CÓRDOBA", 120, 50, 100, 15),
				token("MOÑITOS", 250, 50, 90, 15),
				token("I.E.", 365, 48, 30, 15),
				token("OBDULIO", 365, 65, 60, 15),
				token("MAYO", 365, 82, 45, 15),
				token("SCARPETA", 365, 99, 70, 15),
				token("CALLE", 465, 50, 45, 15),
				token("22B", 465, 67, 30, 15),
				token("15", 570, 50, 20, 15)
		);

		var resultado = OcrCensoServiceImpl.mapear("NUIP DEPARTAMENTO MUNICIPIO PUESTO DIRECCIÓN MESA", tokens);

		assertEquals("1003100736", resultado.numeroDocumento());
		assertEquals("CÓRDOBA", resultado.departamentoVotacion());
		assertEquals("MOÑITOS", resultado.municipioVotacion());
		assertEquals("I.E. OBDULIO MAYO SCARPETA", resultado.puestoVotacion());
		assertEquals("CALLE 22B", resultado.direccion());
		assertEquals("15", resultado.mesaVotacion());
		assertTrue(resultado.confianza() > 0);
	}

	@Test
	void recuperaMesaCuandoFaltaSuCajaDeEncabezadoPeroEstaEnTextoReconocido() {
		List<OcrCensoServiceImpl.OcrToken> tokens = List.of(
				token("NUIP", 20, 10, 50, 15),
				token("DEPARTAMENTO", 120, 10, 100, 15),
				token("MUNICIPIO", 250, 10, 90, 15),
				token("PUESTO", 365, 10, 80, 15),
				token("DIRECCIÓN", 465, 10, 80, 15),
				token("1003100736", 20, 50, 50, 15),
				token("CÓRDOBA", 120, 50, 100, 15),
				token("MOÑITOS", 250, 50, 90, 15),
				token("OBDULIO", 365, 50, 60, 15),
				token("CALLE", 465, 50, 45, 15),
				token("22B", 465, 67, 30, 15),
				token("15", 645, 50, 20, 15)
		);

		var resultado = OcrCensoServiceImpl.mapear(
				"NUIP DEPARTAMENTO MUNICIPIO PUESTO DIRECCIÓN MESA", tokens, 687);

		assertEquals("CALLE 22B", resultado.direccion());
		assertEquals("15", resultado.mesaVotacion());
	}

	@Test
	void normalizaAbreviaturaYGuionDeDireccion() {
		assertEquals("CALLE 22B Nº 9-17",
				OcrCensoServiceImpl.normalizarDireccion("CALLE 22B Nª 9 - 17"));
	}

	private static OcrCensoServiceImpl.OcrToken token(String text, int x, int y, int width, int height) {
		return new OcrCensoServiceImpl.OcrToken(text, x, y, width, height, 95);
	}
}

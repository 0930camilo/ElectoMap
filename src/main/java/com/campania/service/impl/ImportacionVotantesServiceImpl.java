package com.campania.service.impl;

import com.campania.dto.votante.ImportacionError;
import com.campania.dto.votante.ImportacionPreviewResponse;
import com.campania.dto.votante.ImportacionResponse;
import com.campania.dto.votante.VotanteCreateRequest;
import com.campania.entity.EstadoVotante;
import com.campania.entity.TipoDocumento;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ConflictException;
import com.campania.repository.VotanteRepository;
import com.campania.service.ImportacionVotantesService;
import com.campania.service.VotanteService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportacionVotantesServiceImpl implements ImportacionVotantesService {

	private static final Set<String> COLUMNAS_REQUERIDAS = Set.of(
			"tipodocumento", "numerodocumento", "nombres", "apellidos", "mesavotacion");

	private final Validator validator;
	private final VotanteRepository votanteRepository;
	private final VotanteService votanteService;

	@Override
	public ImportacionPreviewResponse validar(MultipartFile archivo, Long liderId) {
		List<ImportRow> filas = leerArchivo(archivo);
		long administradorId = votanteService.validarDestinoImportacion(liderId);
		ValidationResult validation = validarFilas(filas, administradorId);
		return new ImportacionPreviewResponse(
				filas.size(),
				validation.validas().size(),
				validation.errores().size(),
				validation.errores());
	}

	private ImportRow construirFila(int numero, List<String> headers, List<String> values) {
		try {
			return new ImportRow(numero, construirRequest(headers, values), null);
		} catch (BusinessRuleException exception) {
			return new ImportRow(numero, null, exception.getMessage());
		}
	}

	@Override
	public ImportacionResponse importar(MultipartFile archivo, Long liderId) {
		long administradorId = votanteService.validarDestinoImportacion(liderId);
		List<ImportRow> filas = leerArchivo(archivo);
		ValidationResult validation = validarFilas(filas, administradorId);
		List<ImportacionError> errores = new ArrayList<>(validation.errores());
		int importados = 0;

		for (ImportRow fila : validation.validas()) {
			try {
				votanteService.registrarParaLiderImportacion(liderId, fila.request());
				importados++;
			} catch (ConflictException | DataIntegrityViolationException exception) {
				if (exception instanceof DataIntegrityViolationException dataIntegrityViolationException
						&& !esConflictoDocumento(dataIntegrityViolationException)) {
					throw exception;
				}
				errores.add(new ImportacionError(fila.numero(),
						"El documento ya existe en la organización de destino."));
			}
		}

		return new ImportacionResponse(filas.size(), importados, errores.size(), List.copyOf(errores));
	}

	@Override
	public byte[] reporteErrores(MultipartFile archivo, Long liderId) {
		long administradorId = votanteService.validarDestinoImportacion(liderId);
		ValidationResult validation = validarFilas(leerArchivo(archivo), administradorId);
		StringBuilder csv = new StringBuilder("\uFEFFfila,mensaje\r\n");
		for (ImportacionError error : validation.errores()) {
			csv.append(error.fila()).append(',')
					.append(escaparCsv(error.mensaje()))
					.append("\r\n");
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String escaparCsv(String valor) {
		return "\"" + valor.replace("\"", "\"\"") + "\"";
	}

	private boolean esConflictoDocumento(DataIntegrityViolationException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause.getMessage() != null
					&& cause.getMessage().toLowerCase(Locale.ROOT).contains("uk_votante_admin_documento")) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}

	private ValidationResult validarFilas(List<ImportRow> filas, long administradorId) {
		List<ImportRow> validas = new ArrayList<>();
		List<ImportacionError> errores = new ArrayList<>();
		Set<String> documentosArchivo = new HashSet<>();

		for (ImportRow fila : filas) {
			if (fila.error() != null) {
				errores.add(new ImportacionError(fila.numero(), fila.error()));
				continue;
			}
			VotanteCreateRequest request = fila.request();
			String documento = request.numeroDocumento() == null
					? ""
					: request.numeroDocumento().trim().toUpperCase(Locale.ROOT);
			if (!documento.isEmpty() && !documentosArchivo.add(documento)) {
				errores.add(new ImportacionError(fila.numero(), "Documento duplicado dentro del archivo."));
				continue;
			}
			Set<ConstraintViolation<VotanteCreateRequest>> violations = validator.validate(request);
			if (!violations.isEmpty()) {
				String message = violations.stream()
						.map(ConstraintViolation::getMessage)
						.distinct()
						.sorted()
						.reduce((first, second) -> first + " " + second)
						.orElse("La fila contiene datos inválidos.");
				errores.add(new ImportacionError(fila.numero(), message));
				continue;
			}
			if (votanteRepository.existsByNumeroDocumentoAndAdministradorId(documento, administradorId)) {
				errores.add(new ImportacionError(fila.numero(),
						"El documento ya existe en la organización de destino."));
				continue;
			}
			validas.add(fila);
		}
		return new ValidationResult(validas, errores);
	}

	private List<ImportRow> leerArchivo(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new BusinessRuleException("Debe adjuntar un archivo CSV o XLSX no vacío.");
		}
		String nombre = archivo.getOriginalFilename();
		if (nombre == null) {
			throw new BusinessRuleException("El archivo debe tener extensión .csv o .xlsx.");
		}
		String lowerName = nombre.toLowerCase(Locale.ROOT);
		try {
			if (lowerName.endsWith(".csv")) {
				return leerCsv(archivo);
			}
			if (lowerName.endsWith(".xlsx")) {
				return leerExcel(archivo);
			}
			throw new BusinessRuleException("Formato de archivo no permitido. Use CSV o XLSX.");
		} catch (IOException exception) {
			throw new BusinessRuleException("No fue posible leer el archivo adjunto.");
		}
	}

	private List<ImportRow> leerCsv(MultipartFile archivo) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
			String headerLine = reader.readLine();
			if (headerLine == null) {
				throw new BusinessRuleException("El archivo CSV no contiene encabezados.");
			}
			headerLine = headerLine.replace("\uFEFF", "");
			char delimiter = contar(headerLine, ';') > contar(headerLine, ',') ? ';' : ',';
			List<String> headers = parseCsvLine(headerLine, delimiter).stream().map(this::normalizarHeader).toList();
			validarEncabezados(headers);

			List<ImportRow> rows = new ArrayList<>();
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank()) {
					continue;
				}
				rows.add(construirFila(lineNumber, headers, parseCsvLine(line, delimiter)));
			}
			return rows;
		}
	}

	private List<ImportRow> leerExcel(MultipartFile archivo) throws IOException {
		try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
			if (workbook.getNumberOfSheets() == 0) {
				throw new BusinessRuleException("El archivo XLSX no contiene hojas.");
			}
			var sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(sheet.getFirstRowNum());
			if (headerRow == null) {
				throw new BusinessRuleException("La primera hoja no contiene encabezados.");
			}
			DataFormatter formatter = new DataFormatter();
			var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			List<String> headers = new ArrayList<>();
			for (int column = 0; column < headerRow.getLastCellNum(); column++) {
				headers.add(normalizarHeader(formatter.formatCellValue(headerRow.getCell(column), evaluator)));
			}
			validarEncabezados(headers);

			List<ImportRow> rows = new ArrayList<>();
			for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
				Row row = sheet.getRow(index);
				if (row == null || row.getLastCellNum() < 0) {
					continue;
				}
				List<String> values = new ArrayList<>();
				for (int column = 0; column < headers.size(); column++) {
					values.add(formatter.formatCellValue(row.getCell(column), evaluator));
				}
				if (values.stream().allMatch(String::isBlank)) {
					continue;
				}
				rows.add(construirFila(index + 1, headers, values));
			}
			return rows;
		} catch (org.apache.poi.EmptyFileException exception) {
			throw new BusinessRuleException("El archivo XLSX está vacío o no es válido.");
		}
	}

	private VotanteCreateRequest construirRequest(List<String> headers, List<String> values) {
		Map<String, String> data = new HashMap<>();
		for (int index = 0; index < headers.size(); index++) {
			data.put(headers.get(index), index < values.size() ? values.get(index).trim() : "");
		}
		TipoDocumento tipoDocumento = null;
		EstadoVotante estado = null;
		try {
			String rawType = data.get("tipodocumento");
			if (rawType != null && !rawType.isBlank()) {
				tipoDocumento = TipoDocumento.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
			}
			String rawState = data.get("estado");
			if (rawState != null && !rawState.isBlank()) {
				estado = EstadoVotante.valueOf(rawState.trim().toUpperCase(Locale.ROOT));
			}
		} catch (IllegalArgumentException exception) {
			throw new BusinessRuleException("El tipoDocumento o estado contiene un valor no reconocido.");
		}
		return new VotanteCreateRequest(
				tipoDocumento,
				data.get("numerodocumento"),
				data.get("nombres"),
				data.get("apellidos"),
				data.get("telefono"),
				data.get("direccion"),
				data.get("barrio"),
				data.get("mesavotacion"),
				estado,
				data.get("observaciones"));
	}

	private void validarEncabezados(List<String> headers) {
		if (!headers.containsAll(COLUMNAS_REQUERIDAS)) {
			throw new BusinessRuleException("Faltan encabezados requeridos: tipoDocumento, numeroDocumento, nombres, apellidos y mesaVotacion.");
		}
	}

	private String normalizarHeader(String header) {
		return Normalizer.normalize(header.trim(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^A-Za-z0-9]", "")
				.toLowerCase(Locale.ROOT);
	}

	private int contar(String value, char character) {
		return (int) value.chars().filter(current -> current == character).count();
	}

	private List<String> parseCsvLine(String line, char delimiter) {
		List<String> values = new ArrayList<>();
		StringBuilder value = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char current = line.charAt(index);
			if (current == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					value.append('"');
					index++;
				} else {
					quoted = !quoted;
				}
			} else if (current == delimiter && !quoted) {
				values.add(value.toString());
				value.setLength(0);
			} else {
				value.append(current);
			}
		}
		if (quoted) {
			throw new BusinessRuleException("El CSV contiene comillas sin cerrar.");
		}
		values.add(value.toString());
		return values;
	}

	private record ImportRow(int numero, VotanteCreateRequest request, String error) {
	}

	private record ValidationResult(List<ImportRow> validas, List<ImportacionError> errores) {
	}
}

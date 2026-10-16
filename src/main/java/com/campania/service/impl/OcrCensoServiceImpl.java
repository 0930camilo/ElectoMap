package com.campania.service.impl;

import com.campania.dto.votante.OcrCensoResponse;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.OcrUnavailableException;
import com.campania.service.OcrCensoService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OcrCensoServiceImpl implements OcrCensoService {

	private static final long MAX_BYTES = 5L * 1024 * 1024;
	private static final long MAX_PIXELS = 20_000_000L;

	private final String tessdataPath;
	private final String languages;

	public OcrCensoServiceImpl(
			@Value("${app.ocr.tessdata-path:./tessdata}") String tessdataPath,
			@Value("${app.ocr.languages:spa+eng}") String languages
	) {
		this.tessdataPath = tessdataPath;
		this.languages = languages;
	}

	@Override
	public OcrCensoResponse extraerDatos(MultipartFile imagen) {
		validarArchivo(imagen);
		BufferedImage bitmap;
		try {
			bitmap = ImageIO.read(imagen.getInputStream());
		} catch (IOException exception) {
			throw new BusinessRuleException("No fue posible leer la imagen enviada.");
		}
		if (bitmap == null || bitmap.getWidth() < 100 || bitmap.getHeight() < 40) {
			throw new BusinessRuleException("El archivo no contiene una imagen PNG o JPEG válida.");
		}
		if ((long) bitmap.getWidth() * bitmap.getHeight() > MAX_PIXELS) {
			throw new BusinessRuleException("La imagen excede el límite de resolución permitido.");
		}
		validarModelosIdioma();

		try {
			ITesseract tesseract = new Tesseract();
			tesseract.setDatapath(tessdataPath);
			tesseract.setLanguage(languages);
			BufferedImage imagenProcesada = ampliarImagen(bitmap);
			tesseract.setTessVariable("user_defined_dpi", "300");
			String texto = tesseract.doOCR(imagenProcesada);
			List<OcrToken> tokens = tesseract.getWords(
							imagenProcesada, ITessAPI.TessPageIteratorLevel.RIL_WORD).stream()
					.map(OcrToken::from)
					.toList();
			OcrCensoResponse resultado = mapear(texto, tokens, imagenProcesada.getWidth());
			String direccion = reconocerDireccion(tesseract, imagenProcesada, tokens);
			if (direccion != null) {
				return new OcrCensoResponse(
						resultado.numeroDocumento(),
						resultado.departamentoVotacion(),
						resultado.municipioVotacion(),
						resultado.puestoVotacion(),
						direccion,
						resultado.mesaVotacion(),
						resultado.textoDetectado(),
						resultado.confianza());
			}
			return resultado;
		} catch (TesseractException | RuntimeException | LinkageError exception) {
			throw new OcrUnavailableException(
					"No se pudo ejecutar el OCR. Verifique las bibliotecas nativas de Tesseract y que OCR_TESSDATA_PATH contenga los modelos de idioma configurados.",
					exception);
		}
	}

	private void validarModelosIdioma() {
		Path carpetaModelos = Path.of(tessdataPath);
		for (String idioma : languages.split("\\+")) {
			Path modelo = carpetaModelos.resolve(idioma + ".traineddata");
			if (!Files.isRegularFile(modelo)) {
				throw new OcrUnavailableException(
						"Falta el modelo OCR " + idioma + ".traineddata. Configure OCR_TESSDATA_PATH "
								+ "con la carpeta que contiene los modelos de idioma.",
						new IOException("No existe el modelo de idioma: " + modelo.toAbsolutePath()));
			}
		}
	}

	private void validarArchivo(MultipartFile imagen) {
		if (imagen == null || imagen.isEmpty()) {
			throw new BusinessRuleException("Debe adjuntar una imagen.");
		}
		if (imagen.getSize() > MAX_BYTES) {
			throw new BusinessRuleException("La imagen no puede superar los 5 MB.");
		}
		String contentType = imagen.getContentType();
		if (contentType == null || !(contentType.equalsIgnoreCase("image/png")
				|| contentType.equalsIgnoreCase("image/jpeg"))) {
			throw new BusinessRuleException("Solo se admiten imágenes PNG o JPEG.");
		}
	}

	private BufferedImage ampliarImagen(BufferedImage original) {
		int ancho = Math.multiplyExact(original.getWidth(), 2);
		int alto = Math.multiplyExact(original.getHeight(), 2);
		BufferedImage ampliada = new BufferedImage(ancho, alto, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D graphics = ampliada.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			graphics.drawImage(original, 0, 0, ancho, alto, null);
		} finally {
			graphics.dispose();
		}
		return ampliada;
	}

	private String reconocerDireccion(ITesseract tesseract, BufferedImage imagen, List<OcrToken> tokens)
			throws TesseractException {
		OcrToken encabezado = tokens.stream()
				.filter(token -> identificarEncabezado(token.texto()) == Campo.DIRECCION)
				.max(Comparator.comparingDouble(OcrToken::confianza))
				.orElse(null);
		if (encabezado == null) {
			return null;
		}

		List<OcrToken> encabezados = tokens.stream()
				.filter(token -> identificarEncabezado(token.texto()) != null)
				.sorted(Comparator.comparingInt(OcrToken::centroX))
				.toList();
		int indice = encabezados.indexOf(encabezado);
		int izquierda = indice == 0 ? 0
				: (encabezados.get(indice - 1).centroX() + encabezado.centroX()) / 2;
		int derecha = indice == encabezados.size() - 1 ? imagen.getWidth()
				: (encabezado.centroX() + encabezados.get(indice + 1).centroX()) / 2;
		int arriba = Math.min(imagen.getHeight() - 1, encabezado.y() + encabezado.alto());
		if (derecha <= izquierda || arriba >= imagen.getHeight()) {
			return null;
		}

		BufferedImage columna = imagen.getSubimage(izquierda, arriba, derecha - izquierda,
				imagen.getHeight() - arriba);
		BufferedImage ampliada = ampliarImagen(columna);
		tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);
		tesseract.setTessVariable("preserve_interword_spaces", "1");
		String texto = tesseract.doOCR(ampliada).replaceAll("\\s+", " ").trim();
		String direccion = normalizarDireccion(texto);
		return direccion.isBlank() ? null : direccion;
	}

	static String normalizarDireccion(String texto) {
		return texto.replaceAll("(?i)\\bN\\s*[°ºªo]\\s*", "Nº ")
				.replaceAll("\\s*-\\s*", "-")
				.replaceAll("\\s+", " ")
				.trim();
	}

	static OcrCensoResponse mapear(String texto, List<OcrToken> tokens) {
		int ancho = tokens.stream().mapToInt(token -> token.x() + token.ancho()).max().orElse(0) + 1;
		return mapear(texto, tokens, ancho);
	}

	static OcrCensoResponse mapear(String texto, List<OcrToken> tokens, int anchoImagen) {
		Map<Campo, OcrToken> encabezados = new EnumMap<>(Campo.class);
		for (OcrToken token : tokens) {
			Campo campo = identificarEncabezado(token.texto());
			if (campo != null) {
				encabezados.merge(campo, token,
						(actual, nuevo) -> nuevo.confianza() > actual.confianza() ? nuevo : actual);
			}
		}

		if (!encabezados.containsKey(Campo.MESA) && contieneEncabezado(texto, "MESA")
				&& !encabezados.isEmpty()) {
			int inicioEncabezado = encabezados.values().stream()
					.mapToInt(OcrToken::y)
					.min()
					.orElse(0);
			int finEncabezado = encabezados.values().stream()
					.mapToInt(token -> token.y() + token.alto())
					.max()
					.orElse(inicioEncabezado);
			encabezados.put(Campo.MESA, new OcrToken(
					"MESA", Math.max(0, anchoImagen - 1), inicioEncabezado, 0,
					finEncabezado - inicioEncabezado, 0));
		}

		Map<Campo, String> valores = new EnumMap<>(Campo.class);
		if (!encabezados.isEmpty()) {
			int finEncabezado = encabezados.values().stream()
					.mapToInt(token -> token.y() + token.alto())
					.max()
					.orElse(0);
			List<Map.Entry<Campo, OcrToken>> columnas = encabezados.entrySet().stream()
					.sorted(Comparator.comparingInt(entry -> entry.getValue().centroX()))
					.toList();

			for (int index = 0; index < columnas.size(); index++) {
				Map.Entry<Campo, OcrToken> columna = columnas.get(index);
				int izquierda = index == 0 ? Integer.MIN_VALUE
						: (columnas.get(index - 1).getValue().centroX() + columna.getValue().centroX()) / 2;
				int derecha = index == columnas.size() - 1 ? Integer.MAX_VALUE
						: (columna.getValue().centroX() + columnas.get(index + 1).getValue().centroX()) / 2;
				String valor = tokens.stream()
						.filter(token -> token.y() > finEncabezado && token.centroX() >= izquierda
								&& token.centroX() < derecha && identificarEncabezado(token.texto()) == null)
						.sorted(Comparator.comparingInt(OcrToken::y).thenComparingInt(OcrToken::x))
						.map(OcrToken::texto)
						.reduce((primero, siguiente) -> primero + " " + siguiente)
						.map(String::trim)
						.orElse(null);
				valores.put(columna.getKey(), valor);
			}
		}

		double confianza = tokens.isEmpty() ? 0.0
				: tokens.stream().mapToDouble(OcrToken::confianza).average().orElse(0.0);
		return new OcrCensoResponse(
				valores.get(Campo.DOCUMENTO),
				valores.get(Campo.DEPARTAMENTO),
				valores.get(Campo.MUNICIPIO),
				valores.get(Campo.PUESTO),
				valores.get(Campo.DIRECCION),
				valores.get(Campo.MESA),
				texto == null ? "" : texto.trim(),
				Math.round(confianza * 100.0) / 100.0);
	}

	private static boolean contieneEncabezado(String texto, String encabezado) {
		return texto != null && java.util.regex.Pattern.compile(
						"(?iu)(^|[^\\p{L}\\p{N}])" + encabezado + "([^\\p{L}\\p{N}]|$)")
				.matcher(texto)
				.find();
	}

	private static Campo identificarEncabezado(String texto) {
		String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toUpperCase(Locale.ROOT)
				.replaceAll("[^A-Z0-9]", "");
		if (normalizado.equals("NUIP") || normalizado.equals("DOCUMENTO")) {
			return Campo.DOCUMENTO;
		}
		if (normalizado.equals("DEPARTAMENTO") || normalizado.equals("DEPTO")) {
			return Campo.DEPARTAMENTO;
		}
		if (normalizado.equals("MUNICIPIO") || normalizado.equals("MPIO")) {
			return Campo.MUNICIPIO;
		}
		if (normalizado.equals("PUESTO")) {
			return Campo.PUESTO;
		}
		if (normalizado.equals("DIRECCION")) {
			return Campo.DIRECCION;
		}
		if (normalizado.equals("MESA")) {
			return Campo.MESA;
		}
		return null;
	}

	private enum Campo {
		DOCUMENTO,
		DEPARTAMENTO,
		MUNICIPIO,
		PUESTO,
		DIRECCION,
		MESA
	}

	record OcrToken(String texto, int x, int y, int ancho, int alto, float confianza) {

		int centroX() {
			return x + ancho / 2;
		}

		static OcrToken from(Word palabra) {
			Rectangle caja = palabra.getBoundingBox();
			return new OcrToken(palabra.getText(), caja.x, caja.y, caja.width, caja.height,
					palabra.getConfidence());
		}
	}
}

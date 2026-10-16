package com.campania.service.impl;

import com.campania.exception.OcrUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OcrCensoServiceTests {

	@Test
	void reportsOcrUnavailableWhenLanguageDataIsNotInstalled() throws Exception {
		BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		MockMultipartFile upload = new MockMultipartFile(
				"imagen", "captura.png", "image/png", bytes.toByteArray());
		OcrCensoServiceImpl service = new OcrCensoServiceImpl(
				Path.of("target", "missing-tessdata").toAbsolutePath().toString(), "spa+eng");

		assertThrows(OcrUnavailableException.class, () -> service.extraerDatos(upload));
	}
}

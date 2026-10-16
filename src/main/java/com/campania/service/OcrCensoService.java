package com.campania.service;

import com.campania.dto.votante.OcrCensoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrCensoService {

	OcrCensoResponse extraerDatos(MultipartFile imagen);
}

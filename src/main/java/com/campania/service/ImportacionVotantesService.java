package com.campania.service;

import com.campania.dto.votante.ImportacionPreviewResponse;
import com.campania.dto.votante.ImportacionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImportacionVotantesService {

	ImportacionPreviewResponse validar(MultipartFile archivo, Long liderId);

	ImportacionResponse importar(MultipartFile archivo, Long liderId);

	byte[] reporteErrores(MultipartFile archivo, Long liderId);
}

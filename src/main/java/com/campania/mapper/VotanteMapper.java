package com.campania.mapper;

import com.campania.dto.votante.VotanteResponse;
import com.campania.entity.Votante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VotanteMapper {

	@Mapping(target = "liderId", source = "lider.id")
	@Mapping(target = "liderNombre", expression = "java(votante.getLider().getNombres() + \" \" + votante.getLider().getApellidos())")
	@Mapping(target = "cabeceraId", source = "lider.cabecera.id")
	@Mapping(target = "cabeceraNombre", source = "lider.cabecera.nombre")
	VotanteResponse toResponse(Votante votante);
}

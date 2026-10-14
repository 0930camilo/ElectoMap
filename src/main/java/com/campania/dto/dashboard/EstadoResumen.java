package com.campania.dto.dashboard;

import com.campania.entity.EstadoVotante;

public record EstadoResumen(EstadoVotante estado, long total) {
}

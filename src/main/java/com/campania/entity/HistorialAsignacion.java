package com.campania.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "historial_asignacion")
public class HistorialAsignacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "votante_id", nullable = false)
	private Votante votante;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lider_anterior_id")
	private Lider liderAnterior;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lider_nuevo_id", nullable = false)
	private Lider liderNuevo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "motivo", nullable = false, length = 500)
	private String motivo;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;
}

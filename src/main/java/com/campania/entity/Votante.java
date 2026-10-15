package com.campania.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "votante",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_votante_admin_documento",
						columnNames = {"administrador_id", "numero_documento"})
		},
		indexes = {
				@Index(name = "idx_votante_nombres", columnList = "nombres"),
				@Index(name = "idx_votante_apellidos", columnList = "apellidos"),
				@Index(name = "idx_votante_telefono", columnList = "telefono"),
				@Index(name = "idx_votante_estado", columnList = "estado"),
				@Index(name = "idx_votante_lider", columnList = "lider_id")
		})
public class Votante extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false, length = 20)
	private TipoDocumento tipoDocumento;

	@Column(name = "numero_documento", nullable = false, length = 30)
	private String numeroDocumento;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	private String apellidos;

	@Column(name = "telefono", length = 30)
	private String telefono;

	@Column(name = "departamento_votacion", length = 100)
	private String departamentoVotacion;

	@Column(name = "municipio_votacion", length = 100)
	private String municipioVotacion;

	@Column(name = "direccion", length = 250)
	private String direccion;

	@Column(name = "barrio", length = 150)
	private String barrio;

	@Column(name = "mesa_votacion", length = 30)
	private String mesaVotacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 30)
	private EstadoVotante estado = EstadoVotante.PENDIENTE;

	@Column(name = "observaciones", length = 1000)
	private String observaciones;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lider_id", nullable = false)
	private Lider lider;

	@Column(name = "administrador_id", nullable = false)
	private Long administradorId;

	@Column(name = "fecha_eliminacion")
	private LocalDateTime fechaEliminacion;
}

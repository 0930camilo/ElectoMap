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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auditoria", indexes = {
		@Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
		@Index(name = "idx_auditoria_entidad", columnList = "entidad, entidad_id"),
		@Index(name = "idx_auditoria_fecha", columnList = "fecha")
})
public class Auditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Enumerated(EnumType.STRING)
	@Column(name = "accion", nullable = false, length = 30)
	private AccionAuditoria accion;

	@Column(name = "entidad", nullable = false, length = 60)
	private String entidad;

	@Column(name = "entidad_id", nullable = false)
	private Long entidadId;

	@Column(name = "descripcion", nullable = false, length = 1000)
	private String descripcion;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	@Column(name = "ip", length = 45)
	private String ip;
}

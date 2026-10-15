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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lider",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_lider_usuario", columnNames = "usuario_id"),
				@UniqueConstraint(name = "uk_lider_admin_documento",
						columnNames = {"administrador_id", "documento"})
		},
		indexes = {
				@Index(name = "idx_lider_cabecera", columnList = "cabecera_id"),
				@Index(name = "idx_lider_estado", columnList = "estado")
		})
public class Lider extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cabecera_id", nullable = false)
	private Cabecera cabecera;

	@Column(name = "administrador_id", nullable = false)
	private Long administradorId;

	@Column(name = "nombres", nullable = false, length = 100)
	private String nombres;

	@Column(name = "apellidos", nullable = false, length = 100)
	private String apellidos;

	@Column(name = "documento", nullable = false, length = 30)
	private String documento;

	@Column(name = "telefono", length = 30)
	private String telefono;

	@Column(name = "email", length = 150)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoLider estado = EstadoLider.ACTIVO;
}

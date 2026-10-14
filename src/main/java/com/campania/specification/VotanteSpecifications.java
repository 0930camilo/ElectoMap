package com.campania.specification;

import com.campania.entity.EstadoVotante;
import com.campania.entity.Votante;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class VotanteSpecifications {

	private VotanteSpecifications() {
	}

	public static Specification<Votante> buscar(
			String numeroDocumento,
			String nombres,
			String apellidos,
			String telefono,
			EstadoVotante estado,
			Long cabeceraId,
			Long liderId
	) {
		return (root, query, builder) -> {
			var predicates = builder.and(
					builder.isNull(root.get("fechaEliminacion")),
					liderId == null ? builder.conjunction()
							: builder.equal(root.get("lider").get("id"), liderId),
					cabeceraId == null ? builder.conjunction()
							: builder.equal(root.get("lider").get("cabecera").get("id"), cabeceraId),
					estado == null ? builder.conjunction() : builder.equal(root.get("estado"), estado),
					contiene(builder, root.get("numeroDocumento"), numeroDocumento),
					contiene(builder, root.get("nombres"), nombres),
					contiene(builder, root.get("apellidos"), apellidos),
					contiene(builder, root.get("telefono"), telefono)
			);
			return predicates;
		};
	}

	private static jakarta.persistence.criteria.Predicate contiene(
			jakarta.persistence.criteria.CriteriaBuilder builder,
			jakarta.persistence.criteria.Path<String> path,
			String value
	) {
		if (value == null || value.isBlank()) {
			return builder.conjunction();
		}
		return builder.like(builder.lower(path), "%" + value.trim().toLowerCase(Locale.ROOT) + "%");
	}
}

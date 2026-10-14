package com.campania.service.impl;

import com.campania.dto.dashboard.AdminDashboardResponse;
import com.campania.dto.dashboard.EstadoResumen;
import com.campania.dto.dashboard.GrupoResumen;
import com.campania.dto.dashboard.LiderDashboardResponse;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.Lider;
import com.campania.entity.Usuario;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.VotanteRepository;
import com.campania.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

	private static final ZoneId ZONA_CAMPANIA = ZoneId.of("America/Bogota");

	private final CabeceraRepository cabeceraRepository;
	private final LiderRepository liderRepository;
	private final VotanteRepository votanteRepository;
	private final UsuarioRepository usuarioRepository;

	@Override
	public AdminDashboardResponse dashboardAdmin() {
		LocalDate hoy = LocalDate.now(ZONA_CAMPANIA);
		return new AdminDashboardResponse(
				cabeceraRepository.countByEstado(EstadoCabecera.ACTIVA),
				liderRepository.countByEstado(EstadoLider.ACTIVO),
				votanteRepository.contarVigentes(null),
				votanteRepository.contarCreadosDesde(hoy.atStartOfDay(), null),
				votanteRepository.contarCreadosDesde(inicioSemana(hoy), null),
				votanteRepository.contarCreadosDesde(hoy.withDayOfMonth(1).atStartOfDay(), null),
				estados(null),
				votanteRepository.contarPorCabecera().stream()
						.map(grupo -> new GrupoResumen(grupo.getId(), grupo.getNombre(), grupo.getTotal()))
						.toList(),
				votanteRepository.contarPorLider().stream()
						.map(grupo -> new GrupoResumen(grupo.getId(), grupo.getNombre(), grupo.getTotal()))
						.toList());
	}

	@Override
	public LiderDashboardResponse dashboardLider() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
		Lider lider = liderRepository.findByUsuario_Id(usuario.getId())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el líder asociado al usuario."));
		LocalDate hoy = LocalDate.now(ZONA_CAMPANIA);
		return new LiderDashboardResponse(
				lider.getId(),
				lider.getNombres() + " " + lider.getApellidos(),
				lider.getCabecera().getId(),
				lider.getCabecera().getNombre(),
				votanteRepository.contarVigentes(lider.getId()),
				votanteRepository.contarCreadosDesde(hoy.atStartOfDay(), lider.getId()),
				votanteRepository.contarCreadosDesde(inicioSemana(hoy), lider.getId()),
				votanteRepository.contarCreadosDesde(hoy.withDayOfMonth(1).atStartOfDay(), lider.getId()),
				estados(lider.getId()));
	}

	private List<EstadoResumen> estados(Long liderId) {
		return votanteRepository.contarPorEstado(liderId).stream()
				.map(estado -> new EstadoResumen(estado.getEstado(), estado.getTotal()))
				.toList();
	}

	private LocalDateTime inicioSemana(LocalDate hoy) {
		return hoy.with(DayOfWeek.MONDAY).atStartOfDay();
	}
}

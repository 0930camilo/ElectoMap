package com.campania.service.impl;

import com.campania.dto.dashboard.AdminDashboardResponse;
import com.campania.dto.dashboard.EstadoResumen;
import com.campania.dto.dashboard.GrupoResumen;
import com.campania.dto.dashboard.LiderDashboardResponse;
import com.campania.entity.EstadoCabecera;
import com.campania.entity.EstadoLider;
import com.campania.entity.Lider;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.exception.BusinessRuleException;
import com.campania.exception.ResourceNotFoundException;
import com.campania.repository.CabeceraRepository;
import com.campania.repository.LiderRepository;
import com.campania.repository.UsuarioRepository;
import com.campania.repository.VotanteRepository;
import com.campania.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
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
		Usuario administrador = administradorAutenticado();
		String username = administrador.getUsername();
		LocalDate hoy = LocalDate.now(ZONA_CAMPANIA);
		return new AdminDashboardResponse(
				cabeceraRepository.countByUsuario_UsernameAndEstado(username, EstadoCabecera.ACTIVA),
				liderRepository.countByCabecera_Usuario_UsernameAndEstado(username, EstadoLider.ACTIVO),
				votanteRepository.contarVigentesDeAdministrador(username),
				votanteRepository.contarCreadosDesdeDeAdministrador(hoy.atStartOfDay(), username),
				votanteRepository.contarCreadosDesdeDeAdministrador(inicioSemana(hoy), username),
				votanteRepository.contarCreadosDesdeDeAdministrador(
						hoy.withDayOfMonth(1).atStartOfDay(), username),
				estadosDeAdministrador(username),
				votanteRepository.contarPorCabeceraDeAdministrador(username).stream()
						.map(grupo -> new GrupoResumen(grupo.getId(), grupo.getNombre(), grupo.getTotal()))
						.toList(),
				votanteRepository.contarPorLiderDeAdministrador(username).stream()
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

	private List<EstadoResumen> estadosDeAdministrador(String username) {
		return votanteRepository.contarPorEstadoDeAdministrador(username).stream()
				.map(estado -> new EstadoResumen(estado.getEstado(), estado.getTotal()))
				.toList();
	}

	private Usuario administradorAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities().stream()
				.noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
			throw new AccessDeniedException("Esta operación requiere permisos de administrador.");
		}
		Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("No se encontró el administrador autenticado."));
		if (usuario.getRol() != Rol.ADMIN) {
			throw new BusinessRuleException("El usuario autenticado no tiene rol de administrador.");
		}
		return usuario;
	}

	private LocalDateTime inicioSemana(LocalDate hoy) {
		return hoy.with(DayOfWeek.MONDAY).atStartOfDay();
	}
}

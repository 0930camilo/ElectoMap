package com.campania.config;

import com.campania.entity.EstadoUsuario;
import com.campania.entity.Rol;
import com.campania.entity.Usuario;
import com.campania.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
public class BootstrapAdminConfig {

	@Bean
	ApplicationRunner bootstrapAdmin(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.admin.username}") String username,
			@Value("${app.bootstrap.admin.password}") String password,
			@Value("${app.bootstrap.admin.nombres}") String nombres,
			@Value("${app.bootstrap.admin.apellidos}") String apellidos,
			@Value("${app.bootstrap.admin.email:}") String email
	) {
		return args -> {
			if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
				throw new IllegalStateException(
						"BOOTSTRAP_ADMIN_USERNAME y BOOTSTRAP_ADMIN_PASSWORD son obligatorios si el bootstrap está habilitado.");
			}
			if (usuarioRepository.existsByUsername(username)) {
				return;
			}

			Usuario administrador = new Usuario();
			administrador.setNombres(nombres);
			administrador.setApellidos(apellidos);
			administrador.setUsername(username);
			administrador.setPasswordHash(passwordEncoder.encode(password));
			administrador.setEmail(StringUtils.hasText(email) ? email : null);
			administrador.setRol(Rol.ADMIN);
			administrador.setEstado(EstadoUsuario.ACTIVO);
			usuarioRepository.save(administrador);
		};
	}
}

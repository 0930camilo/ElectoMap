INSERT INTO usuario (
    nombres, apellidos, username, password_hash, email, rol, estado, fecha_creacion, fecha_actualizacion
) VALUES (
    'Administrador', 'Inicial', 'admin', '$2a$10$dtFE5x8Z65MgT79dX/q2yOeKHgNo0t0zjLK73IvweM34HoOxjfgAy',
    NULL, 'ADMIN', 'ACTIVO', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);

INSERT INTO cabecera (
    nombre, descripcion, zona, estado, fecha_creacion, fecha_actualizacion
) VALUES (
    'Cabecera de prueba', 'Registro inicial para pruebas locales.', 'Zona de prueba',
    'ACTIVA', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);

INSERT INTO usuario (
    nombres, apellidos, username, password_hash, email, rol, estado, fecha_creacion, fecha_actualizacion
) VALUES (
    'Líder', 'De Prueba', 'lider.prueba', '$2a$10$dtFE5x8Z65MgT79dX/q2yOeKHgNo0t0zjLK73IvweM34HoOxjfgAy',
    NULL, 'LIDER', 'ACTIVO', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);

INSERT INTO lider (
    usuario_id, cabecera_id, nombres, apellidos, documento, telefono, email, estado,
    fecha_creacion, fecha_actualizacion
)
SELECT u.id, c.id, 'Líder', 'De Prueba', 'PRUEBA0001', NULL, NULL, 'ACTIVO',
       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM usuario u
CROSS JOIN cabecera c
WHERE u.username = 'lider.prueba'
  AND c.nombre = 'Cabecera de prueba';

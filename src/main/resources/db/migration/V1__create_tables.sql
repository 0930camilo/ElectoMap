CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(150) NULL,
    rol VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_usuario_email ON usuario (email);
CREATE INDEX idx_usuario_estado ON usuario (estado);

CREATE TABLE cabecera (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    zona VARCHAR(150) NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    CONSTRAINT pk_cabecera PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cabecera_estado ON cabecera (estado);
CREATE INDEX idx_cabecera_nombre ON cabecera (nombre);

CREATE TABLE lider (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    cabecera_id BIGINT NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    telefono VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    CONSTRAINT pk_lider PRIMARY KEY (id),
    CONSTRAINT uk_lider_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_lider_documento UNIQUE (documento),
    CONSTRAINT fk_lider_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_lider_cabecera FOREIGN KEY (cabecera_id) REFERENCES cabecera (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_lider_cabecera ON lider (cabecera_id);
CREATE INDEX idx_lider_estado ON lider (estado);

CREATE TABLE votante (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(30) NULL,
    email VARCHAR(150) NULL,
    direccion VARCHAR(250) NULL,
    barrio VARCHAR(150) NULL,
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(1000) NULL,
    lider_id BIGINT NOT NULL,
    fecha_eliminacion DATETIME(6) NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    CONSTRAINT pk_votante PRIMARY KEY (id),
    CONSTRAINT uk_votante_numero_documento UNIQUE (numero_documento),
    CONSTRAINT fk_votante_lider FOREIGN KEY (lider_id) REFERENCES lider (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_votante_nombres ON votante (nombres);
CREATE INDEX idx_votante_apellidos ON votante (apellidos);
CREATE INDEX idx_votante_telefono ON votante (telefono);
CREATE INDEX idx_votante_estado ON votante (estado);
CREATE INDEX idx_votante_lider ON votante (lider_id);

CREATE TABLE historial_asignacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    votante_id BIGINT NOT NULL,
    lider_anterior_id BIGINT NULL,
    lider_nuevo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    CONSTRAINT pk_historial_asignacion PRIMARY KEY (id),
    CONSTRAINT fk_historial_votante FOREIGN KEY (votante_id) REFERENCES votante (id),
    CONSTRAINT fk_historial_lider_anterior FOREIGN KEY (lider_anterior_id) REFERENCES lider (id),
    CONSTRAINT fk_historial_lider_nuevo FOREIGN KEY (lider_nuevo_id) REFERENCES lider (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_historial_votante_fecha ON historial_asignacion (votante_id, fecha);

CREATE TABLE auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    accion VARCHAR(30) NOT NULL,
    entidad VARCHAR(60) NOT NULL,
    entidad_id BIGINT NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    ip VARCHAR(45) NULL,
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auditoria_usuario ON auditoria (usuario_id);
CREATE INDEX idx_auditoria_entidad ON auditoria (entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);

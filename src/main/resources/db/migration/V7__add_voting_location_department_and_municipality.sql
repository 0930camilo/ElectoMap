ALTER TABLE votante
    ADD COLUMN departamento_votacion VARCHAR(100) NULL AFTER telefono,
    ADD COLUMN municipio_votacion VARCHAR(100) NULL AFTER departamento_votacion;

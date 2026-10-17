ALTER TABLE lider
    ADD COLUMN administrador_id BIGINT NULL AFTER cabecera_id;

UPDATE lider l
JOIN cabecera c ON c.id = l.cabecera_id
SET l.administrador_id = c.usuario_id
WHERE l.administrador_id IS NULL;

ALTER TABLE lider
    MODIFY COLUMN administrador_id BIGINT NOT NULL;

ALTER TABLE lider
    ADD CONSTRAINT fk_lider_administrador
        FOREIGN KEY (administrador_id) REFERENCES usuario (id);

ALTER TABLE lider
    ADD CONSTRAINT uk_lider_admin_documento
        UNIQUE (administrador_id, documento);

ALTER TABLE lider
    DROP INDEX uk_lider_documento;

ALTER TABLE votante
    ADD COLUMN administrador_id BIGINT NULL AFTER lider_id;

UPDATE votante v
JOIN lider l ON l.id = v.lider_id
SET v.administrador_id = l.administrador_id
WHERE v.administrador_id IS NULL;

ALTER TABLE votante
    MODIFY COLUMN administrador_id BIGINT NOT NULL;

ALTER TABLE votante
    ADD CONSTRAINT fk_votante_administrador
        FOREIGN KEY (administrador_id) REFERENCES usuario (id);

ALTER TABLE votante
    ADD CONSTRAINT uk_votante_admin_documento
        UNIQUE (administrador_id, numero_documento);

ALTER TABLE votante
    DROP INDEX uk_votante_numero_documento;

ALTER TABLE cabecera
    ADD COLUMN usuario_id BIGINT NULL AFTER zona;

UPDATE cabecera c
JOIN (
    SELECT id
    FROM usuario
    WHERE rol = 'ADMIN'
    ORDER BY id
    LIMIT 1
) admin_user ON TRUE
SET c.usuario_id = admin_user.id
WHERE c.usuario_id IS NULL;

CREATE INDEX idx_cabecera_usuario ON cabecera (usuario_id);

ALTER TABLE cabecera
    MODIFY COLUMN usuario_id BIGINT NOT NULL;

ALTER TABLE cabecera
    ADD CONSTRAINT fk_cabecera_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

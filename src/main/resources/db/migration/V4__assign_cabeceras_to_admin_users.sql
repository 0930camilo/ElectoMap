ALTER TABLE cabecera
    ADD COLUMN usuario_id BIGINT NULL AFTER zona;

UPDATE cabecera
SET usuario_id = (
    SELECT id
    FROM usuario
    WHERE rol = 'ADMIN'
    ORDER BY id
    LIMIT 1
)
WHERE usuario_id IS NULL;

ALTER TABLE cabecera
    MODIFY COLUMN usuario_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_cabecera_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id);

CREATE INDEX idx_cabecera_usuario ON cabecera (usuario_id);

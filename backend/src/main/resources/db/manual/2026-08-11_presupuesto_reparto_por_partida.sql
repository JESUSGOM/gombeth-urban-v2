-- Gombeth Urban v2
-- Reparto del presupuesto por partida y seleccion de propietarios.
-- Cambio ADITIVO: no modifica tablas existentes.

CREATE TABLE IF NOT EXISTS presupuesto_reparto_configuracion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    presupuesto_id BIGINT NOT NULL,
    metodo_reparto VARCHAR(30) NOT NULL,
    aplica_todos TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_presupuesto_reparto_config_presupuesto (presupuesto_id),
    CONSTRAINT fk_presupuesto_reparto_config_presupuesto
        FOREIGN KEY (presupuesto_id)
        REFERENCES presupuestos (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS presupuesto_reparto_vecinos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    presupuesto_id BIGINT NOT NULL,
    vecino_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_presupuesto_reparto_vecino (presupuesto_id, vecino_id),
    KEY idx_presupuesto_reparto_vecinos_vecino (vecino_id),
    CONSTRAINT fk_presupuesto_reparto_vecinos_presupuesto
        FOREIGN KEY (presupuesto_id)
        REFERENCES presupuestos (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_presupuesto_reparto_vecinos_vecino
        FOREIGN KEY (vecino_id)
        REFERENCES vecinos (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

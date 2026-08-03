-- Gombeth Urban V2
-- Auditoría del ciclo de vida de las remesas.
--
-- Migración manual para MySQL.
--
-- No se crean claves foráneas para mantener la compatibilidad
-- con la aplicación anterior que utiliza la misma base sepa_1914.
--
-- No ejecutar todavía. Se aplicará en el siguiente paso,
-- justo antes de conectar los endpoints.

CREATE TABLE remesa_eventos (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                remesa_id BIGINT NOT NULL,
                                comunidad_id BIGINT NOT NULL,
                                usuario_id BIGINT NULL,
                                tipo_evento VARCHAR(40) NOT NULL,
                                estado_anterior VARCHAR(30) NULL,
                                estado_nuevo VARCHAR(30) NULL,
                                formato VARCHAR(10) NULL,
                                nombre_archivo VARCHAR(255) NULL,
                                fecha_evento DATETIME(6) NOT NULL,
                                detalle VARCHAR(500) NULL,

                                PRIMARY KEY (id),

                                INDEX idx_remesa_eventos_remesa_fecha (
                                                                       remesa_id,
                                                                       fecha_evento
                                    ),

                                INDEX idx_remesa_eventos_comunidad_fecha (
                                                                          comunidad_id,
                                                                          fecha_evento
                                    ),

                                INDEX idx_remesa_eventos_usuario_fecha (
                                                                        usuario_id,
                                                                        fecha_evento
                                    ),

                                INDEX idx_remesa_eventos_tipo (
                                                               tipo_evento
                                    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;

-- Comprobación:
--
-- SHOW CREATE TABLE remesa_eventos;
--
-- SELECT COUNT(*)
-- FROM remesa_eventos;
-- Gombeth Urban V2
-- Asociación y copia histórica de la cuenta presentadora utilizada.
--
-- Migración manual para MySQL.
--
-- Las columnas admiten NULL para mantener la compatibilidad con:
--   1. Las remesas históricas ya existentes.
--   2. El proyecto anterior que utiliza la misma base de datos.
--
-- No se crea una clave foránea deliberadamente:
-- una cuenta presentadora podrá eliminarse sin perder los datos históricos
-- que quedaron copiados en cada remesa.

-- ============================================================
-- PASO 1. AÑADIR LAS COLUMNAS
-- ============================================================

ALTER TABLE ficheros_generados
    ADD COLUMN cuenta_presentador_id BIGINT NULL,
    ADD COLUMN presentador_alias VARCHAR(100) NULL,
    ADD COLUMN presentador_identificador VARCHAR(35) NULL,
    ADD COLUMN presentador_nif_cif VARCHAR(20) NULL,
    ADD COLUMN presentador_sufijo VARCHAR(3) NULL,
    ADD COLUMN presentador_iban VARCHAR(34) NULL,
    ADD COLUMN presentador_bic VARCHAR(11) NULL;

-- ============================================================
-- PASO 2. CREAR EL ÍNDICE POR SEPARADO
-- ============================================================
--
-- Ejecutar solamente después de comprobar que las columnas existen.
--
-- CREATE INDEX idx_ficheros_generados_cuenta_presentador
--     ON ficheros_generados (cuenta_presentador_id);

-- ============================================================
-- COMPROBACIÓN
-- ============================================================
--
-- SELECT
--     COLUMN_NAME,
--     COLUMN_TYPE,
--     IS_NULLABLE
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'sepa_1914'
--   AND TABLE_NAME = 'ficheros_generados'
--   AND COLUMN_NAME IN (
--       'cuenta_presentador_id',
--       'presentador_alias',
--       'presentador_identificador',
--       'presentador_nif_cif',
--       'presentador_sufijo',
--       'presentador_iban',
--       'presentador_bic'
--   )
-- ORDER BY ORDINAL_POSITION;
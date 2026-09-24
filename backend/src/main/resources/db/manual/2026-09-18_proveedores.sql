/*
 * Gombeth Urban V2
 *
 * Maestro de proveedores por Administrador y asociación
 * de cada proveedor con las comunidades en las que trabaja.
 *
 * IMPORTANTE:
 * Este script se prepara primero para revisión.
 * NO ejecutar todavía en producción.
 */


/* ==========================================================
   1. MAESTRO DE PROVEEDORES POR ADMINISTRADOR
   ========================================================== */

CREATE TABLE proveedores (
                             id BIGINT NOT NULL AUTO_INCREMENT,

                             administrador_id BIGINT NOT NULL,

                             nombre VARCHAR(255) NOT NULL,

                             nif_cif VARCHAR(20) NULL,

                             telefono VARCHAR(50) NULL,

                             email VARCHAR(255) NULL,

                             observaciones VARCHAR(1000) NULL,

                             activo BIT(1) NOT NULL DEFAULT b'1',

                             PRIMARY KEY (id),

                             CONSTRAINT fk_proveedores_administrador
                                 FOREIGN KEY (administrador_id)
                                     REFERENCES administradores(id),

                             CONSTRAINT uk_proveedores_administrador_nif
                                 UNIQUE (administrador_id, nif_cif)
);


/* ==========================================================
   2. PROVEEDOR ASOCIADO A UNA COMUNIDAD
   ========================================================== */

CREATE TABLE proveedor_comunidades (
                                       id BIGINT NOT NULL AUTO_INCREMENT,

                                       proveedor_id BIGINT NOT NULL,

                                       comunidad_id BIGINT NOT NULL,

                                       cuenta_contable_id BIGINT NOT NULL,

                                       activo BIT(1) NOT NULL DEFAULT b'1',

                                       PRIMARY KEY (id),

                                       CONSTRAINT fk_proveedor_comunidad_proveedor
                                           FOREIGN KEY (proveedor_id)
                                               REFERENCES proveedores(id),

                                       CONSTRAINT fk_proveedor_comunidad_comunidad
                                           FOREIGN KEY (comunidad_id)
                                               REFERENCES comunidades(id),

                                       CONSTRAINT fk_proveedor_comunidad_cuenta
                                           FOREIGN KEY (cuenta_contable_id)
                                               REFERENCES contabilidad_cuentas(id),

                                       CONSTRAINT uk_proveedor_comunidad
                                           UNIQUE (proveedor_id, comunidad_id),

                                       CONSTRAINT uk_comunidad_cuenta_proveedor
                                           UNIQUE (comunidad_id, cuenta_contable_id)
);


/* ==========================================================
   3. RELACIÓN DEL GASTO CON SU PROVEEDOR EN LA COMUNIDAD
   ========================================================== */

ALTER TABLE contabilidad_gastos
    ADD COLUMN proveedor_comunidad_id BIGINT NULL
        AFTER proveedor;


/*
 * La columna actual:
 *
 *     proveedor VARCHAR(255)
 *
 * SE CONSERVA.
 *
 * No la eliminamos porque existen gastos históricos que dependen
 * de ella y necesitamos una migración progresiva.
 */

ALTER TABLE contabilidad_gastos
    ADD CONSTRAINT fk_gasto_proveedor_comunidad
        FOREIGN KEY (proveedor_comunidad_id)
            REFERENCES proveedor_comunidades(id);


/* ==========================================================
   FIN DE MIGRACIÓN
   ========================================================== */
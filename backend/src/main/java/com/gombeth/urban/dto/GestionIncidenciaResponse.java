package com.gombeth.urban.dto;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.GestionIncidencia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GestionIncidenciaResponse {

    private final Long id;
    private final BigDecimal costeEstimado;
    private final String descripcion;
    private final String observacionesInternas;
    private final String estado;
    private final LocalDateTime fechaRegistro;
    private final LocalDateTime fechaActualizacion;
    private final LocalDateTime fechaFinalizacion;
    private final LocalDateTime fechaCierre;
    private final String prioridad;
    private final String titulo;
    private final ComunidadResumen comunidad;

    public GestionIncidenciaResponse(
            Long id,
            BigDecimal costeEstimado,
            String descripcion,
            String observacionesInternas,
            String estado,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaActualizacion,
            LocalDateTime fechaFinalizacion,
            LocalDateTime fechaCierre,
            String prioridad,
            String titulo,
            ComunidadResumen comunidad
    ) {
        this.id = id;
        this.costeEstimado = costeEstimado;
        this.descripcion = descripcion;
        this.observacionesInternas = observacionesInternas;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.fechaActualizacion = fechaActualizacion;
        this.fechaFinalizacion = fechaFinalizacion;
        this.fechaCierre = fechaCierre;
        this.prioridad = prioridad;
        this.titulo = titulo;
        this.comunidad = comunidad;
    }

    public static GestionIncidenciaResponse desde(
            GestionIncidencia incidencia
    ) {
        ComunidadResumen comunidadResumen = null;

        Comunidad comunidad = incidencia.getComunidad();

        if (comunidad != null) {
            comunidadResumen = new ComunidadResumen(
                    comunidad.getId(),
                    comunidad.getNombre()
            );
        }

        return new GestionIncidenciaResponse(
                incidencia.getId(),
                incidencia.getCosteEstimado(),
                incidencia.getDescripcion(),
                incidencia.getObservacionesInternas(),
                incidencia.getEstado(),
                incidencia.getFechaRegistro(),
                incidencia.getFechaActualizacion(),
                incidencia.getFechaFinalizacion(),
                incidencia.getFechaCierre(),
                incidencia.getPrioridad(),
                incidencia.getTitulo(),
                comunidadResumen
        );
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getCosteEstimado() {
        return costeEstimado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getObservacionesInternas() {
        return observacionesInternas;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public LocalDateTime getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public String getTitulo() {
        return titulo;
    }

    public ComunidadResumen getComunidad() {
        return comunidad;
    }

    public static class ComunidadResumen {

        private final Long id;
        private final String nombre;

        public ComunidadResumen(
                Long id,
                String nombre
        ) {
            this.id = id;
            this.nombre = nombre;
        }

        public Long getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }
    }
}
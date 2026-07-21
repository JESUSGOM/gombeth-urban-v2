package com.gombeth.urban.repository;

import com.gombeth.urban.entity.GestionIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GestionIncidenciaRepository
        extends JpaRepository<GestionIncidencia, Long> {

    List<GestionIncidencia> findByComunidadIdOrderByFechaRegistroDesc(Long comunidadId);

}
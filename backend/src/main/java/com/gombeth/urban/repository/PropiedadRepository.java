package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    List<Propiedad> findByVecinoId(Long vecinoId);

    List<Propiedad> findByComunidadId(Long comunidadId);

    List<Propiedad> findByVecinoIdAndActivoTrue(Long vecinoId);
}
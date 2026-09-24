package com.gombeth.urban.repository;

import com.gombeth.urban.entity.Acta;
import com.gombeth.urban.entity.Comunidad;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ActaRepository extends JpaRepository<Acta, Long> {

    List<Acta> findByComunidad(Comunidad comunidad);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM Acta a
        WHERE a.id = :id
        """)
    Optional<Acta> findByIdForUpdate(
            @Param("id") Long id
    );
}
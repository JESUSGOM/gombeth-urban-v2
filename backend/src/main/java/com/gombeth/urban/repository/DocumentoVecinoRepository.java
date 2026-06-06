package com.gombeth.urban.repository;

import com.gombeth.urban.entity.DocumentoVecino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoVecinoRepository
        extends JpaRepository<DocumentoVecino, Long> {

    List<DocumentoVecino> findByVecinoId(Long vecinoId);
}
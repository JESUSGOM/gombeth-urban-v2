package com.gombeth.urban.repository;

import com.gombeth.urban.entity.VecinoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VecinoDocumentoRepository
        extends JpaRepository<VecinoDocumento, Long> {

    List<VecinoDocumento>
    findByVecinoIdOrderByFechaSubidaDescIdDesc(
            Long vecinoId
    );

    List<VecinoDocumento>
    findByVecinoIdAndTipoDocumentoOrderByFechaSubidaDescIdDesc(
            Long vecinoId,
            String tipoDocumento
    );
}
package com.gombeth.urban.controller;

import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.service.ConciliacionBancariaService;
import com.gombeth.urban.service.Norma43Service;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/norma43")
public class Norma43Controller {

    private final Norma43Service norma43Service;
    private final ConciliacionBancariaService conciliacionBancariaService;

    public Norma43Controller(
            Norma43Service norma43Service,
            ConciliacionBancariaService conciliacionBancariaService
    ) {
        this.norma43Service = norma43Service;
        this.conciliacionBancariaService = conciliacionBancariaService;
    }

    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<MovimientoBancario> importar(
            @RequestParam Long comunidadId,
            @RequestParam MultipartFile fichero
    ) throws IOException {

        String contenido = new String(
                fichero.getBytes(),
                StandardCharsets.ISO_8859_1
        );

        return norma43Service.importarContenido(
                comunidadId,
                contenido
        );
    }

    @PostMapping("/conciliar")
    public Map<String, Object> conciliar(
            @RequestParam Long comunidadId
    ) {
        int conciliados =
                conciliacionBancariaService.conciliarAutomaticamenteComunidad(
                        comunidadId
                );

        return Map.of(
                "comunidadId", comunidadId,
                "conciliados", conciliados,
                "mensaje", "Conciliación automática finalizada"
        );
    }
}
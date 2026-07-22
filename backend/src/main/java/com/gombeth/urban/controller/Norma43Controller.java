package com.gombeth.urban.controller;

import com.gombeth.urban.entity.MovimientoBancario;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.ConciliacionBancariaService;
import com.gombeth.urban.service.Norma43Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/norma43")
public class Norma43Controller {

    private final Norma43Service norma43Service;

    private final ConciliacionBancariaService
            conciliacionBancariaService;

    private final AccesoComunidadService
            accesoComunidadService;

    public Norma43Controller(
            Norma43Service norma43Service,
            ConciliacionBancariaService
                    conciliacionBancariaService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.norma43Service = norma43Service;

        this.conciliacionBancariaService =
                conciliacionBancariaService;

        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Importa un fichero Norma 43 únicamente después de
     * comprobar que el usuario autenticado tiene acceso
     * a la comunidad.
     */
    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<MovimientoBancario> importar(
            @RequestParam("comunidadId") Long comunidadId,
            @RequestParam("fichero") MultipartFile fichero,
            Authentication authentication
    ) throws IOException {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        if (
                fichero == null
                        || fichero.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un fichero Norma 43."
            );
        }

        String contenido =
                new String(
                        fichero.getBytes(),
                        StandardCharsets.ISO_8859_1
                );

        return norma43Service.importarContenido(
                comunidadId,
                contenido
        );
    }

    /**
     * Ejecuta la conciliación automática solamente para
     * una comunidad accesible por el usuario autenticado.
     */
    @PostMapping("/conciliar")
    public Map<String, Object> conciliar(
            @RequestParam("comunidadId") Long comunidadId,
            Authentication authentication
    ) {
        validarAccesoComunidad(
                authentication,
                comunidadId
        );

        int conciliados =
                conciliacionBancariaService
                        .conciliarAutomaticamenteComunidad(
                                comunidadId
                        );

        return Map.of(
                "comunidadId",
                comunidadId,
                "conciliados",
                conciliados,
                "mensaje",
                "Conciliación automática finalizada"
        );
    }

    private void validarAccesoComunidad(
            Authentication authentication,
            Long comunidadId
    ) {
        if (comunidadId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad es obligatoria."
            );
        }

        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );
    }
}
package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.AccesoComunidadService;
import com.gombeth.urban.service.PdfService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vecinos")
public class VecinoController {

    private final VecinoRepository vecinoRepository;

    private final PdfService pdfService;

    private final AccesoComunidadService
            accesoComunidadService;

    public VecinoController(
            VecinoRepository vecinoRepository,
            PdfService pdfService,
            AccesoComunidadService accesoComunidadService
    ) {
        this.vecinoRepository = vecinoRepository;
        this.pdfService = pdfService;
        this.accesoComunidadService =
                accesoComunidadService;
    }

    /**
     * Lista exclusivamente los propietarios pertenecientes
     * a las comunidades accesibles por el usuario autenticado.
     *
     * Cualquier usuarioId que Angular todavía envíe como
     * parámetro será ignorado.
     */
    @GetMapping
    public Page<Vecino> listar(
            Authentication authentication,
            Pageable pageable
    ) {
        List<Long> comunidadIds =
                accesoComunidadService
                        .listarComunidadesOrdenadas(
                                authentication
                        )
                        .stream()
                        .map(Comunidad::getId)
                        .toList();

        if (comunidadIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return vecinoRepository.findByComunidadIdIn(
                comunidadIds,
                pageable
        );
    }

    @GetMapping("/comunidad/{comunidadId}")
    public Page<Vecino> listarPorComunidad(
            @PathVariable Long comunidadId,
            @RequestParam(
                    defaultValue = "activos"
            )
            String estado,
            Pageable pageable,
            Authentication authentication
    ) {
        accesoComunidadService.validarAcceso(
                authentication,
                comunidadId
        );

        if ("bajas".equalsIgnoreCase(estado)) {
            return vecinoRepository
                    .findByComunidadIdAndActivo(
                            comunidadId,
                            false,
                            pageable
                    );
        }

        if ("todos".equalsIgnoreCase(estado)) {
            return vecinoRepository
                    .findByComunidadId(
                            comunidadId,
                            pageable
                    );
        }

        return vecinoRepository
                .findByComunidadIdAndActivo(
                        comunidadId,
                        true,
                        pageable
                );
    }

    @GetMapping("/{id}")
    public Vecino obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return obtenerVecinoAutorizado(
                id,
                authentication
        );
    }

    @PutMapping("/{id}")
    public Vecino actualizar(
            @PathVariable Long id,
            @RequestBody Vecino datos,
            Authentication authentication
    ) {
        Vecino vecino = obtenerVecinoAutorizado(
                id,
                authentication
        );

        /*
         * No se permite modificar comunidadId.
         * El propietario permanece en su comunidad actual.
         */
        vecino.setNombre(datos.getNombre());
        vecino.setNif(datos.getNif());
        vecino.setIban(datos.getIban());
        vecino.setBic(datos.getBic());
        vecino.setEmail(datos.getEmail());

        vecino.setTelefono1(datos.getTelefono1());
        vecino.setTelefono2(datos.getTelefono2());
        vecino.setTelefono3(datos.getTelefono3());

        vecino.setDireccion(datos.getDireccion());
        vecino.setPoblacion(datos.getPoblacion());
        vecino.setProvincia(datos.getProvincia());
        vecino.setCodigoPostal(
                datos.getCodigoPostal()
        );
        vecino.setPaisCod(datos.getPaisCod());

        vecino.setVivienda(datos.getVivienda());
        vecino.setDomiciliado(
                datos.isDomiciliado()
        );
        vecino.setActivo(datos.isActivo());

        vecino.setReferenciaMandato(
                datos.getReferenciaMandato()
        );
        vecino.setFechaMandato(
                datos.getFechaMandato()
        );
        vecino.setDireccionNotificacion(
                datos.getDireccionNotificacion()
        );
        vecino.setRutaMandatoFirmado(
                datos.getRutaMandatoFirmado()
        );
        vecino.setCoeficiente(
                datos.getCoeficiente()
        );
        vecino.setNotas(
                datos.getNotas()
        );

        return vecinoRepository.save(vecino);
    }

    @PostMapping
    public Vecino crear(
            @RequestBody Vecino datos,
            Authentication authentication
    ) {
        if (datos.getComunidadId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comunidad es obligatoria."
            );
        }

        /*
         * Impide crear un propietario dentro de
         * una comunidad ajena.
         */
        accesoComunidadService.validarAcceso(
                authentication,
                datos.getComunidadId()
        );

        datos.setActivo(true);

        if (
                datos.getPaisCod() == null
                        || datos.getPaisCod().isBlank()
        ) {
            datos.setPaisCod("ES");
        }

        if (
                datos.getReferenciaMandato() == null
                        || datos.getReferenciaMandato()
                        .isBlank()
        ) {
            datos.setReferenciaMandato(
                    "GTI-"
                            + System.currentTimeMillis()
                            / 1000
            );
        }

        return vecinoRepository.save(datos);
    }

    @DeleteMapping("/{id}")
    public void darDeBaja(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Vecino vecino = obtenerVecinoAutorizado(
                id,
                authentication
        );

        vecino.setActivo(false);

        vecinoRepository.save(vecino);
    }

    @GetMapping("/{id}/mandato-pdf")
    public ResponseEntity<byte[]> descargarMandatoPdf(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Vecino vecino = obtenerVecinoAutorizado(
                id,
                authentication
        );

        Comunidad comunidad =
                accesoComunidadService
                        .obtenerComunidadAutorizada(
                                authentication,
                                vecino.getComunidadId()
                        );

        byte[] pdf = pdfService.generarMandatoSepa(
                comunidad,
                vecino
        );

        String nombreArchivo =
                "mandato_sepa_"
                        + vecino.getId()
                        + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + nombreArchivo
                                + "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }

    /**
     * Obtiene un propietario y verifica que su comunidad
     * pertenece al usuario autenticado.
     */
    private Vecino obtenerVecinoAutorizado(
            Long vecinoId,
            Authentication authentication
    ) {
        Vecino vecino = vecinoRepository
                .findById(vecinoId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Propietario no encontrado "
                                        + "con ID: "
                                        + vecinoId
                        )
                );

        accesoComunidadService.validarAcceso(
                authentication,
                vecino.getComunidadId()
        );

        return vecino;
    }
}
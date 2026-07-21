package com.gombeth.urban.controller;

import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.VecinoRepository;
import com.gombeth.urban.service.PdfService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vecinos")
public class VecinoController {

    private final VecinoRepository vecinoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ComunidadRepository comunidadRepository;
    private final PdfService pdfService;

    public VecinoController(
            VecinoRepository vecinoRepository,
            JdbcTemplate jdbcTemplate,
            ComunidadRepository comunidadRepository,
            PdfService pdfService
    ) {
        this.vecinoRepository = vecinoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.comunidadRepository = comunidadRepository;
        this.pdfService = pdfService;
    }

    @GetMapping
    public Page<Vecino> listar(
            @RequestParam Long usuarioId,
            Pageable pageable
    ) {
        List<Long> comunidadIds = jdbcTemplate.queryForList(
                """
                SELECT c.id
                FROM comunidades c
                WHERE c.usuario_id = ?
                   OR c.id IN (
                        SELECT uc.comunidad_id
                        FROM usuario_comunidades uc
                        WHERE uc.usuario_id = ?
                   )
                """,
                Long.class,
                usuarioId,
                usuarioId
        );

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
            @RequestParam(defaultValue = "activos") String estado,
            Pageable pageable
    ) {
        if ("bajas".equalsIgnoreCase(estado)) {
            return vecinoRepository.findByComunidadIdAndActivo(
                    comunidadId,
                    false,
                    pageable
            );
        }

        if ("todos".equalsIgnoreCase(estado)) {
            return vecinoRepository.findByComunidadId(
                    comunidadId,
                    pageable
            );
        }

        return vecinoRepository.findByComunidadIdAndActivo(
                comunidadId,
                true,
                pageable
        );
    }

    @GetMapping("/{id}")
    public Vecino obtenerPorId(
            @PathVariable Long id
    ) {
        return vecinoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vecino no encontrado"
                        )
                );
    }

    @PutMapping("/{id}")
    public Vecino actualizar(
            @PathVariable Long id,
            @RequestBody Vecino datos
    ) {
        Vecino vecino = vecinoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vecino no encontrado"
                        )
                );

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
        vecino.setCodigoPostal(datos.getCodigoPostal());
        vecino.setPaisCod(datos.getPaisCod());

        vecino.setVivienda(datos.getVivienda());
        vecino.setDomiciliado(datos.isDomiciliado());
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
            @RequestBody Vecino datos
    ) {
        if (datos.getComunidadId() == null) {
            throw new RuntimeException(
                    "La comunidad es obligatoria"
            );
        }

        datos.setActivo(true);

        if (
                datos.getPaisCod() == null
                        || datos.getPaisCod().isBlank()
        ) {
            datos.setPaisCod("ES");
        }

        if (
                datos.getReferenciaMandato() == null
                        || datos.getReferenciaMandato().isBlank()
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
            @PathVariable Long id
    ) {
        Vecino vecino = vecinoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vecino no encontrado"
                        )
                );

        vecino.setActivo(false);
        vecinoRepository.save(vecino);
    }

    @GetMapping("/{id}/mandato-pdf")
    public ResponseEntity<byte[]> descargarMandatoPdf(
            @PathVariable Long id
    ) {
        Vecino vecino = vecinoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Vecino no encontrado"
                        )
                );

        Comunidad comunidad = comunidadRepository
                .findById(vecino.getComunidadId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Comunidad no encontrada"
                        )
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
}
package com.gombeth.urban.controller;

import com.gombeth.urban.entity.DocumentoVecino;
import com.gombeth.urban.repository.DocumentoVecinoRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoVecinoController {

    private final DocumentoVecinoRepository repository;

    public DocumentoVecinoController(
            DocumentoVecinoRepository repository
    ) {
        this.repository = repository;
    }

    @PostMapping("/{vecinoId}")
    public DocumentoVecino subirDocumento(
            @PathVariable Long vecinoId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "MANDATO")
            String tipoDocumento
    ) throws Exception {

        DocumentoVecino doc = new DocumentoVecino();

        doc.setVecinoId(vecinoId);
        doc.setTipoDocumento(tipoDocumento);
        doc.setNombreArchivo(file.getOriginalFilename());
        doc.setContentType(file.getContentType());
        doc.setContenido(file.getBytes());
        doc.setFechaSubida(LocalDateTime.now());

        return repository.save(doc);
    }

    @GetMapping("/vecino/{vecinoId}")
    public List<DocumentoVecino> listar(
            @PathVariable Long vecinoId
    ) {
        return repository.findByVecinoId(vecinoId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long id
    ) {

        DocumentoVecino doc = repository.findById(id)
                .orElseThrow();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                doc.getNombreArchivo() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                doc.getContentType()
                        )
                )
                .body(doc.getContenido());
    }
}
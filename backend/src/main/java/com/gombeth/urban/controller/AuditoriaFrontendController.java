package com.gombeth.urban.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaFrontendController {

    /**
     * El filtro transversal de auditoría registra el cuerpo
     * sanitizado de esta petición. El controlador no duplica
     * el evento: únicamente confirma su recepción.
     */
    @PostMapping("/frontend")
    public ResponseEntity<Void> frontend(
            @RequestBody(required = false)
            Map<String, Object> evento
    ) {
        return ResponseEntity
                .noContent()
                .build();
    }
}

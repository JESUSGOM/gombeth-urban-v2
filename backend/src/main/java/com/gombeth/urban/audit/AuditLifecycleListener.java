package com.gombeth.urban.audit;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLifecycleListener {

    private final AuditLogService auditLogService;

    public AuditLifecycleListener(
            AuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void aplicacionLista(
            ApplicationReadyEvent event
    ) {
        auditLogService.registrarSistema(
                "APLICACION_INICIADA",
                "CORRECTO",
                "Gombeth Urban está disponible."
        );
    }

    @EventListener
    public void aplicacionCerrada(
            ContextClosedEvent event
    ) {
        auditLogService.registrarSistema(
                "APLICACION_DETENIDA",
                "CORRECTO",
                "Gombeth Urban está cerrando."
        );
    }
}

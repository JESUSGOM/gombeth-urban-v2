package com.gombeth.urban.service;

import com.gombeth.urban.dto.GestionIncidenciaResponse;
import com.gombeth.urban.entity.GestionIncidencia;
import com.gombeth.urban.repository.GestionIncidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GestionIncidenciaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "EN_PROCESO",
            "ESPERANDO_PROVEEDOR",
            "FINALIZADA",
            "CERRADA",
            "CANCELADA"
    );

    private static final Set<String> PRIORIDADES_VALIDAS = Set.of(
            "BAJA",
            "MEDIA",
            "ALTA",
            "URGENTE"
    );

    private final GestionIncidenciaRepository repository;

    public GestionIncidenciaService(
            GestionIncidenciaRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GestionIncidenciaResponse> listarPorComunidad(
            Long comunidadId
    ) {
        if (comunidadId == null || comunidadId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la comunidad no es válido."
            );
        }

        return repository
                .findByComunidadIdOrderByFechaRegistroDesc(comunidadId)
                .stream()
                .map(GestionIncidenciaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public GestionIncidenciaResponse obtener(Long id) {
        return GestionIncidenciaResponse.desde(
                obtenerEntidad(id)
        );
    }

    @Transactional
    public GestionIncidenciaResponse guardar(
            GestionIncidencia incidencia
    ) {
        if (incidencia == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se han recibido los datos de la incidencia."
            );
        }

        validarComunidadParaAlta(incidencia);

        incidencia.setTitulo(
                validarYNormalizarTitulo(
                        incidencia.getTitulo()
                )
        );

        incidencia.setEstado(
                validarYNormalizarEstado(
                        incidencia.getEstado(),
                        "PENDIENTE"
                )
        );

        incidencia.setPrioridad(
                validarYNormalizarPrioridad(
                        incidencia.getPrioridad(),
                        "MEDIA"
                )
        );

        incidencia.setObservacionesInternas(
                normalizarTextoOpcional(
                        incidencia.getObservacionesInternas()
                )
        );

        validarCosteEstimado(
                incidencia.getCosteEstimado()
        );

        LocalDateTime ahora = LocalDateTime.now();

        if (incidencia.getFechaRegistro() == null) {
            incidencia.setFechaRegistro(ahora);
        }

        incidencia.setFechaActualizacion(ahora);

        actualizarFechasSegunEstado(
                incidencia,
                incidencia.getEstado(),
                ahora
        );

        GestionIncidencia incidenciaGuardada =
                repository.save(incidencia);

        return GestionIncidenciaResponse.desde(
                incidenciaGuardada
        );
    }

    @Transactional
    public GestionIncidenciaResponse actualizar(
            Long id,
            GestionIncidencia datos
    ) {
        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se han recibido los datos de la incidencia."
            );
        }

        GestionIncidencia incidencia =
                obtenerEntidad(id);

        incidencia.setTitulo(
                validarYNormalizarTitulo(
                        datos.getTitulo()
                )
        );

        incidencia.setDescripcion(
                datos.getDescripcion()
        );

        incidencia.setObservacionesInternas(
                normalizarTextoOpcional(
                        datos.getObservacionesInternas()
                )
        );

        incidencia.setPrioridad(
                validarYNormalizarPrioridad(
                        datos.getPrioridad(),
                        incidencia.getPrioridad()
                )
        );

        String estadoNormalizado =
                validarYNormalizarEstado(
                        datos.getEstado(),
                        incidencia.getEstado()
                );

        incidencia.setEstado(
                estadoNormalizado
        );

        validarCosteEstimado(
                datos.getCosteEstimado()
        );

        incidencia.setCosteEstimado(
                datos.getCosteEstimado()
        );

        if (datos.getComunidad() != null) {
            if (datos.getComunidad().getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La comunidad indicada no es válida."
                );
            }

            incidencia.setComunidad(
                    datos.getComunidad()
            );
        }

        if (datos.getMovimientoBancario() != null) {
            incidencia.setMovimientoBancario(
                    datos.getMovimientoBancario()
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        incidencia.setFechaActualizacion(ahora);

        actualizarFechasSegunEstado(
                incidencia,
                estadoNormalizado,
                ahora
        );

        GestionIncidencia incidenciaGuardada =
                repository.save(incidencia);

        return GestionIncidenciaResponse.desde(
                incidenciaGuardada
        );
    }

    private GestionIncidencia obtenerEntidad(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la incidencia no es válido."
            );
        }

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Incidencia no encontrada: " + id
                        )
                );
    }

    private String validarYNormalizarTitulo(
            String titulo
    ) {
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El título de la incidencia es obligatorio."
            );
        }

        String tituloNormalizado = titulo.trim();

        if (tituloNormalizado.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El título no puede superar los 255 caracteres."
            );
        }

        return tituloNormalizado;
    }

    private String validarYNormalizarEstado(
            String estado,
            String valorPorDefecto
    ) {
        String estadoNormalizado =
                normalizarValor(
                        estado,
                        valorPorDefecto
                );

        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de incidencia no válido: "
                            + estadoNormalizado
            );
        }

        return estadoNormalizado;
    }

    private String validarYNormalizarPrioridad(
            String prioridad,
            String valorPorDefecto
    ) {
        String prioridadNormalizada =
                normalizarValor(
                        prioridad,
                        valorPorDefecto
                );

        if (!PRIORIDADES_VALIDAS.contains(
                prioridadNormalizada
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Prioridad de incidencia no válida: "
                            + prioridadNormalizada
            );
        }

        return prioridadNormalizada;
    }

    private String normalizarValor(
            String valor,
            String valorPorDefecto
    ) {
        String valorSeleccionado = valor;

        if (
                valorSeleccionado == null
                        || valorSeleccionado.isBlank()
        ) {
            valorSeleccionado = valorPorDefecto;
        }

        if (
                valorSeleccionado == null
                        || valorSeleccionado.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El valor recibido no es válido."
            );
        }

        return valorSeleccionado
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizarTextoOpcional(
            String texto
    ) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    private void validarCosteEstimado(
            BigDecimal costeEstimado
    ) {
        if (
                costeEstimado != null
                        && costeEstimado.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El coste estimado no puede ser negativo."
            );
        }
    }

    private void validarComunidadParaAlta(
            GestionIncidencia incidencia
    ) {
        if (
                incidencia.getComunidad() == null
                        || incidencia.getComunidad().getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La incidencia debe estar asociada a una comunidad."
            );
        }
    }

    private void actualizarFechasSegunEstado(
            GestionIncidencia incidencia,
            String estado,
            LocalDateTime fecha
    ) {
        if (
                "FINALIZADA".equals(estado)
                        && incidencia.getFechaFinalizacion() == null
        ) {
            incidencia.setFechaFinalizacion(fecha);
        }

        if ("CERRADA".equals(estado)) {
            if (incidencia.getFechaFinalizacion() == null) {
                incidencia.setFechaFinalizacion(fecha);
            }

            if (incidencia.getFechaCierre() == null) {
                incidencia.setFechaCierre(fecha);
            }
        }
    }
}
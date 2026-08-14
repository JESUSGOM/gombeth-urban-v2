package com.gombeth.urban.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gombeth.urban.entity.Usuario;
import com.gombeth.urban.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger AUDIT_LOG =
            LoggerFactory.getLogger("AUDIT");

    private static final int MAX_BODY_CHARS = 100_000;
    private static final int MAX_VALUE_CHARS = 10_000;

    private static final Set<String> SENSITIVE_FIELD_FRAGMENTS = Set.of(
            "password",
            "contrasena",
            "contraseña",
            "passwd",
            "pwd",
            "secret",
            "token",
            "csrf",
            "xsrf",
            "authorization",
            "cookie",
            "session",
            "iban",
            "cuentabancaria",
            "cuenta_bancaria",
            "smtp_password",
            "smtppassword"
    );

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-xsrf-token",
            "x-csrf-token"
    );

    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;

    public AuditLogService(
            ObjectMapper objectMapper,
            UsuarioRepository usuarioRepository
    ) {
        this.objectMapper = objectMapper;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrarPeticion(
            ContentCachingRequestWrapper request,
            int status,
            long duracionMs,
            Authentication authenticationAntes,
            Authentication authenticationDespues,
            String contentTypeRespuesta,
            String contentLengthRespuesta,
            Throwable error
    ) {

        try {
            String username = obtenerUsername(
                    authenticationDespues,
                    authenticationAntes
            );

            Usuario usuario = obtenerUsuario(
                    username
            );

            Map<String, Object> evento =
                    crearEventoBase(
                            "HTTP"
                    );

            evento.put(
                    "accion",
                    determinarAccion(
                            request
                    )
            );

            evento.put(
                    "modulo",
                    determinarModulo(
                            request.getRequestURI()
                    )
            );

            evento.put(
                    "resultado",
                    determinarResultado(
                            status,
                            error
                    )
            );

            evento.put(
                    "usuarioId",
                    usuario == null
                            ? null
                            : usuario.getId()
            );

            evento.put(
                    "username",
                    username
            );

            evento.put(
                    "administradorId",
                    usuario == null
                            ? null
                            : usuario.getAdministradorId()
            );

            String usernameSolicitado =
                    obtenerUsernameSolicitado(
                            request
                    );

            if (
                    username == null
                    && usernameSolicitado != null
            ) {
                evento.put(
                        "usernameSolicitado",
                        usernameSolicitado
                );
            }

            evento.put(
                    "metodoHttp",
                    request.getMethod()
            );

            evento.put(
                    "ruta",
                    request.getRequestURI()
            );

            evento.put(
                    "query",
                    sanitizarParametros(
                            request.getParameterMap()
                    )
            );

            evento.put(
                    "body",
                    obtenerBodySanitizado(
                            request
                    )
            );

            evento.put(
                    "headers",
                    obtenerHeadersSanitizados(
                            request
                    )
            );

            evento.put(
                    "ipRemota",
                    request.getRemoteAddr()
            );

            evento.put(
                    "xForwardedFor",
                    valorSeguro(
                            request.getHeader(
                                    "X-Forwarded-For"
                            )
                    )
            );

            evento.put(
                    "sesion",
                    obtenerHashSesion(
                            request.getSession(false)
                    )
            );

            evento.put(
                    "statusHttp",
                    status
            );

            evento.put(
                    "duracionMs",
                    duracionMs
            );

            evento.put(
                    "contentTypeRespuesta",
                    valorSeguro(
                            contentTypeRespuesta
                    )
            );

            evento.put(
                    "contentLengthRespuesta",
                    valorSeguro(
                            contentLengthRespuesta
                    )
            );

            if (error != null) {
                evento.put(
                        "errorTipo",
                        error.getClass().getName()
                );

                evento.put(
                        "errorMensaje",
                        valorSeguro(
                                error.getMessage()
                        )
                );
            }

            escribir(
                    evento
            );

        } catch (Exception exception) {
            LoggerFactory
                    .getLogger(
                            AuditLogService.class
                    )
                    .error(
                            "No se pudo escribir un evento de auditoría",
                            exception
                    );
        }
    }

    public void registrarSistema(
            String accion,
            String resultado,
            String detalle
    ) {

        Map<String, Object> evento =
                crearEventoBase(
                        "SISTEMA"
                );

        evento.put(
                "accion",
                accion
        );

        evento.put(
                "resultado",
                resultado
        );

        evento.put(
                "detalle",
                valorSeguro(
                        detalle
                )
        );

        escribir(
                evento
        );
    }

    private Map<String, Object> crearEventoBase(
            String tipo
    ) {

        Map<String, Object> evento =
                new LinkedHashMap<>();

        evento.put(
                "eventoId",
                UUID.randomUUID().toString()
        );

        evento.put(
                "timestampUtc",
                Instant.now().toString()
        );

        evento.put(
                "tipo",
                tipo
        );

        return evento;
    }

    private void escribir(
            Map<String, Object> evento
    ) {

        try {
            AUDIT_LOG.info(
                    objectMapper.writeValueAsString(
                            evento
                    )
            );

        } catch (JsonProcessingException exception) {
            AUDIT_LOG.info(
                    "{\"tipo\":\"AUDIT_SERIALIZATION_ERROR\"," +
                            "\"mensaje\":\"{}\"}",
                    limpiarTexto(
                            exception.getMessage()
                    )
            );
        }
    }

    private Usuario obtenerUsuario(
            String username
    ) {

        if (!StringUtils.hasText(username)) {
            return null;
        }

        return usuarioRepository
                .findByUsername(
                        username
                )
                .orElse(null);
    }

    private String obtenerUsername(
            Authentication authenticationDespues,
            Authentication authenticationAntes
    ) {

        String despues =
                usernameValido(
                        authenticationDespues
                );

        if (despues != null) {
            return despues;
        }

        return usernameValido(
                authenticationAntes
        );
    }

    private String usernameValido(
            Authentication authentication
    ) {

        if (
                authentication == null
                || !authentication.isAuthenticated()
        ) {
            return null;
        }

        String nombre =
                authentication.getName();

        if (
                !StringUtils.hasText(nombre)
                || "anonymousUser".equalsIgnoreCase(
                        nombre
                )
        ) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (
                principal instanceof UserDetails userDetails
                && StringUtils.hasText(
                        userDetails.getUsername()
                )
        ) {
            return userDetails.getUsername();
        }

        return nombre;
    }

    private String obtenerUsernameSolicitado(
            ContentCachingRequestWrapper request
    ) {

        if (
                !"/api/auth/login".equals(
                        request.getRequestURI()
                )
                && !"/api/auth/cambiar-password".equals(
                        request.getRequestURI()
                )
        ) {
            return null;
        }

        byte[] contenido =
                request.getContentAsByteArray();

        if (contenido.length == 0) {
            return null;
        }

        try {
            JsonNode root =
                    objectMapper.readTree(
                            contenido
                    );

            JsonNode username =
                    root.get(
                            "username"
                    );

            if (
                    username == null
                    || username.isNull()
            ) {
                return null;
            }

            return valorSeguro(
                    username.asText()
            );

        } catch (Exception ignored) {
            return null;
        }
    }

    private String determinarAccion(
            HttpServletRequest request
    ) {

        String uri =
                request.getRequestURI();

        if (
                "/api/auth/login".equals(uri)
        ) {
            return "LOGIN";
        }

        if (
                "/api/auth/logout".equals(uri)
        ) {
            return "LOGOUT";
        }

        if (
                "/api/auth/cambiar-password".equals(uri)
        ) {
            return "CAMBIO_PASSWORD";
        }

        if (
                "/api/auth/me".equals(uri)
        ) {
            return "COMPROBAR_SESION";
        }

        if (
                "/api/auth/csrf".equals(uri)
        ) {
            return "OBTENER_CSRF";
        }

        if (
                "/api/auditoria/frontend".equals(uri)
        ) {
            return "EVENTO_FRONTEND";
        }

        return switch (
                request.getMethod()
                        .toUpperCase(
                                Locale.ROOT
                        )
        ) {
            case "GET", "HEAD" ->
                    "CONSULTA";

            case "POST" ->
                    "CREACION_O_EJECUCION";

            case "PUT", "PATCH" ->
                    "MODIFICACION";

            case "DELETE" ->
                    "ELIMINACION";

            case "OPTIONS" ->
                    "CORS_PREFLIGHT";

            default ->
                    "PETICION_HTTP";
        };
    }

    private String determinarModulo(
            String uri
    ) {

        if (!StringUtils.hasText(uri)) {
            return null;
        }

        String[] partes =
                uri.split("/");

        if (
                partes.length >= 4
                && "api".equals(
                        partes[1]
                )
        ) {
            return partes[2];
        }

        if (
                partes.length >= 3
                && "api".equals(
                        partes[1]
                )
        ) {
            return partes[2];
        }

        return "aplicacion";
    }

    private String determinarResultado(
            int status,
            Throwable error
    ) {

        if (error != null) {
            return "ERROR";
        }

        if (
                status == 401
                || status == 403
        ) {
            return "DENEGADO";
        }

        if (status >= 400) {
            return "ERROR";
        }

        return "CORRECTO";
    }

    private Map<String, Object> sanitizarParametros(
            Map<String, String[]> parametros
    ) {

        if (
                parametros == null
                || parametros.isEmpty()
        ) {
            return Collections.emptyMap();
        }

        Map<String, Object> salida =
                new LinkedHashMap<>();

        parametros.forEach((clave, valores) -> {

            if (esCampoSensible(clave)) {
                salida.put(
                        clave,
                        "***PROTEGIDO***"
                );
                return;
            }

            List<String> lista =
                    new ArrayList<>();

            if (valores != null) {
                for (String valor : valores) {
                    lista.add(
                            valorSeguro(
                                    valor
                            )
                    );
                }
            }

            salida.put(
                    clave,
                    lista
            );
        });

        return salida;
    }

    private Map<String, String> obtenerHeadersSanitizados(
            HttpServletRequest request
    ) {

        Map<String, String> headers =
                new LinkedHashMap<>();

        Enumeration<String> nombres =
                request.getHeaderNames();

        if (nombres == null) {
            return headers;
        }

        while (nombres.hasMoreElements()) {

            String nombre =
                    nombres.nextElement();

            String nombreNormalizado =
                    nombre.toLowerCase(
                            Locale.ROOT
                    );

            if (
                    SENSITIVE_HEADERS.contains(
                            nombreNormalizado
                    )
            ) {
                headers.put(
                        nombre,
                        "***PROTEGIDO***"
                );
                continue;
            }

            headers.put(
                    nombre,
                    valorSeguro(
                            request.getHeader(
                                    nombre
                            )
                    )
            );
        }

        return headers;
    }

    private Object obtenerBodySanitizado(
            ContentCachingRequestWrapper request
    ) {

        String metodo =
                request.getMethod();

        if (
                "GET".equalsIgnoreCase(metodo)
                || "HEAD".equalsIgnoreCase(metodo)
                || "OPTIONS".equalsIgnoreCase(metodo)
        ) {
            return null;
        }

        String contentType =
                request.getContentType();

        if (
                contentType != null
                && contentType.toLowerCase(
                        Locale.ROOT
                ).startsWith(
                        "multipart/"
                )
        ) {
            Map<String, Object> multipart =
                    new LinkedHashMap<>();

            multipart.put(
                    "tipo",
                    "MULTIPART"
            );

            multipart.put(
                    "contentLength",
                    request.getContentLengthLong()
            );

            multipart.put(
                    "parametros",
                    sanitizarParametros(
                            request.getParameterMap()
                    )
            );

            multipart.put(
                    "nota",
                    "Los bytes de archivos no se registran en auditoría."
            );

            return multipart;
        }

        if (
                contentType != null
                && contentType.toLowerCase(
                        Locale.ROOT
                ).startsWith(
                        "application/x-www-form-urlencoded"
                )
        ) {
            return sanitizarParametros(
                    request.getParameterMap()
            );
        }

        byte[] contenido =
                request.getContentAsByteArray();

        if (contenido.length == 0) {
            return null;
        }

        String texto =
                new String(
                        contenido,
                        StandardCharsets.UTF_8
                );

        boolean truncado =
                texto.length()
                        > MAX_BODY_CHARS;

        if (truncado) {
            texto = texto.substring(
                    0,
                    MAX_BODY_CHARS
            );
        }

        if (
                contentType != null
                && contentType.toLowerCase(
                        Locale.ROOT
                ).contains(
                        "json"
                )
        ) {
            try {
                JsonNode root =
                        objectMapper.readTree(
                                texto
                        );

                JsonNode sanitizado =
                        sanitizarJson(
                                root,
                                null
                        );

                if (!truncado) {
                    return sanitizado;
                }

                Map<String, Object> salida =
                        new LinkedHashMap<>();

                salida.put(
                        "contenido",
                        sanitizado
                );

                salida.put(
                        "truncado",
                        true
                );

                return salida;

            } catch (Exception ignored) {
                // Continúa como texto plano.
            }
        }

        Map<String, Object> salida =
                new LinkedHashMap<>();

        salida.put(
                "contenido",
                sanitizarTextoLibre(
                        texto
                )
        );

        if (truncado) {
            salida.put(
                    "truncado",
                    true
            );
        }

        return salida;
    }

    private JsonNode sanitizarJson(
            JsonNode nodo,
            String nombreCampo
    ) {

        if (nodo == null || nodo.isNull()) {
            return objectMapper.nullNode();
        }

        if (
                nombreCampo != null
                && esCampoSensible(
                        nombreCampo
                )
        ) {
            return objectMapper
                    .getNodeFactory()
                    .textNode(
                            "***PROTEGIDO***"
                    );
        }

        if (nodo.isObject()) {

            ObjectNode salida =
                    objectMapper.createObjectNode();

            nodo.fields().forEachRemaining(entry ->
                    salida.set(
                            entry.getKey(),
                            sanitizarJson(
                                    entry.getValue(),
                                    entry.getKey()
                            )
                    )
            );

            return salida;
        }

        if (nodo.isArray()) {

            ArrayNode salida =
                    objectMapper.createArrayNode();

            nodo.forEach(elemento ->
                    salida.add(
                            sanitizarJson(
                                    elemento,
                                    nombreCampo
                            )
                    )
            );

            return salida;
        }

        if (nodo.isTextual()) {
            return objectMapper
                    .getNodeFactory()
                    .textNode(
                            valorSeguro(
                                    nodo.asText()
                            )
                    );
        }

        return nodo;
    }

    private String sanitizarTextoLibre(
            String texto
    ) {

        if (texto == null) {
            return null;
        }

        return limpiarTexto(
                texto
        )
                .replaceAll(
                        "(?i)(password|contraseña|contrasena|passwd|pwd)\\s*[:=]\\s*[^,&\\s]+",
                        "$1=***PROTEGIDO***"
                )
                .replaceAll(
                        "(?i)(token|authorization|csrf|xsrf)\\s*[:=]\\s*[^,&\\s]+",
                        "$1=***PROTEGIDO***"
                );
    }

    private boolean esCampoSensible(
            String nombre
    ) {

        if (nombre == null) {
            return false;
        }

        String normalizado =
                nombre.toLowerCase(
                        Locale.ROOT
                )
                        .replace("-", "")
                        .replace("_", "")
                        .replace(" ", "");

        return SENSITIVE_FIELD_FRAGMENTS
                .stream()
                .map(fragmento ->
                        fragmento
                                .replace("-", "")
                                .replace("_", "")
                                .replace(" ", "")
                )
                .anyMatch(
                        normalizado::contains
                );
    }

    private String obtenerHashSesion(
            HttpSession session
    ) {

        if (session == null) {
            return null;
        }

        try {
            byte[] digest =
                    MessageDigest
                            .getInstance(
                                    "SHA-256"
                            )
                            .digest(
                                    session
                                            .getId()
                                            .getBytes(
                                                    StandardCharsets.UTF_8
                                            )
                            );

            StringBuilder hex =
                    new StringBuilder();

            for (byte valor : digest) {
                hex.append(
                        String.format(
                                "%02x",
                                valor
                        )
                );
            }

            return hex.substring(
                    0,
                    16
            );

        } catch (Exception exception) {
            return "NO_DISPONIBLE";
        }
    }

    private String valorSeguro(
            Object valor
    ) {

        if (valor == null) {
            return null;
        }

        String texto =
                limpiarTexto(
                        String.valueOf(
                                valor
                        )
                );

        if (
                texto.length()
                        <= MAX_VALUE_CHARS
        ) {
            return texto;
        }

        return texto.substring(
                0,
                MAX_VALUE_CHARS
        ) + "…[TRUNCADO]";
    }

    private String limpiarTexto(
            String texto
    ) {

        if (texto == null) {
            return null;
        }

        return texto
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}

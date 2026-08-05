package com.gombeth.urban.service;

import com.gombeth.urban.entity.Administrador;
import com.gombeth.urban.entity.Comunidad;
import com.gombeth.urban.entity.ContabilidadRecibo;
import com.gombeth.urban.entity.Vecino;
import com.gombeth.urban.repository.AdministradorRepository;
import com.gombeth.urban.repository.ComunidadRepository;
import com.gombeth.urban.repository.ContabilidadReciboRepository;
import com.gombeth.urban.repository.VecinoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
public class ReciboEmailService {

    private static final DateTimeFormatter FECHA_ES =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContabilidadReciboRepository
            reciboRepository;

    private final ComunidadRepository
            comunidadRepository;

    private final VecinoRepository vecinoRepository;

    private final AdministradorRepository
            administradorRepository;

    private final PdfService pdfService;

    private final EmailService emailService;

    public ReciboEmailService(
            ContabilidadReciboRepository reciboRepository,
            ComunidadRepository comunidadRepository,
            VecinoRepository vecinoRepository,
            AdministradorRepository administradorRepository,
            PdfService pdfService,
            EmailService emailService
    ) {
        this.reciboRepository = reciboRepository;
        this.comunidadRepository = comunidadRepository;
        this.vecinoRepository = vecinoRepository;
        this.administradorRepository =
                administradorRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public ResultadoEnvio enviarRecibo(
            Long reciboId
    ) {
        ContabilidadRecibo recibo = reciboRepository
                .findById(reciboId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Recibo no encontrado con ID: "
                                        + reciboId
                        )
                );

        Comunidad comunidad = comunidadRepository
                .findById(recibo.getComunidadId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró la comunidad del recibo."
                        )
                );

        Vecino vecino = vecinoRepository
                .findById(recibo.getVecinoId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el propietario del recibo."
                        )
                );

        if (vecino.getEmail() == null
                || vecino.getEmail().isBlank()) {
            throw new IllegalStateException(
                    "El propietario "
                            + vecino.getNombre()
                            + " no tiene correo electrónico."
            );
        }

        if (comunidad.getAdministradorId() == null) {
            throw new IllegalStateException(
                    "La comunidad no tiene administrador asignado."
            );
        }

        Administrador administrador =
                administradorRepository
                        .findById(
                                comunidad.getAdministradorId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encontró el administrador "
                                                + "de la comunidad."
                                )
                        );

        byte[] pdf = pdfService.generarReciboPdf(
                recibo,
                comunidad,
                vecino
        );

        String nombreArchivo =
                pdfService.nombreArchivoRecibo(
                        recibo,
                        vecino
                );

        String asunto = "Recibo "
                + recibo.getId()
                + " - "
                + comunidad.getNombre();

        String cuerpoTexto = construirCuerpoTexto(
                recibo,
                comunidad,
                vecino,
                administrador
        );

        String cuerpoHtml = construirCuerpoHtml(
                recibo,
                comunidad,
                vecino,
                administrador
        );

        emailService.enviarConAdjuntoHtml(
                vecino.getEmail(),
                asunto,
                cuerpoTexto,
                cuerpoHtml,
                pdf,
                nombreArchivo,
                administrador
        );

        return new ResultadoEnvio(
                recibo.getId(),
                vecino.getEmail(),
                "Recibo enviado correctamente a "
                        + vecino.getEmail()
                        + "."
        );
    }

    private String construirCuerpoTexto(
            ContabilidadRecibo recibo,
            Comunidad comunidad,
            Vecino vecino,
            Administrador administrador
    ) {
        return "Hola "
                + texto(vecino.getNombre())
                + ",\n\n"
                + "Le remitimos el recibo correspondiente a "
                + texto(comunidad.getNombre())
                + ".\n\n"
                + "Referencia: "
                + recibo.getId()
                + "\n"
                + "Fecha de emisión: "
                + fecha(recibo)
                + "\n"
                + "Concepto: "
                + texto(recibo.getConcepto())
                + "\n"
                + "Importe: "
                + importe(recibo.getImporte())
                + " €\n"
                + "Estado: "
                + texto(recibo.getEstado())
                + "\n\n"
                + "El PDF oficial se encuentra adjunto a este correo.\n\n"
                + "Atentamente,\n"
                + nombreAdministrador(administrador)
                + "\n"
                + "Gombeth Urban";
    }

    private String construirCuerpoHtml(
            ContabilidadRecibo recibo,
            Comunidad comunidad,
            Vecino vecino,
            Administrador administrador
    ) {
        String estado = texto(recibo.getEstado());
        boolean cobrado = "COBRADO".equalsIgnoreCase(estado);

        String colorEstado = cobrado
                ? "#1f7a4e"
                : "#a06000";

        String fondoEstado = cobrado
                ? "#e2f5eb"
                : "#fff3d6";

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Recibo de comunidad</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6f9;font-family:Arial,Helvetica,sans-serif;color:#1f2933;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f3f6f9;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="620" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:620px;background:#ffffff;border:1px solid #d7dee6;border-radius:10px;overflow:hidden;">
                          <tr>
                            <td style="background:#14406c;padding:22px 26px;color:#ffffff;">
                              <div style="font-size:22px;font-weight:700;">Gombeth Urban</div>
                              <div style="font-size:13px;margin-top:4px;opacity:.9;">Gestión profesional de comunidades</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 26px 10px 26px;">
                              <div style="font-size:22px;font-weight:700;color:#14406c;">Recibo de comunidad</div>
                              <p style="font-size:15px;line-height:1.6;margin:18px 0 0 0;">
                                Hola <strong>%s</strong>,
                              </p>
                              <p style="font-size:15px;line-height:1.6;margin:10px 0 0 0;">
                                Le remitimos el recibo correspondiente a
                                <strong>%s</strong>.
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:14px 26px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;border:1px solid #d7dee6;">
                                <tr>
                                  <td style="padding:11px 13px;background:#f5f7fa;font-size:13px;font-weight:700;width:34%%;">Referencia</td>
                                  <td style="padding:11px 13px;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:11px 13px;background:#f5f7fa;font-size:13px;font-weight:700;">Fecha de emisión</td>
                                  <td style="padding:11px 13px;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:11px 13px;background:#f5f7fa;font-size:13px;font-weight:700;">Concepto</td>
                                  <td style="padding:11px 13px;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:11px 13px;background:#f5f7fa;font-size:13px;font-weight:700;">Vivienda</td>
                                  <td style="padding:11px 13px;font-size:13px;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:11px 13px;background:#f5f7fa;font-size:13px;font-weight:700;">Estado</td>
                                  <td style="padding:11px 13px;font-size:13px;">
                                    <span style="display:inline-block;padding:5px 10px;border-radius:14px;background:%s;color:%s;font-weight:700;">%s</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:4px 26px 16px 26px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="background:#ebf4fc;border:1px solid #9ebbd4;padding:18px;text-align:center;">
                                    <div style="font-size:12px;color:#52606d;text-transform:uppercase;letter-spacing:.5px;">Importe total</div>
                                    <div style="font-size:28px;font-weight:700;color:#14406c;margin-top:5px;">%s €</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 26px 24px 26px;">
                              <p style="font-size:14px;line-height:1.6;margin:0;">
                                El PDF oficial del recibo se encuentra adjunto a este correo.
                              </p>
                              <p style="font-size:14px;line-height:1.6;margin:18px 0 0 0;">
                                Atentamente,<br>
                                <strong>%s</strong><br>
                                Administración de la comunidad
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f5f7fa;border-top:1px solid #d7dee6;padding:16px 26px;text-align:center;color:#66717e;font-size:11px;line-height:1.5;">
                              Este correo ha sido generado por Gombeth Urban.<br>
                              Por favor, no responda automáticamente a este mensaje.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                html(vecino.getNombre()),
                html(comunidad.getNombre()),
                recibo.getId(),
                html(fecha(recibo)),
                html(recibo.getConcepto()),
                html(vecino.getVivienda()),
                fondoEstado,
                colorEstado,
                html(capitalizar(estado)),
                importe(recibo.getImporte()),
                html(nombreAdministrador(administrador))
        );
    }

    private String nombreAdministrador(
            Administrador administrador
    ) {
        if (administrador == null
                || administrador.getNombre() == null
                || administrador.getNombre().isBlank()) {
            return "Administración de la comunidad";
        }

        return administrador.getNombre().trim();
    }

    private String fecha(
            ContabilidadRecibo recibo
    ) {
        return recibo.getFechaEmision() != null
                ? FECHA_ES.format(recibo.getFechaEmision())
                : "";
    }

    private String importe(
            BigDecimal valor
    ) {
        BigDecimal seguro = valor != null
                ? valor
                : BigDecimal.ZERO;

        return seguro
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    private String capitalizar(
            String valor
    ) {
        String limpio = texto(valor).toLowerCase();

        if (limpio.isBlank()) {
            return "";
        }

        return limpio.substring(0, 1).toUpperCase()
                + limpio.substring(1);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String html(String valor) {
        return texto(valor)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record ResultadoEnvio(
            Long reciboId,
            String destinatario,
            String mensaje
    ) {
    }
}

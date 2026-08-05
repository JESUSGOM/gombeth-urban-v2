package com.gombeth.urban.service;

import com.gombeth.urban.entity.Administrador;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailService.class);

    public void enviarConAdjunto(
            String destinatario,
            String asunto,
            String cuerpo,
            byte[] adjunto,
            String nombreAdjunto,
            Administrador administrador
    ) {
        enviarConAdjuntoHtml(
                destinatario,
                asunto,
                cuerpo,
                null,
                adjunto,
                nombreAdjunto,
                administrador
        );
    }

    public void enviarConAdjuntoHtml(
            String destinatario,
            String asunto,
            String cuerpoTexto,
            String cuerpoHtml,
            byte[] adjunto,
            String nombreAdjunto,
            Administrador administrador
    ) {
        validarDatos(
                destinatario,
                asunto,
                adjunto,
                nombreAdjunto,
                administrador
        );

        try {
            JavaMailSenderImpl mailSender =
                    crearMailSender(administrador);

            MimeMessage mensaje =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(
                    administrador.getSmtpUsername()
            );
            helper.setTo(destinatario.trim());
            helper.setSubject(asunto.trim());

            String textoSeguro =
                    cuerpoTexto == null
                            ? ""
                            : cuerpoTexto;

            if (cuerpoHtml != null
                    && !cuerpoHtml.isBlank()) {
                helper.setText(
                        textoSeguro,
                        cuerpoHtml
                );
            } else {
                helper.setText(
                        textoSeguro,
                        false
                );
            }

            helper.addAttachment(
                    nombreAdjunto,
                    new ByteArrayResource(adjunto),
                    "application/pdf"
            );

            mailSender.send(mensaje);

            LOGGER.info(
                    "Recibo enviado por correo a {} mediante {}.",
                    destinatario,
                    administrador.getSmtpHost()
            );

        } catch (Exception excepcion) {
            LOGGER.error(
                    "No se pudo enviar el recibo a {} mediante {}.",
                    destinatario,
                    administrador.getSmtpHost(),
                    excepcion
            );

            throw new IllegalStateException(
                    "No se pudo enviar el correo al propietario. "
                            + mensajeSeguro(excepcion),
                    excepcion
            );
        }
    }

    private JavaMailSenderImpl crearMailSender(
            Administrador administrador
    ) {
        JavaMailSenderImpl mailSender =
                new JavaMailSenderImpl();

        int puerto = administrador.getSmtpPort() != null
                ? administrador.getSmtpPort()
                : 587;

        mailSender.setHost(
                administrador.getSmtpHost().trim()
        );
        mailSender.setPort(puerto);
        mailSender.setUsername(
                administrador.getSmtpUsername().trim()
        );
        mailSender.setPassword(
                administrador.getSmtpPassword()
        );
        mailSender.setDefaultEncoding("UTF-8");

        Properties propiedades =
                mailSender.getJavaMailProperties();

        propiedades.put(
                "mail.smtp.auth",
                String.valueOf(administrador.isSmtpAuth())
        );
        propiedades.put(
                "mail.smtp.connectiontimeout",
                "10000"
        );
        propiedades.put(
                "mail.smtp.timeout",
                "10000"
        );
        propiedades.put(
                "mail.smtp.writetimeout",
                "10000"
        );
        propiedades.put(
                "mail.smtp.ssl.trust",
                administrador.getSmtpHost().trim()
        );

        if (puerto == 465) {
            propiedades.put(
                    "mail.smtp.ssl.enable",
                    "true"
            );
            propiedades.put(
                    "mail.smtp.starttls.enable",
                    "false"
            );
        } else {
            propiedades.put(
                    "mail.smtp.ssl.enable",
                    "false"
            );
            propiedades.put(
                    "mail.smtp.starttls.enable",
                    String.valueOf(
                            administrador.isSmtpStarttls()
                    )
            );
            propiedades.put(
                    "mail.smtp.starttls.required",
                    "false"
            );
        }

        return mailSender;
    }

    private void validarDatos(
            String destinatario,
            String asunto,
            byte[] adjunto,
            String nombreAdjunto,
            Administrador administrador
    ) {
        if (destinatario == null
                || destinatario.isBlank()) {
            throw new IllegalArgumentException(
                    "El propietario no tiene correo electrónico."
            );
        }

        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException(
                    "El asunto del correo es obligatorio."
            );
        }

        if (adjunto == null || adjunto.length == 0) {
            throw new IllegalArgumentException(
                    "El PDF del recibo está vacío."
            );
        }

        if (nombreAdjunto == null
                || nombreAdjunto.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del PDF es obligatorio."
            );
        }

        if (administrador == null) {
            throw new IllegalArgumentException(
                    "No se encontró el administrador de la comunidad."
            );
        }

        if (administrador.getSmtpHost() == null
                || administrador.getSmtpHost().isBlank()
                || administrador.getSmtpUsername() == null
                || administrador.getSmtpUsername().isBlank()
                || administrador.getSmtpPassword() == null
                || administrador.getSmtpPassword().isBlank()) {
            throw new IllegalStateException(
                    "El administrador no tiene completa "
                            + "la configuración SMTP."
            );
        }
    }

    private String mensajeSeguro(
            Exception excepcion
    ) {
        String mensaje = excepcion.getMessage();

        return mensaje == null || mensaje.isBlank()
                ? "Revise la configuración SMTP."
                : mensaje;
    }
}

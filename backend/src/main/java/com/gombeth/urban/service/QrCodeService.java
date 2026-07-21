package com.gombeth.urban.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int TAMANO_PREDETERMINADO = 400;

    public byte[] generarQrPng(String contenido) {
        return generarQrPng(
                contenido,
                TAMANO_PREDETERMINADO,
                TAMANO_PREDETERMINADO
        );
    }

    public byte[] generarQrPng(
            String contenido,
            int ancho,
            int alto
    ) {
        validarParametros(contenido, ancho, alto);

        Map<EncodeHintType, Object> configuracion =
                new EnumMap<>(EncodeHintType.class);

        configuracion.put(
                EncodeHintType.CHARACTER_SET,
                "UTF-8"
        );

        configuracion.put(
                EncodeHintType.ERROR_CORRECTION,
                ErrorCorrectionLevel.M
        );

        configuracion.put(
                EncodeHintType.MARGIN,
                2
        );

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {
            BitMatrix matriz = qrCodeWriter.encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    ancho,
                    alto,
                    configuracion
            );

            try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(
                        matriz,
                        "PNG",
                        salida
                );

                return salida.toByteArray();
            }

        } catch (WriterException | IOException e) {
            throw new IllegalStateException(
                    "No se pudo generar el código QR.",
                    e
            );
        }
    }

    private void validarParametros(
            String contenido,
            int ancho,
            int alto
    ) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del código QR es obligatorio."
            );
        }

        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException(
                    "El ancho y el alto del código QR deben ser mayores que cero."
            );
        }
    }
}
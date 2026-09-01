package com.gombeth.urban.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class BraveBrowserLauncher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BraveBrowserLauncher.class);

    private final boolean abrirNavegador;
    private final int puertoServidor;

    public BraveBrowserLauncher(
            @Value("${gombeth.desktop.open-browser:false}")
            boolean abrirNavegador,

            @Value("${server.port:8080}")
            int puertoServidor
    ) {
        this.abrirNavegador = abrirNavegador;
        this.puertoServidor = puertoServidor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void abrirBraveCuandoAplicacionEstePreparada() {

        if (!abrirNavegador) {
            return;
        }

        if (!esWindows()) {
            LOGGER.warn(
                    "La apertura automática de Brave sólo está habilitada en Windows."
            );
            return;
        }

        String url =
                "http://localhost:" + puertoServidor + "/login";

        Optional<Path> brave =
                localizarBrave();

        if (brave.isPresent()) {

            try {

                new ProcessBuilder(
                        brave.get().toString(),
                        "--new-window",
                        url
                ).start();

                LOGGER.info(
                        "Brave abierto automáticamente en {}",
                        url
                );

                return;

            } catch (IOException ex) {

                LOGGER.warn(
                        "No se ha podido abrir Brave desde {}.",
                        brave.get(),
                        ex
                );
            }
        }

        LOGGER.warn(
                "No se ha localizado brave.exe. " +
                        "Se intentará abrir la URL con el navegador predeterminado."
        );

        abrirConNavegadorPredeterminado(url);
    }

    private Optional<Path> localizarBrave() {

        List<Path> candidatos =
                new ArrayList<>();

        agregarCandidato(
                candidatos,
                System.getenv("ProgramFiles")
        );

        agregarCandidato(
                candidatos,
                System.getenv("ProgramFiles(x86)")
        );

        String localAppData =
                System.getenv("LOCALAPPDATA");

        if (
                localAppData != null
                        && !localAppData.isBlank()
        ) {

            candidatos.add(
                    Path.of(
                            localAppData,
                            "BraveSoftware",
                            "Brave-Browser",
                            "Application",
                            "brave.exe"
                    )
            );
        }

        return candidatos
                .stream()
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private void agregarCandidato(
            List<Path> candidatos,
            String carpetaProgramFiles
    ) {

        if (
                carpetaProgramFiles == null
                        || carpetaProgramFiles.isBlank()
        ) {
            return;
        }

        candidatos.add(
                Path.of(
                        carpetaProgramFiles,
                        "BraveSoftware",
                        "Brave-Browser",
                        "Application",
                        "brave.exe"
                )
        );
    }

    private void abrirConNavegadorPredeterminado(
            String url
    ) {

        try {

            new ProcessBuilder(
                    "rundll32.exe",
                    "url.dll,FileProtocolHandler",
                    url
            ).start();

        } catch (IOException ex) {

            LOGGER.error(
                    "No se ha podido abrir automáticamente el navegador.",
                    ex
            );
        }
    }

    private boolean esWindows() {

        String sistemaOperativo =
                System.getProperty(
                        "os.name",
                        ""
                );

        return sistemaOperativo
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}
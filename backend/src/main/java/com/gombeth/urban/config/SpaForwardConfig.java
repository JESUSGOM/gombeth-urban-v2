package com.gombeth.urban.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

    private static final Resource INDEX_HTML =
            new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {

                    @Override
                    protected Resource getResource(
                            String resourcePath,
                            Resource location
                    ) throws IOException {

                        if (
                                resourcePath == null
                                        || resourcePath.isBlank()
                        ) {
                            return INDEX_HTML;
                        }

                        Resource recursoSolicitado =
                                super.getResource(
                                        resourcePath,
                                        location
                                );

                        if (recursoSolicitado != null) {
                            return recursoSolicitado;
                        }

                        if (esRutaAngular(resourcePath)) {
                            return INDEX_HTML;
                        }

                        return null;
                    }
                });
    }

    private boolean esRutaAngular(
            String resourcePath
    ) {
        String rutaNormalizada =
                resourcePath.replace('\\', '/');

        if (
                rutaNormalizada.equals("api")
                        || rutaNormalizada.startsWith("api/")
                        || rutaNormalizada.equals("error")
                        || rutaNormalizada.startsWith("error/")
        ) {
            return false;
        }

        /*
         * Los archivos estáticos contienen una extensión:
         * .js, .css, .png, .ico, .woff, etc.
         *
         * Si un archivo no existe, debe devolver 404 y no
         * responder incorrectamente con index.html.
         */
        return !rutaNormalizada.contains(".");
    }
}
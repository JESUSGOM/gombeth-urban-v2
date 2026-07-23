package com.gombeth.urban.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(
            ViewControllerRegistry registry
    ) {

        registry.addViewController("/")
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/{path:[^\\.]*}"
                )
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/{path1:[^\\.]*}/{path2:[^\\.]*}"
                )
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}"
                )
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/vecinos/nuevo/comunidad/{comunidadId}"
                )
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/conceptos/comunidad/{comunidadId}/nuevo"
                )
                .setViewName("forward:/index.html");

        registry.addViewController(
                        "/conceptos/comunidad/{comunidadId}/editar/{conceptoId}"
                )
                .setViewName("forward:/index.html");
    }
}
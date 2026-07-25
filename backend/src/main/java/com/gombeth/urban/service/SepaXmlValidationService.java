package com.gombeth.urban.service;

import com.gombeth.urban.dto.SepaValidacionResultado;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@Service
public class SepaXmlValidationService {

    private static final String RUTA_XSD =
            "xsd/EPC130-08_2025_V1.0_pain.008.001.08.xsd";

    public SepaValidacionResultado validar(
            String xml
    ) {
        SepaValidacionResultado resultado =
                new SepaValidacionResultado();

        if (
                xml == null
                        || xml.isBlank()
        ) {
            resultado.addError(
                    "El XML SEPA está vacío."
            );

            return resultado;
        }

        ClassPathResource recursoXsd =
                new ClassPathResource(
                        RUTA_XSD
                );

        if (!recursoXsd.exists()) {
            resultado.addError(
                    "No se encuentra el esquema XSD oficial SEPA en el classpath: "
                            + RUTA_XSD
            );

            return resultado;
        }

        try (
                InputStream entradaXsd =
                        recursoXsd.getInputStream()
        ) {
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(
                            XMLConstants.W3C_XML_SCHEMA_NS_URI
                    );

            configurarSeguridad(
                    schemaFactory
            );

            Schema schema =
                    schemaFactory.newSchema(
                            new StreamSource(
                                    entradaXsd
                            )
                    );

            Validator validator =
                    schema.newValidator();

            configurarSeguridad(
                    validator
            );

            validator.validate(
                    new StreamSource(
                            new StringReader(
                                    xml
                            )
                    )
            );

        } catch (SAXParseException error) {
            resultado.addError(
                    construirMensajeValidacion(
                            error
                    )
            );

        } catch (SAXException error) {
            resultado.addError(
                    "El XML SEPA no cumple el esquema XSD oficial: "
                            + mensajeSeguro(
                            error
                    )
            );

        } catch (IOException error) {
            resultado.addError(
                    "No se pudo leer el esquema XSD oficial SEPA: "
                            + mensajeSeguro(
                            error
                    )
            );

        } catch (RuntimeException error) {
            resultado.addError(
                    "No se pudo validar el XML SEPA: "
                            + mensajeSeguro(
                            error
                    )
            );
        }

        return resultado;
    }

    private void configurarSeguridad(
            SchemaFactory schemaFactory
    ) throws SAXException {
        schemaFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        schemaFactory.setProperty(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        schemaFactory.setProperty(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );
    }

    private void configurarSeguridad(
            Validator validator
    ) throws SAXException {
        validator.setProperty(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        validator.setProperty(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );
    }

    private String construirMensajeValidacion(
            SAXParseException error
    ) {
        StringBuilder mensaje =
                new StringBuilder(
                        "El XML SEPA no cumple el esquema XSD oficial"
                );

        if (error.getLineNumber() > 0) {
            mensaje.append(
                    " en la línea "
            ).append(
                    error.getLineNumber()
            );
        }

        if (error.getColumnNumber() > 0) {
            mensaje.append(
                    ", columna "
            ).append(
                    error.getColumnNumber()
            );
        }

        mensaje.append(
                ": "
        ).append(
                mensajeSeguro(
                        error
                )
        );

        return mensaje.toString();
    }

    private String mensajeSeguro(
            Exception error
    ) {
        if (
                error.getMessage() == null
                        || error.getMessage().isBlank()
        ) {
            return error.getClass()
                    .getSimpleName();
        }

        return error.getMessage();
    }
}
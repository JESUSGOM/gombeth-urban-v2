package com.gombeth.urban.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptor.class);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return null;

        try {
            return EncryptorUtil.encrypt(attribute);
        } catch (Exception e) {
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;

        try {
            return EncryptorUtil.decrypt(dbData);
        } catch (Exception e) {
            log.warn("Error al desencriptar dato: {}. Se asume texto plano.", dbData);
            return dbData;
        }
    }
}
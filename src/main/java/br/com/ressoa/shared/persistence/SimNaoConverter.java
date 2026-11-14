package br.com.ressoa.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte booleanos Java para o padrão CHAR(1) 'S'/'N' usado no esquema Oracle.
 */
@Converter
public class SimNaoConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "S" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String valor) {
        return "S".equalsIgnoreCase(valor);
    }
}

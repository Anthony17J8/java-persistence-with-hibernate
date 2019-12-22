package com.ico.ltd.hibernateinaction2nd.domain.converters;

import com.ico.ltd.hibernateinaction2nd.domain.GermanZipcode;
import com.ico.ltd.hibernateinaction2nd.domain.SwissZipcode;
import com.ico.ltd.hibernateinaction2nd.domain.Zipcode;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class ZipcodeConverter implements AttributeConverter<Zipcode, String> {

    @Override
    public String convertToDatabaseColumn(Zipcode attribute) {
        return attribute.getValue();
    }

    @Override
    public Zipcode convertToEntityAttribute(String code) {
        if (code.length() == 5) {
            return new GermanZipcode(code);
        } else if (code.length() == 4) {
            return new SwissZipcode(code);
        }
        throw new IllegalArgumentException("Unsupported zipcode in database: " + code);
    }
}

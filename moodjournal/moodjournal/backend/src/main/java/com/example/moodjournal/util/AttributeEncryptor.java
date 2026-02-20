package com.example.moodjournal.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    
    
    
    
    
    

    private final EncryptionUtil encryptionUtil;

    @Autowired
    public AttributeEncryptor(EncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null)
            return null;
        try {
            return encryptionUtil.encrypt(attribute);
        } catch (Exception e) {
            
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null)
            return null;
        try {
            return encryptionUtil.decrypt(dbData);
        } catch (Exception e) {
            
            
            
            
            
            
            
            
            return dbData;
        }
    }
}

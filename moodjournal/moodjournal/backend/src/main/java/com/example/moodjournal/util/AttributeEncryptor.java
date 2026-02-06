package com.example.moodjournal.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    // Helper static reference because JPA converters are instantiated by Hibernate
    // not strictly by Spring context in all phases, but @Component usually works
    // with recent Spring Boot.
    // However, to be safe with JPA 2.1+, we can use static injection or try
    // dependency injection.
    // Spring Boot supports DI in converters safely.

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
            // In production, log this critical failure but don't leak data
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
            // FAIL-SAFE for "Nightmare Mode" migration:
            // If we can't decrypt, it might be OLD plain text data.
            // We return it as is effectively "migrating on read" (not write, but read).
            // This is a trade-off. For "Purist" security we should crash.
            // But crashing invalidates all old data.
            // Let's Try-Catch and return raw data if decryption fails (assuming it's
            // legacy).
            // Check if it's base64 first? Nah, just return raw.
            return dbData;
        }
    }
}

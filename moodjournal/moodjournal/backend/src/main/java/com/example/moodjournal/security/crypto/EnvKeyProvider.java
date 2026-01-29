package com.example.moodjournal.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EnvKeyProvider implements KeyProvider {

    private final SecretKey key;

    public EnvKeyProvider(@Value("${app.encryption.key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("Encryption key not found in environment variables!");
        }
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.key = new SecretKeySpec(decodedKey, "AES");
    }

    @Override
    public SecretKey getKey() {
        return key;
    }
}

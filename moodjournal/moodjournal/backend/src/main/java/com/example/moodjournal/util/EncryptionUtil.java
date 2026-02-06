package com.example.moodjournal.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // Must be one of {128, 120, 112, 104, 96}
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;

    private final SecretKey secretKey;

    public EncryptionUtil(@Value("${app.encryption.key}") String secretKeyString) {
        // "Nightmare Mode": If key is missing or weak, we should probably explode.
        // For now, we assume it's provided via application.properties or ENV.
        // Key must be 32 bytes (256 bits) for AES-256.
        if (secretKeyString == null || secretKeyString.length() < 32) {
            throw new IllegalArgumentException("Encryption key must be at least 32 characters long for AES-256.");
        }
        // Use the first 32 chars (bytes) if string is longer, or raw bytes if strictly
        // encoded.
        // Simple approach: Use string bytes (UTF-8) of first 32 chars.
        byte[] keyBytes = new byte[32];
        System.arraycopy(secretKeyString.getBytes(StandardCharsets.UTF_8), 0, keyBytes, 0, 32);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null)
            return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prefix IV to cipherText (IV is needed for decryption, it's not secret)
            byte[] ivAndCipherText = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipherText, 0, iv.length);
            System.arraycopy(cipherText, 0, ivAndCipherText, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(ivAndCipherText);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during encryption: " + e.getMessage(), e);
        }
    }

    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null)
            return null;
        try {
            byte[] ivAndCipherText = Base64.getDecoder().decode(cipherTextBase64);

            // Fail-safe: If data is not encrypted (legacy plain text), GCM will likely
            // fail.
            // In a real migration we'd check a prefix or version.
            // Here we let it fail and catch exception if needed, but for "Secure" app
            // we assume all data IS encrypted or we want to know if it's corrupt.

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[ivAndCipherText.length - IV_LENGTH_BYTE];

            System.arraycopy(ivAndCipherText, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(ivAndCipherText, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // "Fail-safe" for legacy data: return original string if decryption fails?
            // SECURITY RISK: If bad actor puts garbage, we return garbage.
            // BUT: If previous data was PLAIN TEXT, this will fail.
            // HACK: Try to return original if it looks like plain text?
            // NO. Hardest Security = Fail if integrity check fails.
            // However, to avoid breaking the app for the user right now (who has existing
            // data),
            // we might handle this gracefully or assume database is empty?
            // User said "Start fresh" implies we might be ok, or we handle migration.
            // Let's rely on standard exception flow.
            throw new RuntimeException(
                    "Error occurred during decryption (Data tampering or invalid key): " + e.getMessage(), e);
        }
    }
}

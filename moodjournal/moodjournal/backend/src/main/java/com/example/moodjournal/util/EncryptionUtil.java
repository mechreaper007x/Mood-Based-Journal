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
    private static final int TAG_LENGTH_BIT = 128; 
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;

    private final SecretKey secretKey;

    public EncryptionUtil(@Value("${app.encryption.key}") String secretKeyString) {
        
        
        
        if (secretKeyString == null || secretKeyString.length() < 32) {
            throw new IllegalArgumentException("Encryption key must be at least 32 characters long for AES-256.");
        }
        
        
        
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

            
            
            
            
            

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[ivAndCipherText.length - IV_LENGTH_BYTE];

            System.arraycopy(ivAndCipherText, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(ivAndCipherText, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            
            
            
            
            
            
            
            
            
            
            throw new RuntimeException(
                    "Error occurred during decryption (Data tampering or invalid key): " + e.getMessage(), e);
        }
    }
}

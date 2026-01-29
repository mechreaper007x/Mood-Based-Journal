package com.example.moodjournal.security.crypto;

import javax.crypto.SecretKey;

/**
 * Interface for retrieving encryption keys.
 * Abstraction layer to allow swapping between Env Vars and HashiCorp Vault.
 */
public interface KeyProvider {
    /**
     * Retrieves the current active encryption key.
     * 
     * @return AES-256 SecretKey
     */
    SecretKey getKey();
}

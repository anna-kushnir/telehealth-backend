package com.telehealth.auth.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Converter
public class StringCryptoConverter implements AttributeConverter<String, String> {

    private static final String AES = "AES";

    private String getSecretKey() {
        String key = System.getenv("DB_ENCRYPTION_KEY");
        if (key == null || key.length() < 16) {
            throw new IllegalStateException("Error: The environment variable DB_ENCRYPTION_KEY is not set correctly");
        }
        return key;
    }

    private Key getKey() {
        return new SecretKeySpec(getSecretKey().getBytes(), AES);
    }

    @Override
    public String convertToDatabaseColumn(String attr) {
        if (attr == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encryptedBytes = cipher.doFinal(attr.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }
}

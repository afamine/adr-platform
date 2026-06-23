package com.adrplatform.auth.service;

import com.adrplatform.auth.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SecretEncryptionService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return PREFIX
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                    + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt secret", ex);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (!storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        try {
            String[] parts = storedValue.substring(PREFIX.length()).split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted secret format");
            }

            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getUrlDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt secret", ex);
        }
    }

    private SecretKeySpec keySpec() throws Exception {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters for secret encryption");
        }
        byte[] key = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}

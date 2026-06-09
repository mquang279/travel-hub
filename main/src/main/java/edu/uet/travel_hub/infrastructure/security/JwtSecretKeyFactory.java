package edu.uet.travel_hub.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class JwtSecretKeyFactory {
    private JwtSecretKeyFactory() {
    }

    public static SecretKey create(String secret) {
        byte[] raw = (secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = raw.length >= 32 ? raw : sha256(raw);
        return new SecretKeySpec(keyBytes, JwtTokenProvider.JWT_ALGORITHM.getName());
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT signing key", exception);
        }
    }
}

package com.bank.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes passwords with PBKDF2-HMAC-SHA256: salted and deliberately slow. Built into the JDK.
 * Stored format: pbkdf2-sha256$iterations$salt$hash (salt and hash in Base64).
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ID = "pbkdf2-sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    public String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);

        Base64.Encoder base64 = Base64.getEncoder();
        return String.join("$", FORMAT_ID, String.valueOf(ITERATIONS),
                base64.encodeToString(salt), base64.encodeToString(hash));
    }

    public boolean matches(char[] password, String stored) {
        if (password == null || password.length == 0) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals(FORMAT_ID)) {
            throw new IllegalArgumentException("Unrecognised password hash format");
        }
        int iterations = Integer.parseInt(parts[1]);
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] salt = base64.decode(parts[2]);
        byte[] expected = base64.decode(parts[3]);

        byte[] actual = derive(password, salt, iterations);
        return MessageDigest.isEqual(expected, actual);   // constant-time comparison
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }
}
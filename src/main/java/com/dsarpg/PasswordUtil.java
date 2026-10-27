
package com.dsarpg;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(char[] password)
            throws GeneralSecurityException {

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] result = derive(password, salt, ITERATIONS);

        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(result);
    }

    public static boolean verify(char[] password, String storedHash)
            throws GeneralSecurityException {

        if (storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split("\\$");

        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);

        // Reject unexpected or excessively expensive parameters.
        if (iterations < 100_000 || iterations > 1_000_000) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);

        if (salt.length < 16 || expected.length != KEY_LENGTH / 8) {
            return false;
        }

        byte[] actual = derive(password, salt, iterations);

        if (actual.length != expected.length) {
            return false;
        }

        int difference = 0;
        for (int i = 0; i < actual.length; i++) {
            difference |= actual[i] ^ expected[i];
        }

        return difference == 0;
    }

    private static byte[] derive(char[] password, byte[] salt,
                                 int iterations)
            throws GeneralSecurityException {

        PBEKeySpec spec =
                new PBEKeySpec(password, salt, iterations, KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}

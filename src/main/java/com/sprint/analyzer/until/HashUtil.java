package com.sprint.analyzer.until;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    normalize(text).getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash", e);
        }
    }

    private static String normalize(String text) {
        return text == null ? "" :
                text.replaceAll("\\s+", " ")
                        .trim()
                        .toLowerCase();
    }
}

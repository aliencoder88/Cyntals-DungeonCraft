package com.dungeoncraft.config;

/**
 * Shared validation for player-assigned verified-signal names/codes.
 *
 * Empty keys are allowed while the verified network is still reserved, but a
 * future verified transmitter or receiver will treat an empty key as
 * unconfigured rather than as a wildcard.
 */
public final class VerifiedSignalKey {
    public static final int MAX_LENGTH = 64;

    private VerifiedSignalKey() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder();

        for (int index = 0;
                index < value.length() && sanitized.length() < MAX_LENGTH;
                index++) {
            char character = value.charAt(index);

            if (!Character.isISOControl(character)) {
                sanitized.append(character);
            }
        }

        return sanitized.toString().trim();
    }
}

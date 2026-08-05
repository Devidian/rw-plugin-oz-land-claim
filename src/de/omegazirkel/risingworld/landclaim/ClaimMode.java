package de.omegazirkel.risingworld.landclaim;

import java.util.Locale;

public enum ClaimMode {
    TIME_BASED,
    ADMINISTRATIVE,
    LAND_PRICING,
    CITY;

    public static ClaimMode parse(String value) {
        if (value == null || value.isBlank()) {
            return TIME_BASED;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "TIME", "TIMEBASED", "ZEITBASIERT" -> TIME_BASED;
            case "ADMIN", "ADMINISTRATIV" -> ADMINISTRATIVE;
            case "LAND_PRICE", "LANDPRICE", "GRUNDSTUECKSPREISE", "GRUNDSTÜCKSPREISE" -> LAND_PRICING;
            case "STADT", "STADTMODUS" -> CITY;
            default -> {
                try {
                    yield ClaimMode.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield TIME_BASED;
                }
            }
        };
    }
}

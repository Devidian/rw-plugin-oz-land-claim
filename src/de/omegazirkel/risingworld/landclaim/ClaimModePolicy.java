package de.omegazirkel.risingworld.landclaim;

/** Central mode gates. Persisted areas are deliberately not rewritten on mode changes. */
public final class ClaimModePolicy {
    private ClaimModePolicy() {
    }

    public static ClaimMode current() {
        return PluginSettings.getInstance().claimMode;
    }

    public static boolean usesClaimTime() {
        return current() == ClaimMode.TIME_BASED;
    }

    public static boolean requiresWallet() {
        return current() == ClaimMode.LAND_PRICING || current() == ClaimMode.CITY;
    }

    public static boolean acquisitionAvailable(boolean walletAvailable) {
        return !requiresWallet() || walletAvailable;
    }

    public static boolean mayCreatePlayerClaim(boolean admin, boolean walletAvailable) {
        return switch (current()) {
            case TIME_BASED -> true;
            case ADMINISTRATIVE -> admin;
            case LAND_PRICING -> acquisitionAvailable(walletAvailable);
            case CITY -> admin && acquisitionAvailable(walletAvailable);
        };
    }

    public static boolean adminBypassesClaimLimit(boolean admin) {
        return admin && current() == ClaimMode.ADMINISTRATIVE;
    }

    public static boolean mayPlayerResizeOrRelease(boolean admin) {
        return admin || current() != ClaimMode.ADMINISTRATIVE;
    }

    public static boolean salesAvailable(boolean configured) {
        return switch (current()) {
            case TIME_BASED, LAND_PRICING -> configured;
            case ADMINISTRATIVE, CITY -> true;
        };
    }

    public static boolean salesAvailable(boolean configured, boolean walletAvailable) {
        return salesAvailable(configured) && (!requiresWallet() || walletAvailable);
    }

    public static boolean allowsFreeAdministrativeTakeover(boolean walletAvailable) {
        return current() == ClaimMode.ADMINISTRATIVE && !walletAvailable;
    }
}

package de.omegazirkel.risingworld.landclaim.web;

import java.sql.SQLException;
import java.util.function.BooleanSupplier;
import com.google.gson.Gson;
import de.omegazirkel.risingworld.OZToolsNativeWebAccess;
import de.omegazirkel.risingworld.landclaim.exports.ClaimSaleExportService;
import de.omegazirkel.risingworld.landclaim.exports.RenewZoneExportService;
import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Native read-only claim-sale and renew-zone exports. */
public final class LandClaimExportRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();

    private final BooleanSupplier enabled;
    private final ClaimSaleExportService sales;
    private final RenewZoneExportService renews;
    private final String world;

    public LandClaimExportRoute(BooleanSupplier enabled, ClaimSaleExportService sales, RenewZoneExportService renews,
            String world) {
        this.enabled = enabled;
        this.sales = sales;
        this.renews = renews;
        this.world = world;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setContentType("application/json; charset=utf-8");
        event.setResponseHeader("Cache-Control", "no-store");
        if (!enabled.getAsBoolean()) {
            event.setResponseCode(404);
            event.setResponseBody("{\"error\":\"not_found\"}");
            return;
        }
        if (!OZToolsNativeWebAccess.authorize(event)) return;
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            event.setResponseBody("{\"error\":\"method_not_allowed\"}");
            return;
        }
        try {
            Long lastChange = cursor(event.getQueryParameters().get("lastChange"));
            Object payload = sales != null
                    ? sales.exportActiveListings(world, lastChange)
                    : renews.exportRenewZones(world, lastChange);
            event.setResponseCode(200);
            event.setResponseBody(GSON.toJson(payload));
        } catch (IllegalArgumentException e) {
            event.setResponseCode(400);
            event.setResponseBody("{\"error\":\"invalid_last_change\"}");
        } catch (SQLException | RuntimeException e) {
            event.setResponseCode(503);
            event.setResponseBody("{\"error\":\"land_claim_unavailable\"}");
        }
    }

    static Long cursor(String value) {
        if (value == null) return null;
        if (!value.matches("\\d+")) throw new IllegalArgumentException();
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

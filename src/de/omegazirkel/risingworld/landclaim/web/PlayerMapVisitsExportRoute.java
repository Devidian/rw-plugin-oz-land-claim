package de.omegazirkel.risingworld.landclaim.web;

import java.sql.SQLException;
import java.util.function.BooleanSupplier;

import com.google.gson.Gson;

import de.omegazirkel.risingworld.OZToolsNativeWebAccess;
import de.omegazirkel.risingworld.landclaim.exports.PlayerMapVisitExportService;
import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Authenticated, paginated player visit-map export for the Manager backend. */
public final class PlayerMapVisitsExportRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();
    private final BooleanSupplier enabled;
    private final PlayerMapVisitExportService exports;
    private final String world;

    public PlayerMapVisitsExportRoute(BooleanSupplier enabled, PlayerMapVisitExportService exports, String world) {
        this.enabled = enabled;
        this.exports = exports;
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
            String uid = event.getQueryParameters().get("uid");
            int page = integer(event.getQueryParameters().get("page"), 0);
            int pageSize = integer(event.getQueryParameters().get("pageSize"), 50);
            event.setResponseCode(200);
            event.setResponseBody(GSON.toJson(exports.export(uid, world, page, pageSize)));
        } catch (IllegalArgumentException e) {
            event.setResponseCode(400);
            event.setResponseBody("{\"error\":\"invalid_player_map_request\"}");
        } catch (SQLException | RuntimeException e) {
            event.setResponseCode(503);
            event.setResponseBody("{\"error\":\"land_claim_unavailable\"}");
        }
    }

    private static int integer(String value, int defaultValue) {
        if (value == null) return defaultValue;
        if (!value.matches("\\d+")) throw new IllegalArgumentException();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

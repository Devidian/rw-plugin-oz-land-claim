package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import de.omegazirkel.risingworld.tools.db.interfaces.DatabaseSchema;

public final class LandClaimChunkSchema implements DatabaseSchema {

    private final String tableName;

    public LandClaimChunkSchema(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void init(Connection con) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + " player_uuid TEXT NOT NULL,"
                + " player_dbid INTEGER NOT NULL,"
                + " world TEXT NOT NULL,"
                + " chunk_x INTEGER NOT NULL,"
                + " chunk_y INTEGER NOT NULL,"
                + " chunk_z INTEGER NOT NULL,"
                + " total_time_ms BIGINT NOT NULL DEFAULT 0,"
                + " last_seen BIGINT NOT NULL DEFAULT 0,"
                + " claimed_at BIGINT NOT NULL DEFAULT 0,"
                + " area_id BIGINT NOT NULL DEFAULT 0,"
                + " price INTEGER NOT NULL DEFAULT 0,"
                + " PRIMARY KEY (player_uuid, world, chunk_x, chunk_y, chunk_z)"
                + ");";

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }
}


package de.omegazirkel.risingworld.landclaim.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.omegazirkel.risingworld.landclaim.db.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.tools.db.interfaces.SQLiteEntityMapper;
import net.risingworld.api.utils.Vector3i;

public class LandClaimChunkMapper
        implements SQLiteEntityMapper<LandClaimChunkKey, LandClaimChunkInfo> {

    private final String table;

    public LandClaimChunkMapper(String table) {
        this.table = table;
    }

    @Override
    public String tableName() {
        return table;
    }

    @Override
    public LandClaimChunkKey keyOf(LandClaimChunkInfo e) {
        return new LandClaimChunkKey(
                e.playerUID,
                e.world,
                e.chunkPos
        );
    }

    @Override
    public LandClaimChunkInfo fromResultSet(ResultSet rs)
            throws SQLException {

        return new LandClaimChunkInfo(
                new Vector3i(
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_y"),
                        rs.getInt("chunk_z")
                ),
                rs.getLong("total_time_ms"),
                rs.getLong("last_seen"),
                rs.getLong("claimed_at"),
                rs.getString("player_uuid"),
                rs.getString("world"),
                rs.getInt("price"),
                rs.getLong("area_id"),
                rs.getInt("player_dbid")
        );
    }

    /* ---------- SQL Templates ---------- */

    @Override
    public String insertSql() {
        return """
            INSERT INTO %s (
                player_uuid, player_dbid, world,
                chunk_x, chunk_y, chunk_z,
                total_time_ms, last_seen,
                claimed_at, area_id, price
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(table);
    }

    @Override
    public String updateSql() {
        return """
            UPDATE %s SET
                player_dbid = ?,
                total_time_ms = ?,
                last_seen = ?,
                claimed_at = ?,
                area_id = ?,
                price = ?
            WHERE player_uuid = ?
              AND world = ?
              AND chunk_x = ?
              AND chunk_y = ?
              AND chunk_z = ?
            """.formatted(table);
    }

    @Override
    public String deleteSql() {
        return """
            DELETE FROM %s
            WHERE player_uuid = ?
              AND world = ?
              AND chunk_x = ?
              AND chunk_y = ?
              AND chunk_z = ?
            """.formatted(table);
    }

    /* ---------- Bindings ---------- */

    @Override
    public void bindInsert(
            PreparedStatement ps,
            LandClaimChunkInfo e
    ) throws SQLException {

        Vector3i c = e.chunkPos;

        ps.setString(1, e.playerUID);
        ps.setInt(2, e.playerDBID);
        ps.setString(3, e.world);
        ps.setInt(4, c.x);
        ps.setInt(5, c.y);
        ps.setInt(6, c.z);
        ps.setLong(7, e.totalTimeMs);
        ps.setLong(8, e.lastSeenMs);
        ps.setLong(9, e.claimedAtMs);
        ps.setLong(10, e.areaID);
        ps.setInt(11, e.price);
    }

    @Override
    public void bindUpdate(
            PreparedStatement ps,
            LandClaimChunkInfo e
    ) throws SQLException {

        Vector3i c = e.chunkPos;

        ps.setInt(1, e.playerDBID);
        ps.setLong(2, e.totalTimeMs);
        ps.setLong(3, e.lastSeenMs);
        ps.setLong(4, e.claimedAtMs);
        ps.setLong(5, e.areaID);
        ps.setInt(6, e.price);

        ps.setString(7, e.playerUID);
        ps.setString(8, e.world);
        ps.setInt(9, c.x);
        ps.setInt(10, c.y);
        ps.setInt(11, c.z);
    }

    @Override
    public void bindDelete(
            PreparedStatement ps,
            LandClaimChunkInfo e
    ) throws SQLException {

        Vector3i c = e.chunkPos;

        ps.setString(1, e.playerUID);
        ps.setString(2, e.world);
        ps.setInt(3, c.x);
        ps.setInt(4, c.y);
        ps.setInt(5, c.z);
    }

    @Override
    public String selectAllSql() {
        return "SELECT * FROM " + table + ";";
    }
}

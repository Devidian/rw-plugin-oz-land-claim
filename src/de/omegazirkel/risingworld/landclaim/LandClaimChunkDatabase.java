package de.omegazirkel.risingworld.landclaim;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.interfaces.ChunkDatabase;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.db.SQLite;
import net.risingworld.api.World;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

public class LandClaimChunkDatabase implements ChunkDatabase {

    private final SQLite db;
    static private final String tableName = "chunkData";

    public static OZLogger logger() {
        return LandClaim.logger();
    }

    public LandClaimChunkDatabase(SQLite database) {
        this.db = database;
        initialize();
    }

    private void initialize() {
        // create table
        db.execute(
                "CREATE TABLE IF NOT EXISTS " + tableName + " ("
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
                        + ");");
    }

    // --- Query Helpers -----------------------------------------------------

    private String q(String value) {
        return "'" + escape(value) + "'";
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    // --- List Queries ------------------------------------------------------

    public List<LandClaimChunkInfo> getChunkInfoListByPlayer(String playerId) {
        return executeListQuery(
                "SELECT * FROM " + tableName + " WHERE player_uuid=" + q(playerId) + ";");
    }

    @Override
    public List<LandClaimChunkInfo> getChunkInfoListByChunk(Vector3i chunk) {
        return executeListQuery(
                "SELECT * FROM " + tableName
                        + " WHERE chunk_x=" + chunk.x
                        + " AND chunk_y=" + chunk.y
                        + " AND chunk_z=" + chunk.z + ";");
    }

    public int getTotalClaimWeight(String playerId) {
        try (ResultSet rs = db.executeQuery(
                "SELECT COUNT(*) AS c FROM " + tableName
                        + " WHERE player_uuid=" + q(playerId)
                        + " AND claimed_at > 0;")) {

            if (rs.next()) {
                return rs.getInt("c");
            }
        } catch (Exception e) {
            logger().error("getTotalClaimWeight failed: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public long getTotalChunkTime(String playerId, Vector3i chunk) {
        LandClaimChunkInfo info = getChunkInfoForPlayer(playerId, chunk);
        return info == null ? 0 : info.totalTimeMs;
    }

    @Override
    public LandClaimChunkInfo getChunkInfoForPlayer(String playerId, Vector3i chunk) {
        try (ResultSet result = db.executeQuery(
                "SELECT * FROM " + tableName
                        + " WHERE player_uuid=" + q(playerId)
                        + " AND chunk_x=" + chunk.x
                        + " AND chunk_y=" + chunk.y
                        + " AND chunk_z=" + chunk.z + ";")) {

            if (result.next()) {
                return new LandClaimChunkInfo(
                        new Vector3i(
                                result.getInt("chunk_x"),
                                result.getInt("chunk_y"),
                                result.getInt("chunk_z")),
                        result.getLong("total_time_ms"),
                        result.getLong("last_seen"),
                        result.getLong("claimed_at"),
                        result.getString("player_uuid"),
                        result.getString("world"),
                        result.getInt("price"),
                        result.getLong("area_id"),
                        result.getInt("player_dbid"));
            }
        } catch (Exception e) {
            logger().error("getChunkInfoForPlayer failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private List<LandClaimChunkInfo> executeListQuery(String query) {
        List<LandClaimChunkInfo> infos = new ArrayList<>();

        try (ResultSet result = db.executeQuery(query)) {
            while (result.next()) {
                Vector3i chunk = new Vector3i(
                        result.getInt("chunk_x"),
                        result.getInt("chunk_y"),
                        result.getInt("chunk_z"));

                infos.add(new LandClaimChunkInfo(
                        chunk,
                        result.getLong("total_time_ms"),
                        result.getLong("last_seen"),
                        result.getLong("claimed_at"),
                        result.getString("player_uuid"),
                        result.getString("world"),
                        result.getInt("price"),
                        result.getLong("area_id"),
                        result.getInt("player_dbid")));
            }
        } catch (Exception e) {
            logger().error("executeListQuery failed: " + e.getMessage());
            e.printStackTrace();
        }

        return infos;
    }

    // --- Overloads ---------------------------------------------------------

    @Override
    public List<LandClaimChunkInfo> getChunkInfoListByPlayer(Player player) {
        return getChunkInfoListByPlayer(player.getUID());
    }

    @Override
    public LandClaimChunkInfo getChunkInfoForPlayer(Player player, Vector3i chunk) {
        return getChunkInfoForPlayer(player.getUID(), chunk);
    }

    @Override
    public int getTotalClaimWeight(Player player) {
        return getTotalClaimWeight(player.getUID());
    }

    @Override
    public long getTotalChunkTime(Player player, Vector3i chunk) {
        return getTotalChunkTime(player.getUID(), chunk);
    }

    // --- UPSERT Logic ------------------------------------------------------

    @Override
    public void saveChunkTime(Player player, Vector3i chunk, long milliseconds) {
        String playerId = player.getUID();
        Integer playerDbId = player.getDbID();
        String world = World.getName();
        long now = System.currentTimeMillis();

        String sql = "INSERT INTO " + tableName + " ("
                + "player_uuid,player_dbid, world, chunk_x, chunk_y, chunk_z,"
                + "total_time_ms, last_seen"
                + ") VALUES ("
                + q(playerId) + ", "
                + playerDbId + ", "
                + q(world) + ", "
                + chunk.x + ", "
                + chunk.y + ", "
                + chunk.z + ", "
                + milliseconds + ", "
                + now
                + ") "
                + "ON CONFLICT(player_uuid, world, chunk_x, chunk_y, chunk_z) DO UPDATE SET "
                + "total_time_ms = total_time_ms + excluded.total_time_ms, "
                + "player_dbid=excluded.player_dbid, "
                + "last_seen=excluded.last_seen;";

        try {
            db.executeUpdate(sql);
        } catch (Exception e) {
            logger().error("saveChunkTime failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveChunkClaim(String playerId, Integer playerDBID, Vector3i chunk, long claimedAt, long areaID) {
        String world = World.getName();
        String sql = "INSERT INTO " + tableName + " ("
                + "player_uuid,player_dbid, world, chunk_x, chunk_y, chunk_z, claimed_at, area_id"
                + ") VALUES ("
                + q(playerId) + ", "
                + playerDBID + ", "
                + q(world) + ", "
                + chunk.x + ", "
                + chunk.y + ", "
                + chunk.z + ", "
                + claimedAt + ", "
                + areaID
                + ") "
                + "ON CONFLICT(player_uuid, world, chunk_x, chunk_y, chunk_z) DO UPDATE SET "
                + "area_id=excluded.area_id,"
                + "player_dbid=excluded.player_dbid,"
                + "claimed_at=excluded.claimed_at;";

        try {
            db.executeUpdate(sql);
        } catch (Exception e) {
            logger().error("saveChunkClaim failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void saveChunkClaim(Player player, Vector3i chunk, long claimedAt, long areaID) {
        String playerId = player.getUID();
        saveChunkClaim(playerId, player.getDbID(), chunk, claimedAt, areaID);
    }

    @Override
    public void removeChunkClaim(String playerId, Integer playerDBID, Vector3i chunk) {
        saveChunkClaim(playerId, playerDBID, chunk, 0, 0);
    }

    @Override
    public void removeChunkClaim(Player player, Vector3i chunk) {
        saveChunkClaim(player.getUID(), player.getDbID(), chunk, 0, 0);
    }

    @Override
    public List<LandClaimChunkInfo> getChunkInfoListClaimed() {
        return executeListQuery(
                "SELECT * FROM " + tableName
                        + " WHERE claimed_at > 0"
                        + " ORDER BY claimed_at DESC;");
    }

    @Override
    public List<LandClaimChunkInfo> getChunkInfoListByArea(long areaId) {
        return executeListQuery(
                "SELECT * FROM " + tableName
                        + " WHERE area_id=" + areaId);
    }

}

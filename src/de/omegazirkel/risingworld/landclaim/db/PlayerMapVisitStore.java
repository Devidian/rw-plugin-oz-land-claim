package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/** Compact, per-sector record of chunks a player has visited. */
public final class PlayerMapVisitStore {
    public static final int SECTOR_SIZE_CHUNKS = 256;
    public static final int BITMAP_BYTES = SECTOR_SIZE_CHUNKS * SECTOR_SIZE_CHUNKS / Byte.SIZE;

    private final Connection connection;

    public PlayerMapVisitStore(Connection connection) throws SQLException {
        this.connection = connection;
        init();
    }

    public synchronized void recordVisit(String playerUuid, int playerDbId, String world, int chunkX, int chunkZ)
            throws SQLException {
        if (playerUuid == null || playerUuid.isBlank() || playerDbId <= 0) return;
        int sectorX = Math.floorDiv(chunkX, SECTOR_SIZE_CHUNKS);
        int sectorZ = Math.floorDiv(chunkZ, SECTOR_SIZE_CHUNKS);
        int localX = Math.floorMod(chunkX, SECTOR_SIZE_CHUNKS);
        int localZ = Math.floorMod(chunkZ, SECTOR_SIZE_CHUNKS);
        int bit = localZ * SECTOR_SIZE_CHUNKS + localX;

        byte[] bitmap = readBitmap(playerUuid, world, sectorX, sectorZ);
        if (bitmap == null) bitmap = new byte[BITMAP_BYTES];
        if ((bitmap[bit >>> 3] & (1 << (bit & 7))) != 0) return;
        bitmap[bit >>> 3] |= (byte) (1 << (bit & 7));

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO landClaimPlayerMapVisits(player_uuid, player_dbid, world, sector_x, sector_z, bitmap, updated_at_ms)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, world, sector_x, sector_z) DO UPDATE SET
                    player_dbid = excluded.player_dbid,
                    bitmap = excluded.bitmap,
                    updated_at_ms = excluded.updated_at_ms
                """)) {
            statement.setString(1, playerUuid);
            statement.setInt(2, playerDbId);
            statement.setString(3, world);
            statement.setInt(4, sectorX);
            statement.setInt(5, sectorZ);
            statement.setBytes(6, bitmap);
            statement.setLong(7, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private byte[] readBitmap(String playerUuid, String world, int sectorX, int sectorZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT bitmap FROM landClaimPlayerMapVisits
                WHERE player_uuid = ? AND world = ? AND sector_x = ? AND sector_z = ?
                """)) {
            statement.setString(1, playerUuid);
            statement.setString(2, world);
            statement.setInt(3, sectorX);
            statement.setInt(4, sectorZ);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                byte[] bitmap = result.getBytes("bitmap");
                return bitmap != null && bitmap.length == BITMAP_BYTES
                        ? bitmap
                        : Arrays.copyOf(bitmap == null ? new byte[0] : bitmap, BITMAP_BYTES);
            }
        }
    }

    private void init() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS landClaimPlayerMapVisits (
                        player_uuid TEXT NOT NULL,
                        player_dbid INTEGER NOT NULL,
                        world TEXT NOT NULL,
                        sector_x INTEGER NOT NULL,
                        sector_z INTEGER NOT NULL,
                        bitmap BLOB NOT NULL,
                        updated_at_ms BIGINT NOT NULL,
                        PRIMARY KEY (player_uuid, world, sector_x, sector_z)
                    )
                    """);
        }
    }
}

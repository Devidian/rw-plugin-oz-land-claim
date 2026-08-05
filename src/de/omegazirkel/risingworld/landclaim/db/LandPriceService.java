package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import net.risingworld.api.Server;
import net.risingworld.api.World;
import net.risingworld.api.objects.Area;
import net.risingworld.api.utils.Vector3i;

/** Persists only non-zero geometric price increments for currently free chunks. */
public final class LandPriceService {
    public static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final String TABLE = "landPriceIncrements";

    private final Connection connection;
    private final String world = World.getName();

    public LandPriceService(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "world TEXT NOT NULL, chunk_x INTEGER NOT NULL, chunk_y INTEGER NOT NULL,"
                    + "chunk_z INTEGER NOT NULL, increment REAL NOT NULL, updated_at BIGINT NOT NULL,"
                    + "PRIMARY KEY(world, chunk_x, chunk_y, chunk_z));");
        }
    }

    public synchronized void refresh() {
        double incrementPerChunk = Math.max(0d, PluginSettings.getInstance().landPriceClusterIncrement);
        Set<Vector3i> occupied = occupiedChunks(Server.getAllAreas());
        Set<Vector3i> boundary = new HashSet<>();
        for (Vector3i chunk : occupied) {
            for (Vector3i neighbor : neighbors(chunk)) {
                if (!occupied.contains(neighbor)) boundary.add(neighbor);
            }
        }
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + TABLE + " WHERE world=?")) {
                delete.setString(1, world);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + TABLE
                    + "(world,chunk_x,chunk_y,chunk_z,increment,updated_at) VALUES(?,?,?,?,?,?)")) {
                long now = System.currentTimeMillis();
                for (Vector3i chunk : boundary) {
                    long influence = adjacentClusterSizeSum(chunk, occupied);
                    if (influence <= 0) continue;
                    insert.setString(1, world);
                    insert.setInt(2, chunk.x);
                    insert.setInt(3, chunk.y);
                    insert.setInt(4, chunk.z);
                    insert.setDouble(5, influence * incrementPerChunk);
                    insert.setLong(6, now);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ex) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            LandClaim.logger().error("Could not refresh land price increments: " + ex.getMessage());
        }
    }

    public long price(Vector3i chunk, long basePrice) {
        double surcharge = storedSurcharge(chunk);
        return calculatePrice(basePrice, 1d, surcharge);
    }

    public long areaValue(Area area, long basePrice, double incrementPerChunk) {
        if (area == null) return Math.max(0L, basePrice);
        Set<Vector3i> occupied = occupiedChunks(Server.getAllAreas());
        long total = 0L;
        for (Vector3i chunk : ChunkClaimUtil.areaToChunks(area)) {
            Set<Vector3i> withoutCurrent = new HashSet<>(occupied);
            withoutCurrent.remove(chunk);
            long value = calculatePrice(basePrice, incrementPerChunk,
                    adjacentClusterSizeSum(chunk, withoutCurrent));
            if (total >= MAX_SAFE_INTEGER - value) return MAX_SAFE_INTEGER;
            total += value;
        }
        return total;
    }

    public double storedSurcharge(Vector3i chunk) {
        String sql = "SELECT increment FROM " + TABLE
                + " WHERE world=? AND chunk_x=? AND chunk_y=? AND chunk_z=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, chunk.x);
            statement.setInt(3, chunk.y);
            statement.setInt(4, chunk.z);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Math.max(0d, result.getDouble(1)) : 0d;
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read land price: " + ex.getMessage());
            return 0d;
        }
    }

    public static long calculatePrice(long basePrice, double incrementPerChunk, double adjacentClusterSizeSum) {
        if (basePrice <= 0) return 0L;
        double value = basePrice * (1d + Math.max(0d, incrementPerChunk) * Math.max(0d, adjacentClusterSizeSum));
        if (!Double.isFinite(value) || value >= MAX_SAFE_INTEGER) return MAX_SAFE_INTEGER;
        return Math.min(MAX_SAFE_INTEGER, (long) Math.ceil(value));
    }

    public static long adjacentClusterSizeSum(Vector3i freeChunk, Set<Vector3i> occupied) {
        if (freeChunk == null || occupied == null || occupied.contains(freeChunk)) return 0L;
        Set<Vector3i> visited = new HashSet<>();
        long total = 0L;
        for (Vector3i neighbor : neighbors(freeChunk)) {
            if (!occupied.contains(neighbor) || visited.contains(neighbor)) continue;
            Set<Vector3i> component = component(neighbor, occupied, visited);
            total = Math.addExact(total, component.size());
        }
        return total;
    }

    private static Set<Vector3i> component(Vector3i start, Set<Vector3i> occupied, Set<Vector3i> visited) {
        Set<Vector3i> result = new HashSet<>();
        ArrayDeque<Vector3i> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Vector3i current = queue.removeFirst();
            result.add(current);
            for (Vector3i neighbor : neighbors(current)) {
                if (occupied.contains(neighbor) && visited.add(neighbor)) queue.addLast(neighbor);
            }
        }
        return result;
    }

    public static Set<Vector3i> occupiedChunks(Area[] areas) {
        Set<Vector3i> result = new HashSet<>();
        if (areas == null) return result;
        for (Area area : areas) {
            if (area != null) result.addAll(ChunkClaimUtil.areaToChunks(area));
        }
        return result;
    }

    public static List<Vector3i> neighbors(Vector3i chunk) {
        List<Vector3i> result = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0)
                        result.add(new Vector3i(chunk.x + x, chunk.y + y, chunk.z + z));
                }
            }
        }
        return result;
    }
}

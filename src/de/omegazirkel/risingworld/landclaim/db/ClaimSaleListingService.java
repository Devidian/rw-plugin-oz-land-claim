package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.World;
import net.risingworld.api.objects.Player;

public class ClaimSaleListingService {
    private static final String TABLE = "claimSaleListings";

    private final Connection connection;
    private final String world = World.getName();

    public ClaimSaleListingService(Connection connection) throws SQLException {
        this.connection = connection;
        init();
    }

    public Optional<ClaimSaleListing> activeListing(long areaId) {
        String sql = "SELECT * FROM " + TABLE + " WHERE world = ? AND area_id = ? AND status = ? ORDER BY id DESC LIMIT 1;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            statement.setString(3, ClaimSaleStatus.ACTIVE.name());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read active claim sale listing: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public List<ClaimSaleListing> listActiveListings() {
        String sql = "SELECT * FROM " + TABLE + " WHERE world = ? AND status = ? ORDER BY listed_at DESC;";
        List<ClaimSaleListing> listings = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setString(2, ClaimSaleStatus.ACTIVE.name());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    listings.add(read(result));
                }
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not list active claim sale listings: " + ex.getMessage());
        }
        return listings;
    }

    public ClaimSaleListing listForSale(Player owner, long areaId, long price) {
        if (owner == null || areaId <= 0 || price <= 0) {
            return null;
        }
        withdrawActiveListing(areaId);
        String sql = """
                INSERT INTO claimSaleListings(
                    world, area_id, owner_uuid, owner_dbid, price,
                    listed_at, buyer_uuid, buyer_dbid, purchased_at, status
                ) VALUES (?, ?, ?, ?, ?, ?, '', 0, 0, ?);
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            statement.setString(3, owner.getUID());
            statement.setInt(4, owner.getDbID());
            statement.setLong(5, price);
            statement.setLong(6, System.currentTimeMillis());
            statement.setString(7, ClaimSaleStatus.ACTIVE.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : 0L;
                return findById(id).orElse(null);
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not create claim sale listing: " + ex.getMessage());
            return null;
        }
    }

    public boolean withdrawActiveListing(long areaId) {
        return updateActiveListingStatus(areaId, ClaimSaleStatus.WITHDRAWN, "", 0, 0);
    }

    public boolean markPurchased(long areaId, Player buyer) {
        if (buyer == null) {
            return false;
        }
        return updateActiveListingStatus(areaId, ClaimSaleStatus.SOLD, buyer.getUID(), buyer.getDbID(),
                System.currentTimeMillis());
    }

    private boolean updateActiveListingStatus(
            long areaId,
            ClaimSaleStatus status,
            String buyerUuid,
            int buyerDbId,
            long purchasedAt) {
        String sql = """
                UPDATE claimSaleListings
                SET status = ?, buyer_uuid = ?, buyer_dbid = ?, purchased_at = ?
                WHERE world = ? AND area_id = ? AND status = ?;
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, buyerUuid == null ? "" : buyerUuid);
            statement.setInt(3, buyerDbId);
            statement.setLong(4, purchasedAt);
            statement.setString(5, world);
            statement.setLong(6, areaId);
            statement.setString(7, ClaimSaleStatus.ACTIVE.name());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not update claim sale listing: " + ex.getMessage());
            return false;
        }
    }

    private Optional<ClaimSaleListing> findById(long id) throws SQLException {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        }
    }

    private ClaimSaleListing read(ResultSet result) throws SQLException {
        return new ClaimSaleListing(
                result.getLong("id"),
                result.getString("world"),
                result.getLong("area_id"),
                result.getString("owner_uuid"),
                result.getInt("owner_dbid"),
                result.getLong("price"),
                result.getLong("listed_at"),
                result.getString("buyer_uuid"),
                result.getInt("buyer_dbid"),
                result.getLong("purchased_at"),
                status(result.getString("status")));
    }

    private ClaimSaleStatus status(String value) {
        try {
            return ClaimSaleStatus.valueOf(value);
        } catch (RuntimeException ex) {
            return ClaimSaleStatus.WITHDRAWN;
        }
    }

    private void init() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " world TEXT NOT NULL,"
                + " area_id BIGINT NOT NULL,"
                + " owner_uuid TEXT NOT NULL,"
                + " owner_dbid INTEGER NOT NULL DEFAULT 0,"
                + " price BIGINT NOT NULL,"
                + " listed_at BIGINT NOT NULL,"
                + " buyer_uuid TEXT NOT NULL DEFAULT '',"
                + " buyer_dbid INTEGER NOT NULL DEFAULT 0,"
                + " purchased_at BIGINT NOT NULL DEFAULT 0,"
                + " status TEXT NOT NULL"
                + ");";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_claimSaleListings_area_status ON "
                    + TABLE + "(world, area_id, status);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_claimSaleListings_status_listed ON "
                    + TABLE + "(world, status, listed_at);");
        }
    }
}

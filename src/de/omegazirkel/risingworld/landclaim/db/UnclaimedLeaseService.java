package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.World;

/** Land-Claim-owned state; all debits remain in OZ Wallet. */
public final class UnclaimedLeaseService {
    private final Connection connection;
    private final String world;

    public UnclaimedLeaseService(Connection connection) throws SQLException { this(connection, World.getName()); }
    public UnclaimedLeaseService(Connection connection, String world) throws SQLException {
        this.connection = connection; this.world = world == null ? "" : world; init();
    }
    public boolean create(long areaId, String tenantUuid, int tenantDbId, long purchasePrice, long dailyRent,
            long ancillaryCost, String billingDate) {
        if (areaId <= 0 || tenantUuid == null || tenantUuid.isBlank() || tenantDbId <= 0 || purchasePrice < 0
                || dailyRent < 0 || ancillaryCost < 0) return false;
        try (PreparedStatement s = connection.prepareStatement("INSERT INTO unclaimedLeaseholds(world,area_id,tenant_uuid,tenant_dbid,purchase_price,daily_rent,ancillary_cost,rent_credit,last_billing_date) VALUES(?,?,?,?,?,?,?,?,?)")) {
            s.setString(1, world); s.setLong(2, areaId); s.setString(3, tenantUuid); s.setInt(4, tenantDbId);
            s.setLong(5, purchasePrice); s.setLong(6, dailyRent); s.setLong(7, ancillaryCost); s.setLong(8, 0);
            s.setString(9, billingDate == null ? "" : billingDate); return s.executeUpdate() == 1;
        } catch (SQLException e) { LandClaim.logger().error("Could not create unclaimed lease: " + e.getMessage()); return false; }
    }
    public List<UnclaimedLeaseRecord> active() {
        List<UnclaimedLeaseRecord> result = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement("SELECT * FROM unclaimedLeaseholds WHERE world=?")) {
            s.setString(1, world); try (ResultSet r = s.executeQuery()) { while (r.next()) result.add(read(r)); }
        } catch (SQLException e) { LandClaim.logger().error("Could not list unclaimed leases: " + e.getMessage()); }
        return result;
    }
    public java.util.Optional<UnclaimedLeaseRecord> find(long areaId) {
        try (PreparedStatement s = connection.prepareStatement("SELECT * FROM unclaimedLeaseholds WHERE world=? AND area_id=?")) {
            s.setString(1, world); s.setLong(2, areaId); try (ResultSet r = s.executeQuery()) {
                return r.next() ? java.util.Optional.of(read(r)) : java.util.Optional.empty();
            }
        } catch (SQLException e) { return java.util.Optional.empty(); }
    }
    public boolean recordRent(long areaId, long credit, String date) { return update("UPDATE unclaimedLeaseholds SET rent_credit=?,last_billing_date=? WHERE world=? AND area_id=?", areaId, credit, date); }
    public boolean remove(long areaId) { try (PreparedStatement s = connection.prepareStatement("DELETE FROM unclaimedLeaseholds WHERE world=? AND area_id=?")) { s.setString(1, world); s.setLong(2, areaId); return s.executeUpdate() == 1; } catch (SQLException e) { return false; } }
    private boolean update(String sql, long areaId, long credit, String date) { try (PreparedStatement s = connection.prepareStatement(sql)) { s.setLong(1, Math.max(0, credit)); s.setString(2, date); s.setString(3, world); s.setLong(4, areaId); return s.executeUpdate() == 1; } catch (SQLException e) { return false; } }
    private UnclaimedLeaseRecord read(ResultSet r) throws SQLException { return new UnclaimedLeaseRecord(r.getLong("area_id"), r.getString("tenant_uuid"), r.getInt("tenant_dbid"), r.getLong("purchase_price"), r.getLong("daily_rent"), r.getLong("ancillary_cost"), r.getLong("rent_credit"), r.getString("last_billing_date")); }
    private void init() throws SQLException { try (Statement s = connection.createStatement()) { s.execute("CREATE TABLE IF NOT EXISTS unclaimedLeaseholds(world TEXT NOT NULL,area_id BIGINT NOT NULL,tenant_uuid TEXT NOT NULL,tenant_dbid INTEGER NOT NULL,purchase_price BIGINT NOT NULL,daily_rent BIGINT NOT NULL,ancillary_cost BIGINT NOT NULL,rent_credit BIGINT NOT NULL DEFAULT 0,last_billing_date TEXT NOT NULL DEFAULT '',PRIMARY KEY(world,area_id))"); } }
}

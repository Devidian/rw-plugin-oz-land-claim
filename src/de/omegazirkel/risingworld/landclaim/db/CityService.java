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
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import net.risingworld.api.World;
import net.risingworld.api.objects.Area;
import net.risingworld.api.utils.Vector3i;

public final class CityService {
    /** Rising World sectors span 256 chunks on each horizontal axis. */
    public static final int CHUNKS_PER_SECTOR = 256;
    private final Connection connection;
    private final String world = World.getName();

    public CityService(Connection connection) throws SQLException {
        this.connection = connection;
        init();
    }

    public synchronized CityRecord createCity(Area core, String name, int radius) {
        CityCreationEligibility eligibility = creationEligibility(core, radius);
        if (!eligibility.eligible()) return null;
        Vector3i center = core.getStartChunkPosition();
        int safeRadius = Math.max(0, radius);
        String sql = "INSERT INTO cities(world,area_id,name,center_x,center_y,center_z,radius,founded_at,"
                + "allow_private_override,private_price_override) VALUES(?,?,?,?,?,?,?,?,NULL,NULL)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, core.getID());
            statement.setString(3, name == null || name.isBlank() ? "City " + core.getID() : name.trim());
            statement.setInt(4, center.x);
            statement.setInt(5, center.y);
            statement.setInt(6, center.z);
            statement.setInt(7, safeRadius);
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
            return findCity(core.getID()).orElse(null);
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not create city: " + ex.getMessage());
            return null;
        }
    }

    public Optional<CityRecord> findCity(long areaId) {
        return queryCity("SELECT * FROM cities WHERE world=? AND area_id=?", statement -> statement.setLong(2, areaId));
    }

    /** Checks city-core geometry before creating persistence or a Wallet account. */
    public CityCreationEligibility creationEligibility(Area core, int radius) {
        if (core == null || core.getID() <= 0) {
            return new CityCreationEligibility(false, CityCreationBlocker.INVALID_CORE);
        }
        if (!sameChunk(core.getStartChunkPosition(), core.getEndChunkPosition())) {
            return new CityCreationEligibility(false, CityCreationBlocker.CORE_NOT_SINGLE_CHUNK);
        }
        Vector3i center = core.getStartChunkPosition();
        int safeRadius = Math.max(0, radius);
        if (!withinSector(center, safeRadius)) {
            return new CityCreationEligibility(false, CityCreationBlocker.SECTOR_BOUNDARY);
        }
        if (overlapsAnother(center, safeRadius, 0)) {
            return new CityCreationEligibility(false, CityCreationBlocker.CITY_OVERLAP);
        }
        return new CityCreationEligibility(true, null);
    }

    public record CityCreationEligibility(boolean eligible, CityCreationBlocker blocker) { }

    public enum CityCreationBlocker {
        INVALID_CORE,
        CORE_NOT_SINGLE_CHUNK,
        SECTOR_BOUNDARY,
        CITY_OVERLAP
    }

    public Optional<CityRecord> containingCity(Vector3i chunk) {
        if (chunk == null) return Optional.empty();
        String sql = "SELECT * FROM cities WHERE world=? AND ABS(center_x-?)<=radius AND ABS(center_y-?)<=radius "
                + "AND ABS(center_z-?)<=radius ORDER BY founded_at LIMIT 1";
        return queryCity(sql, statement -> {
            statement.setInt(2, chunk.x);
            statement.setInt(3, chunk.y);
            statement.setInt(4, chunk.z);
        });
    }

    public List<CityRecord> listCities(String search, int offset, int limit) {
        String value = search == null ? "" : search.trim().toLowerCase();
        List<CityRecord> values = new ArrayList<>();
        String sql = "SELECT * FROM cities WHERE world=? AND LOWER(name) LIKE ? ORDER BY founded_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setString(2, "%" + value + "%");
            statement.setInt(3, Math.min(100, Math.max(1, limit)));
            statement.setInt(4, Math.max(0, offset));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(readCity(result));
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not list cities: " + ex.getMessage());
        }
        return values;
    }

    public boolean expandCity(long areaId) {
        CityRecord city = findCity(areaId).orElse(null);
        if (city == null || !expansionEligibility(areaId).eligible()) return false;
        int next = city.radius() + 1;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cities SET radius=? WHERE world=? AND area_id=? AND radius=?")) {
            statement.setInt(1, next);
            statement.setString(2, world);
            statement.setLong(3, areaId);
            statement.setInt(4, city.radius());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not expand city: " + ex.getMessage());
            return false;
        }
    }

    /** Checks geometry only, so the UI never collects a payment for an impossible expansion. */
    public boolean canExpandCity(long areaId) {
        return expansionEligibility(areaId).eligible();
    }

    public ExpansionEligibility expansionEligibility(long areaId) {
        CityRecord city = findCity(areaId).orElse(null);
        if (city == null) return new ExpansionEligibility(false, ExpansionBlocker.CITY_NOT_FOUND);
        if (city.radius() == Integer.MAX_VALUE) return new ExpansionEligibility(false, ExpansionBlocker.MAX_RADIUS);
        int next = city.radius() + 1;
        if (!withinSector(city.center(), next)) {
            return new ExpansionEligibility(false, ExpansionBlocker.SECTOR_BOUNDARY);
        }
        if (overlapsAnother(city.center(), next, city.areaId())) {
            return new ExpansionEligibility(false, ExpansionBlocker.CITY_OVERLAP);
        }
        return new ExpansionEligibility(true, null);
    }

    public record ExpansionEligibility(boolean eligible, ExpansionBlocker blocker) { }

    public enum ExpansionBlocker {
        CITY_NOT_FOUND,
        MAX_RADIUS,
        SECTOR_BOUNDARY,
        CITY_OVERLAP
    }

    public boolean updateCityOverrides(long areaId, Boolean allowPrivate, Long privatePrice) {
        if (privatePrice != null && (privatePrice < 0 || privatePrice > LandPriceService.MAX_SAFE_INTEGER)) return false;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE cities SET allow_private_override=?,"
                + "private_price_override=? WHERE world=? AND area_id=?")) {
            if (allowPrivate == null) statement.setNull(1, java.sql.Types.INTEGER);
            else statement.setInt(1, allowPrivate ? 1 : 0);
            if (privatePrice == null) statement.setNull(2, java.sql.Types.BIGINT);
            else statement.setLong(2, privatePrice);
            statement.setString(3, world);
            statement.setLong(4, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not update city overrides: " + ex.getMessage());
            return false;
        }
    }

    public boolean renameCity(long areaId, String name) {
        if (name == null || name.isBlank()) return false;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cities SET name=? WHERE world=? AND area_id=?")) {
            statement.setString(1, name.trim());
            statement.setString(2, world);
            statement.setLong(3, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not rename city: " + ex.getMessage());
            return false;
        }
    }

    public boolean deleteCityRecord(long areaId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM cities WHERE world=? AND area_id=?")) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not delete city record: " + ex.getMessage());
            return false;
        }
    }

    public boolean deleteLeaseholdRecord(long areaId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM leaseholds WHERE world=? AND area_id=?")) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not delete leasehold record: " + ex.getMessage());
            return false;
        }
    }

    public long expansionChunkCount(CityRecord city) {
        if (city == null) return 0;
        long oldSide = 2L * city.radius() + 1L;
        long newSide = oldSide + 2L;
        try { return Math.subtractExact(Math.multiplyExact(Math.multiplyExact(newSide, newSide), newSide),
                Math.multiplyExact(Math.multiplyExact(oldSide, oldSide), oldSide)); }
        catch (ArithmeticException ex) { return Long.MAX_VALUE; }
    }

    public synchronized LeaseholdRecord createLeasehold(Area area, CityRecord city) {
        if (area == null || city == null || containingCity(area.getStartChunkPosition()).map(CityRecord::areaId)
                .orElse(0L) != city.areaId() || !contains(city, area.getEndChunkPosition())) return null;
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO leaseholds(world,area_id,city_area_id,"
                + "purchase_price,daily_rent,purchase_allowed,rent_allowed,owner_uuid,owner_dbid,ownership_type,"
                + "paid_rent_credit,last_billing_date) VALUES(?,?,?,0,0,0,0,'',0,'NONE',0,'')")) {
            statement.setString(1, world);
            statement.setLong(2, area.getID());
            statement.setLong(3, city.areaId());
            statement.executeUpdate();
            return findLeasehold(area.getID()).orElse(null);
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not create leasehold: " + ex.getMessage());
            return null;
        }
    }

    public Optional<LeaseholdRecord> findLeasehold(long areaId) {
        String sql = "SELECT * FROM leaseholds WHERE world=? AND area_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readLeasehold(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read leasehold: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public List<LeaseholdRecord> rentedLeaseholds() {
        List<LeaseholdRecord> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM leaseholds WHERE world=? AND ownership_type='RENTED' AND owner_dbid>0")) {
            statement.setString(1, world);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(readLeasehold(result));
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not list rented leaseholds: " + ex.getMessage());
        }
        return values;
    }

    public List<LeaseholdRecord> leaseholdsForCity(long cityAreaId) {
        List<LeaseholdRecord> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM leaseholds WHERE world=? AND city_area_id=? ORDER BY area_id")) {
            statement.setString(1, world);
            statement.setLong(2, cityAreaId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(readLeasehold(result));
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not list city leaseholds: " + ex.getMessage());
        }
        return values;
    }

    /** Compact city-dashboard statistics; rented parcels alone produce daily income. */
    public LeaseholdSummary leaseholdSummary(long cityAreaId) {
        String sql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN owner_dbid>0 AND owner_uuid<>'' THEN 1 ELSE 0 END) AS occupied, "
                + "SUM(CASE WHEN ownership_type='RENTED' AND owner_dbid>0 THEN 1 ELSE 0 END) AS rented, "
                + "COALESCE(SUM(CASE WHEN ownership_type='RENTED' AND owner_dbid>0 THEN daily_rent ELSE 0 END),0) AS income "
                + "FROM leaseholds WHERE world=? AND city_area_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, cityAreaId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return new LeaseholdSummary(result.getInt("total"), result.getInt("occupied"),
                        result.getInt("rented"), result.getLong("income"));
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not calculate city leasehold summary: " + ex.getMessage());
        }
        return new LeaseholdSummary(0, 0, 0, 0);
    }

    public boolean configureLeasehold(long areaId, long purchasePrice, long dailyRent,
            boolean purchaseAllowed, boolean rentAllowed) {
        if (purchasePrice < 0 || dailyRent < 0 || purchasePrice > LandPriceService.MAX_SAFE_INTEGER
                || dailyRent > LandPriceService.MAX_SAFE_INTEGER) return false;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE leaseholds SET purchase_price=?,"
                + "daily_rent=?,purchase_allowed=?,rent_allowed=? WHERE world=? AND area_id=?")) {
            statement.setLong(1, purchasePrice);
            statement.setLong(2, dailyRent);
            statement.setInt(3, purchaseAllowed ? 1 : 0);
            statement.setInt(4, rentAllowed ? 1 : 0);
            statement.setString(5, world);
            statement.setLong(6, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not configure leasehold: " + ex.getMessage());
            return false;
        }
    }

    public boolean assignLeasehold(long areaId, String ownerUuid, int ownerDbId, String type, long rentCredit,
            String billingDate) {
        if (ownerUuid == null || ownerUuid.isBlank() || ownerDbId <= 0
                || !("RENTED".equals(type) || "PURCHASED".equals(type))) return false;
        return updateLeaseOwner(areaId, ownerUuid, ownerDbId, type, Math.max(0, rentCredit), billingDate);
    }

    public boolean clearLeasehold(long areaId) {
        return updateLeaseOwner(areaId, "", 0, "NONE", 0, "");
    }

    public boolean recordRent(long areaId, long newCredit, String billingDate) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE leaseholds SET paid_rent_credit=?,"
                + "last_billing_date=? WHERE world=? AND area_id=? AND ownership_type='RENTED'")) {
            statement.setLong(1, Math.max(0, newCredit));
            statement.setString(2, billingDate == null ? "" : billingDate);
            statement.setString(3, world);
            statement.setLong(4, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not record rent: " + ex.getMessage());
            return false;
        }
    }

    public int leaseholdClaimWeight(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank()) return 0;
        int total = 0;
        String sql = "SELECT area_id FROM leaseholds WHERE world=? AND owner_uuid=? AND owner_dbid>0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setString(2, ownerUuid);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Area area = net.risingworld.api.Server.getArea(result.getLong(1));
                    if (area != null) total = Math.addExact(total, ChunkClaimUtil.areaToChunks(area).size());
                }
            }
        } catch (SQLException | ArithmeticException ex) {
            return Integer.MAX_VALUE;
        }
        return total;
    }

    public long addPendingNotification(int playerDbId, String messageKey, String arguments) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO landClaimPendingNotifications"
                + "(world,player_dbid,message_key,message_args,created_at) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, world);
            statement.setInt(2, playerDbId);
            statement.setString(3, messageKey);
            statement.setString(4, arguments == null ? "" : arguments);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not store pending notification: " + ex.getMessage());
            return 0;
        }
    }

    public void rememberPlayerLanguage(int playerDbId, String language) {
        if (playerDbId <= 0 || language == null || language.isBlank()) return;
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO landClaimPlayerLocales"
                + "(world,player_dbid,language,updated_at) VALUES(?,?,?,?) ON CONFLICT(world,player_dbid) DO UPDATE SET "
                + "language=excluded.language,updated_at=excluded.updated_at")) {
            statement.setString(1, world);
            statement.setInt(2, playerDbId);
            statement.setString(3, language.trim().toLowerCase(java.util.Locale.ROOT));
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not store player language: " + ex.getMessage());
        }
    }

    public Optional<String> playerLanguage(int playerDbId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT language FROM landClaimPlayerLocales WHERE world=? AND player_dbid=?")) {
            statement.setString(1, world);
            statement.setInt(2, playerDbId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read player language: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public List<PendingNotification> pendingNotifications(int playerDbId) {
        List<PendingNotification> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id,message_key,message_args FROM "
                + "landClaimPendingNotifications WHERE world=? AND player_dbid=? ORDER BY created_at")) {
            statement.setString(1, world);
            statement.setInt(2, playerDbId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(new PendingNotification(result.getLong(1), result.getString(2),
                        result.getString(3)));
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read pending notifications: " + ex.getMessage());
        }
        return values;
    }

    public void deletePendingNotification(long id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM landClaimPendingNotifications WHERE world=? AND id=?")) {
            statement.setString(1, world);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not delete pending notification: " + ex.getMessage());
        }
    }

    public void beginEconomyOperation(String correlationId, String type, long areaId, int playerDbId, long amount) {
        if (correlationId == null || correlationId.isBlank()) return;
        String sql = "INSERT OR IGNORE INTO landClaimEconomyOperations(correlation_id,world,operation_type,area_id,"
                + "player_dbid,amount,status,created_at,updated_at,last_error) VALUES(?,?,?,?,?,?,'PREPARED',?,?, '')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            statement.setString(1, correlationId);
            statement.setString(2, world);
            statement.setString(3, type);
            statement.setLong(4, areaId);
            statement.setInt(5, playerDbId);
            statement.setLong(6, amount);
            statement.setLong(7, now);
            statement.setLong(8, now);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not journal economy operation: " + ex.getMessage());
        }
    }

    public void updateEconomyOperation(String correlationId, String status, long areaId, String error) {
        if (correlationId == null || correlationId.isBlank()) return;
        try (PreparedStatement statement = connection.prepareStatement("UPDATE landClaimEconomyOperations SET "
                + "status=?,area_id=?,updated_at=?,last_error=? WHERE correlation_id=? AND world=?")) {
            statement.setString(1, status);
            statement.setLong(2, areaId);
            statement.setLong(3, System.currentTimeMillis());
            statement.setString(4, error == null ? "" : error);
            statement.setString(5, correlationId);
            statement.setString(6, world);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not update economy operation journal: " + ex.getMessage());
        }
    }

    public int countUnresolvedEconomyOperations() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM landClaimEconomyOperations "
                + "WHERE world=? AND status NOT IN ('COMPLETED','REVERSED','FAILED')")) {
            statement.setString(1, world);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : 0; }
        } catch (SQLException ex) {
            return -1;
        }
    }

    private boolean updateLeaseOwner(long areaId, String uuid, int dbId, String type, long credit, String date) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE leaseholds SET owner_uuid=?,owner_dbid=?,"
                + "ownership_type=?,paid_rent_credit=?,last_billing_date=? WHERE world=? AND area_id=?")) {
            statement.setString(1, uuid);
            statement.setInt(2, dbId);
            statement.setString(3, type);
            statement.setLong(4, credit);
            statement.setString(5, date == null ? "" : date);
            statement.setString(6, world);
            statement.setLong(7, areaId);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not update leasehold owner: " + ex.getMessage());
            return false;
        }
    }

    private boolean overlapsAnother(Vector3i center, int radius, long excludedAreaId) {
        String sql = "SELECT 1 FROM cities WHERE world=? AND area_id<>? AND "
                + "ABS(center_x-?)<=radius+? AND ABS(center_z-?)<=radius+? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, excludedAreaId);
            statement.setInt(3, center.x);
            statement.setInt(4, radius);
            statement.setInt(5, center.z);
            statement.setInt(6, radius);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not validate city overlap: " + ex.getMessage());
            return true;
        }
    }

    public static boolean contains(CityRecord city, Vector3i chunk) {
        return city != null && chunk != null
                && Math.abs((long) chunk.x - city.center().x) <= city.radius()
                && Math.abs((long) chunk.y - city.center().y) <= city.radius()
                && Math.abs((long) chunk.z - city.center().z) <= city.radius();
    }

    static boolean sameChunk(Vector3i first, Vector3i second) {
        return first != null && second != null && first.x == second.x && first.y == second.y && first.z == second.z;
    }

    public static boolean withinSector(Vector3i center, int radius) {
        if (center == null || radius < 0) return false;
        return Math.floorDiv(center.x - radius, CHUNKS_PER_SECTOR) == Math.floorDiv(center.x, CHUNKS_PER_SECTOR)
                && Math.floorDiv(center.x + radius, CHUNKS_PER_SECTOR) == Math.floorDiv(center.x, CHUNKS_PER_SECTOR)
                && Math.floorDiv(center.z - radius, CHUNKS_PER_SECTOR) == Math.floorDiv(center.z, CHUNKS_PER_SECTOR)
                && Math.floorDiv(center.z + radius, CHUNKS_PER_SECTOR) == Math.floorDiv(center.z, CHUNKS_PER_SECTOR);
    }

    private Optional<CityRecord> queryCity(String sql, SqlBinder binder) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readCity(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read city: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private CityRecord readCity(ResultSet result) throws SQLException {
        Object allow = result.getObject("allow_private_override");
        Object price = result.getObject("private_price_override");
        return new CityRecord(result.getLong("area_id"), result.getString("world"), result.getString("name"),
                new Vector3i(result.getInt("center_x"), result.getInt("center_y"), result.getInt("center_z")),
                result.getInt("radius"), result.getLong("founded_at"),
                allow == null ? null : result.getInt("allow_private_override") != 0,
                price == null ? null : result.getLong("private_price_override"));
    }

    private LeaseholdRecord readLeasehold(ResultSet result) throws SQLException {
        return new LeaseholdRecord(result.getLong("area_id"), result.getLong("city_area_id"),
                result.getLong("purchase_price"), result.getLong("daily_rent"),
                result.getInt("purchase_allowed") != 0, result.getInt("rent_allowed") != 0,
                result.getString("owner_uuid"), result.getInt("owner_dbid"), result.getString("ownership_type"),
                result.getLong("paid_rent_credit"), result.getString("last_billing_date"));
    }

    private void init() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS cities(world TEXT NOT NULL,area_id BIGINT NOT NULL,name TEXT NOT NULL,"
                    + "center_x INTEGER NOT NULL,center_y INTEGER NOT NULL,center_z INTEGER NOT NULL,radius INTEGER NOT NULL,"
                    + "founded_at BIGINT NOT NULL,allow_private_override INTEGER,private_price_override BIGINT,"
                    + "PRIMARY KEY(world,area_id))");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_cities_founded ON cities(world,founded_at DESC)");
            statement.execute("CREATE TABLE IF NOT EXISTS leaseholds(world TEXT NOT NULL,area_id BIGINT NOT NULL,city_area_id BIGINT NOT NULL,"
                    + "purchase_price BIGINT NOT NULL,daily_rent BIGINT NOT NULL,purchase_allowed INTEGER NOT NULL,rent_allowed INTEGER NOT NULL,"
                    + "owner_uuid TEXT NOT NULL,owner_dbid INTEGER NOT NULL,ownership_type TEXT NOT NULL,paid_rent_credit BIGINT NOT NULL,"
                    + "last_billing_date TEXT NOT NULL,PRIMARY KEY(world,area_id))");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_leaseholds_owner ON leaseholds(world,owner_uuid)");
            statement.execute("CREATE TABLE IF NOT EXISTS landClaimPendingNotifications(id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "world TEXT NOT NULL,player_dbid INTEGER NOT NULL,message_key TEXT NOT NULL,message_args TEXT NOT NULL,created_at BIGINT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS landClaimPlayerLocales(world TEXT NOT NULL,player_dbid INTEGER NOT NULL,"
                    + "language TEXT NOT NULL,updated_at BIGINT NOT NULL,PRIMARY KEY(world,player_dbid))");
            statement.execute("CREATE TABLE IF NOT EXISTS landClaimEconomyOperations(correlation_id TEXT PRIMARY KEY,world TEXT NOT NULL,"
                    + "operation_type TEXT NOT NULL,area_id BIGINT NOT NULL,player_dbid INTEGER NOT NULL,amount BIGINT NOT NULL,"
                    + "status TEXT NOT NULL,created_at BIGINT NOT NULL,updated_at BIGINT NOT NULL,last_error TEXT NOT NULL)");
        }
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
    public record PendingNotification(long id, String messageKey, String arguments) { }
}

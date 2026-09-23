package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.World;

/** Land-Claim-owned persistence; Wallet remains the payment authority. */
public final class PlayerLeaseService {
    private final Connection connection;
    private final String world;
    public PlayerLeaseService(Connection connection) throws SQLException { this(connection, World.getName()); }
    public PlayerLeaseService(Connection connection, String world) throws SQLException {
        this.connection = connection; this.world = world == null ? "" : world; init();
    }
    public Optional<PlayerLeaseRecord> find(long areaId) { return query("SELECT * FROM playerLeaseholds WHERE world=? AND area_id=?", areaId); }
    public List<PlayerLeaseRecord> occupied() {
        List<PlayerLeaseRecord> records = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement("SELECT * FROM playerLeaseholds WHERE world=? AND tenant_dbid>0")) {
            s.setString(1, world); try (ResultSet r = s.executeQuery()) { while (r.next()) records.add(read(r)); }
        } catch (SQLException e) { LandClaim.logger().error("Could not list player leases: " + e.getMessage()); }
        return records;
    }
    public boolean offer(long areaId, String landlordUuid, int landlordDbId, long price, long rent, boolean purchaseAllowed) {
        if (areaId <= 0 || landlordUuid == null || landlordUuid.isBlank() || landlordDbId <= 0 || price < 0 || rent < 0) return false;
        String sql = "INSERT INTO playerLeaseholds(world,area_id,landlord_uuid,landlord_dbid,tenant_uuid,tenant_dbid,purchase_price,daily_rent,purchase_allowed,rent_credit,last_billing_date) VALUES(?,?,?,?, '',0,?,?,?,0,'') ON CONFLICT(world,area_id) DO UPDATE SET landlord_uuid=excluded.landlord_uuid,landlord_dbid=excluded.landlord_dbid,purchase_price=excluded.purchase_price,daily_rent=excluded.daily_rent,purchase_allowed=excluded.purchase_allowed WHERE playerLeaseholds.tenant_dbid=0";
        try (PreparedStatement s = connection.prepareStatement(sql)) { s.setString(1,world);s.setLong(2,areaId);s.setString(3,landlordUuid);s.setInt(4,landlordDbId);s.setLong(5,price);s.setLong(6,rent);s.setInt(7,purchaseAllowed?1:0);return s.executeUpdate()==1; } catch(SQLException e){LandClaim.logger().error("Could not offer player lease: "+e.getMessage());return false;}
    }
    public boolean assign(long areaId,String tenantUuid,int tenantDbId,String billingDate){return update("UPDATE playerLeaseholds SET tenant_uuid=?,tenant_dbid=?,last_billing_date=? WHERE world=? AND area_id=? AND tenant_dbid=0",tenantUuid,tenantDbId,billingDate,areaId);}
    public boolean savePermissionSnapshot(long areaId, Map<Integer, String> permissions) {
        if (areaId <= 0 || permissions == null) return false;
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM playerLeasePermissionSnapshots WHERE world=? AND area_id=?")) {
            delete.setString(1, world); delete.setLong(2, areaId); delete.executeUpdate();
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO playerLeasePermissionSnapshots(world,area_id,player_dbid,permission) VALUES(?,?,?,?)")) {
                for (Map.Entry<Integer, String> entry : permissions.entrySet()) {
                    if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null) continue;
                    insert.setString(1, world); insert.setLong(2, areaId); insert.setInt(3, entry.getKey()); insert.setString(4, entry.getValue()); insert.addBatch();
                }
                insert.executeBatch();
            }
            return true;
        } catch (SQLException e) { LandClaim.logger().error("Could not save player lease permission snapshot: " + e.getMessage()); return false; }
    }
    public Map<Integer, String> permissionSnapshot(long areaId) {
        Map<Integer, String> permissions = new LinkedHashMap<>();
        try (PreparedStatement s = connection.prepareStatement("SELECT player_dbid,permission FROM playerLeasePermissionSnapshots WHERE world=? AND area_id=?")) {
            s.setString(1, world); s.setLong(2, areaId); try (ResultSet r = s.executeQuery()) { while (r.next()) permissions.put(r.getInt(1), r.getString(2)); }
        } catch (SQLException e) { LandClaim.logger().error("Could not load player lease permission snapshot: " + e.getMessage()); }
        return permissions;
    }
    public boolean removePermissionSnapshot(long areaId){try(PreparedStatement s=connection.prepareStatement("DELETE FROM playerLeasePermissionSnapshots WHERE world=? AND area_id=?")){s.setString(1,world);s.setLong(2,areaId);s.executeUpdate();return true;}catch(SQLException e){return false;}}
    public boolean recordRent(long areaId,long credit,String date){try(PreparedStatement s=connection.prepareStatement("UPDATE playerLeaseholds SET rent_credit=?,last_billing_date=? WHERE world=? AND area_id=? AND tenant_dbid>0")){s.setLong(1,Math.max(0,credit));s.setString(2,date);s.setString(3,world);s.setLong(4,areaId);return s.executeUpdate()==1;}catch(SQLException e){return false;}}
    public boolean clearTenant(long areaId){try(PreparedStatement s=connection.prepareStatement("UPDATE playerLeaseholds SET tenant_uuid='',tenant_dbid=0,rent_credit=0,last_billing_date='' WHERE world=? AND area_id=?")){s.setString(1,world);s.setLong(2,areaId);return s.executeUpdate()==1;}catch(SQLException e){return false;}}
    public boolean withdrawOffer(long areaId){try(PreparedStatement s=connection.prepareStatement("DELETE FROM playerLeaseholds WHERE world=? AND area_id=? AND tenant_dbid=0")){s.setString(1,world);s.setLong(2,areaId);boolean deleted=s.executeUpdate()==1;if(deleted)removePermissionSnapshot(areaId);return deleted;}catch(SQLException e){return false;}}
    public boolean completePurchase(long areaId){try(PreparedStatement s=connection.prepareStatement("DELETE FROM playerLeaseholds WHERE world=? AND area_id=? AND tenant_dbid>0")){s.setString(1,world);s.setLong(2,areaId);boolean deleted=s.executeUpdate()==1;if(deleted)removePermissionSnapshot(areaId);return deleted;}catch(SQLException e){return false;}}
    private Optional<PlayerLeaseRecord> query(String sql,long areaId){try(PreparedStatement s=connection.prepareStatement(sql)){s.setString(1,world);s.setLong(2,areaId);try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}}catch(SQLException e){return Optional.empty();}}
    private boolean update(String sql,String uuid,int dbid,String date,long id){if(uuid==null||uuid.isBlank()||dbid<=0)return false;try(PreparedStatement s=connection.prepareStatement(sql)){s.setString(1,uuid);s.setInt(2,dbid);s.setString(3,date==null?"":date);s.setString(4,world);s.setLong(5,id);return s.executeUpdate()==1;}catch(SQLException e){return false;}}
    private PlayerLeaseRecord read(ResultSet r)throws SQLException{return new PlayerLeaseRecord(r.getLong("area_id"),r.getString("landlord_uuid"),r.getInt("landlord_dbid"),r.getString("tenant_uuid"),r.getInt("tenant_dbid"),r.getLong("purchase_price"),r.getLong("daily_rent"),r.getInt("purchase_allowed")!=0,r.getLong("rent_credit"),r.getString("last_billing_date"));}
    private void init()throws SQLException{try(Statement s=connection.createStatement()){s.execute("CREATE TABLE IF NOT EXISTS playerLeaseholds(world TEXT NOT NULL,area_id BIGINT NOT NULL,landlord_uuid TEXT NOT NULL,landlord_dbid INTEGER NOT NULL,tenant_uuid TEXT NOT NULL DEFAULT '',tenant_dbid INTEGER NOT NULL DEFAULT 0,purchase_price BIGINT NOT NULL,daily_rent BIGINT NOT NULL,purchase_allowed INTEGER NOT NULL,rent_credit BIGINT NOT NULL DEFAULT 0,last_billing_date TEXT NOT NULL DEFAULT '',PRIMARY KEY(world,area_id))");s.execute("CREATE TABLE IF NOT EXISTS playerLeasePermissionSnapshots(world TEXT NOT NULL,area_id BIGINT NOT NULL,player_dbid INTEGER NOT NULL,permission TEXT NOT NULL,PRIMARY KEY(world,area_id,player_dbid))");}}
}

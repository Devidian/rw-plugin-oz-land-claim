package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.SQLException;

import de.omegazirkel.risingworld.landclaim.db.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.tools.db.SQLiteCachedStore;

public class LandClaimChunkStore extends SQLiteCachedStore<LandClaimChunkKey, LandClaimChunkInfo> {

    public LandClaimChunkStore(Connection con) throws SQLException {
        super(con, new LandClaimChunkSchema("chunkData"), new LandClaimChunkMapper("chunkData"), 60f);
    }
}

package divesttrump.parrotsnoop;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;


@Dao
public interface DbPacketDao {

    @Insert
    void insert(DbPacket dbPacket);

    @Delete
    void delete(DbPacket dbPacket);

    @Query("SELECT * FROM packets WHERE timestamp_long > :timestamp ORDER BY timestamp_long DESC LIMIT :maxDisplay")
    List<DbPacket> getPacketsAfter(long timestamp, int maxDisplay);

    @Query("SELECT * FROM packets ORDER BY timestamp_long DESC")
    List<DbPacket> getAllPackets();
}

package divesttrump.parrotsnoop;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;


@Dao
public interface SettingsDao {

    @Insert
    void insert(Settings settings);

    @Update
    void update(Settings settings);

    @Query("SELECT * FROM settings")
    Settings get();
}
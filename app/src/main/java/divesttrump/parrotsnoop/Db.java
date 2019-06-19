package divesttrump.parrotsnoop;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;


@Database(entities = {DbPacket.class, Settings.class}, version = 1, exportSchema = false)
abstract class Db extends RoomDatabase {

    private static Db INSTANCE;

    abstract DbPacketDao dbPacketDao();
    abstract SettingsDao settingsDao();

    static Db getAppDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Db.class, "parrotsnoop_db").allowMainThreadQueries().build();
        }
        return INSTANCE;
    }

    static void destroyInstance() {
        INSTANCE = null;
    }
}

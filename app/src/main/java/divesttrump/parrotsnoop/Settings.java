package divesttrump.parrotsnoop;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity(tableName = "settings")
class Settings {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "max_packets")
    private int maxPackets;

    @ColumnInfo(name = "max_display_packets")
    private int maxDisplayPackets;

    @ColumnInfo(name = "screen_refresh")
    private int screenRefresh;

    Settings() {
        id = 0;
        maxPackets = 1000;
        maxDisplayPackets = 100;
        screenRefresh = 5000;
    }

    int getId() {
        return this.id;
    }
    void setId(int id) {
        this.id = id;
    }

    int getMaxPackets() {
        return this.maxPackets;
    }
    void setMaxPackets(int maxPackets) {
        this.maxPackets = maxPackets;
    }

    int getMaxDisplayPackets() {
        return this.maxDisplayPackets;
    }
    void setMaxDisplayPackets(int maxDisplayPackets) {
        this.maxDisplayPackets = maxDisplayPackets;
    }

    int getScreenRefresh() {
        return this.screenRefresh;
    }
    void setScreenRefresh(int screenRefresh) {
        this.screenRefresh = screenRefresh;
    }
}

package divesttrump.parrotsnoop;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;


class SettingsViewModel extends ViewModel {

    private Db db;
    private MutableLiveData<Settings> settingsMutableLiveData;

    void initDb(Context context) {
        db = Db.getAppDatabase(context);
        loadSettings();
    }

    LiveData<Settings> getSettings() {
        if (settingsMutableLiveData == null) {
            settingsMutableLiveData = new MutableLiveData<>();
            loadSettings();
        }
        return settingsMutableLiveData;
    }

    private void loadSettings() {
        Settings settings = db.settingsDao().get();
        if (settings == null) {
            settings = new Settings();
            db.settingsDao().insert(settings);
        }
        if (settingsMutableLiveData == null)
            settingsMutableLiveData = new MutableLiveData<>();
        settingsMutableLiveData.setValue(settings);
    }

    void setSettings(Settings settings) {
        if (settingsMutableLiveData == null)
            settingsMutableLiveData = new MutableLiveData<>();
        settingsMutableLiveData.setValue(settings);
        db.settingsDao().update(settings);
    }
}

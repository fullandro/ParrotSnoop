package divesttrump.parrotsnoop;


import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;


public class CaptureClear extends AsyncTask<Void, Void, String> {

    private WeakReference<SettingsTab> settingsTabWeakReference;
    private Db db;

    CaptureClear(SettingsTab settingsTab) {
        settingsTabWeakReference = new WeakReference<>(settingsTab);
        db = Db.getAppDatabase(settingsTab.getContext());
    }

    @Override
    protected String doInBackground(Void... voids) {
        List<DbPacket> packets = db.dbPacketDao().getAllPackets();
        int totalSize = packets.size();
        for (DbPacket packet : packets) {
            db.dbPacketDao().delete(packet);
        }

        return "Cleared " + String.valueOf(totalSize) + " Packets";
    }

    @Override
    protected void onProgressUpdate(Void... voids) {

    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);

        settingsTabWeakReference.get().clearCaptureResult(message);
    }
}

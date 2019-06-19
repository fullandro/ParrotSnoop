package divesttrump.parrotsnoop;


import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;


class CaptureGet extends AsyncTask<Void, Void, List<DbPacket>> {

    private WeakReference<CaptureTab> captureTabWeakReference;
    private Db db;
    private int totalSize = 0;
    private int maxDisplayPackets;

    private Date date;
    private int delta;

    CaptureGet(CaptureTab captureTab, Date date, int delta, int maxDisplayPackets) {
        captureTabWeakReference = new WeakReference<>(captureTab);
        db = Db.getAppDatabase(captureTab.getContext());
        this.date = date;
        this.delta = delta;
        this.maxDisplayPackets = maxDisplayPackets;
    }

    @Override
    protected List<DbPacket> doInBackground(Void... voids) {
        List<DbPacket> packets = db.dbPacketDao().getAllPackets();
        totalSize = packets.size();

        return db.dbPacketDao().getPacketsAfter(date.getTime() - delta, maxDisplayPackets);
    }

    @Override
    protected void onProgressUpdate(Void... voids) {

    }

    @Override
    protected void onPostExecute(List<DbPacket> packets) {
        super.onPostExecute(packets);

        captureTabWeakReference.get().setPackets(packets, totalSize);
    }
}
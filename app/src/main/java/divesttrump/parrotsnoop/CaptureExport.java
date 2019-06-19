package divesttrump.parrotsnoop;


import android.os.AsyncTask;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class CaptureExport extends AsyncTask<Void, Void, String> {

    private static final String TAG = CaptureExport.class.getSimpleName();

    private WeakReference<CaptureTab> captureTabWeakReference;
    private Db db;
    private String fileName;
    private boolean directoryCreated;
    private String filePath;

    CaptureExport(CaptureTab captureTab) {
        captureTabWeakReference = new WeakReference<>(captureTab);
        db = Db.getAppDatabase(captureTab.getContext());
    }

    @Override
    protected String doInBackground(Void... voids) {
        setFileName();
        File directory = getDirectory();
        if (directoryCreated) {
            File file = new File(directory, fileName);
            filePath = file.getAbsolutePath();
            boolean writeSuccess = writeFile(file);
            if (writeSuccess)
                filePath = "Exported to:\n" + filePath;
            else
                filePath = "Could not export to:\n" + filePath;
        }
        return filePath;
    }

    private void setFileName() {
        Date now = Calendar.getInstance().getTime();
        fileName = "Capture_" + DateFormat.format("yyyy", now);
        fileName = fileName + "_" + DateFormat.format("MM", now);
        fileName = fileName + "_" + DateFormat.format("dd", now);
        fileName = fileName + "_" + DateFormat.format("HH", now);
        fileName = fileName + "_" + DateFormat.format("mm", now);
        fileName = fileName + "_" + DateFormat.format("ss", now) + ".csv";
    }

    private File getDirectory() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ParrotSnoop");
        if (directory.exists()) {
            directoryCreated = true;
        } else {
            if (!directory.mkdirs()) {
                directoryCreated = false;
                Log.e(TAG, "Directory not created");
            } else {
                directoryCreated = true;
            }
        }
        return directory;
    }

    private boolean writeFile(File file) {
        List<DbPacket> packets = db.dbPacketDao().getAllPackets();
        FileOutputStream fileOutputStream;
        try {
            //fileOutputStream = contextWeakReference.get().openFileOutput(file.getAbsolutePath(), Context.MODE_APPEND);
            fileOutputStream = new FileOutputStream(file, true);
            for (DbPacket dbPacket : packets) {
                try {
                    String csvLine = dbPacket.getCsvLine();
                    fileOutputStream.write(csvLine.getBytes());
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            fileOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Void... voids) {

    }

    @Override
    protected void onPostExecute(String string) {
        super.onPostExecute(string);

        captureTabWeakReference.get().exportComplete(string);
    }
}
package divesttrump.parrotsnoop;


import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


class AppLoader extends AsyncTask<Void, Void, ArrayList<InstalledApp>> {

    private ArrayList<InstalledApp> apps;
    private WeakReference<AppsTab> appsTabWeakReference;
    private WeakReference<PackageManager> packageManagerWeakReference;

    AppLoader(AppsTab appsTab, PackageManager packageManager) {
        appsTabWeakReference = new WeakReference<>(appsTab);
        packageManagerWeakReference = new WeakReference<>(packageManager);
    }

    @Override
    protected ArrayList<InstalledApp> doInBackground(Void... voids) {
        getInstalledApps();
        return apps;
    }

    private void getInstalledApps() {
        List<ApplicationInfo> packagesMeta = packageManagerWeakReference.get().getInstalledApplications(PackageManager.GET_META_DATA);
        List<PackageInfo> packagesPermission = packageManagerWeakReference.get().getInstalledPackages(PackageManager.GET_PERMISSIONS);

        apps = new ArrayList<>();

        for (ApplicationInfo packageMeta : packagesMeta) {

            String packageName = packageMeta.packageName;
            boolean packageEnabled = packageMeta.enabled;
            Drawable packageIcon = packageMeta.loadIcon(packageManagerWeakReference.get());
            ArrayList<String> packagePermissions = new ArrayList<>();

            for (PackageInfo packagePermission : packagesPermission) {
                if (packagePermission.packageName.equals(packageName)) {
                    String[] permissions = packagePermission.requestedPermissions;
                    if (permissions != null) {
                        packagePermissions.addAll(Arrays.asList(permissions));
                    }
                }
            }

            InstalledApp installedApp = new InstalledApp(packageName, packageEnabled, packageIcon, packagePermissions);
            apps.add(installedApp);
        }

        Collections.sort(apps, new Comparator<InstalledApp>() {
            @Override
            public int compare(InstalledApp i1, InstalledApp i2) {
                return i1.getName().compareTo(i2.getName());
            }
        });
    }

    @Override
    protected void onPostExecute(ArrayList<InstalledApp> arrayList) {
        super.onPostExecute(arrayList);

        appsTabWeakReference.get().setInstalledApps(apps);
    }
}

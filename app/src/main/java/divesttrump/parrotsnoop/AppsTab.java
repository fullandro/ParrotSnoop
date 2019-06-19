package divesttrump.parrotsnoop;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;


public class AppsTab extends Fragment {

    private ViewGroup rootView;

    private PackageManager packageManager;
    private ArrayList<InstalledApp> installedApps;

    private ProgressBar appsTabProgressBar;
    private LinearLayout appMenu;
    private LinearLayout permissionModal;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        rootView = (ViewGroup) inflater.inflate(R.layout.apps_tab, container, false);

        packageManager = rootView.getContext().getPackageManager();

        appsTabProgressBar = rootView.findViewById(R.id.appsTabProgressBar);
        appMenu = rootView.findViewById(R.id.appMenu);
        permissionModal = (LinearLayout) inflater.inflate(R.layout.apps_tab_permissions, rootView, false);
        permissionModal.setVisibility(View.GONE);
        rootView.addView(permissionModal);

        new AppLoader(this, packageManager).execute();

        return rootView;
    }

    protected void setInstalledApps(ArrayList<InstalledApp> installedApps) {
        this.installedApps = installedApps;

        LinearLayout appMenuInner = rootView.findViewById(R.id.appMenuInner);
        appMenuInner.removeAllViews();

        LayoutInflater inflater = getLayoutInflater();

        for (InstalledApp installedApp : this.installedApps) {
            LinearLayout appItem = (LinearLayout) inflater.inflate(R.layout.app_item_template, rootView, false);
            appItem.setTag(installedApp.getName());
            appItem.setOnClickListener(viewPermissions);

            ImageView appEnabledImageView = appItem.findViewById(R.id.appEnabledImageView);
            if (!installedApp.isEnabled())
                appEnabledImageView.setImageDrawable(appMenuInner.getContext().getDrawable(R.drawable.ic_x_red));
            else
                appEnabledImageView.setImageDrawable(appMenuInner.getContext().getDrawable(R.drawable.ic_check_green));
            appEnabledImageView.setContentDescription(installedApp.getName());
            appEnabledImageView.setTag(installedApp.getName());
            appEnabledImageView.setOnClickListener(viewPermissions);

            ImageView appIconImageView = appItem.findViewById(R.id.appIconImageView);
            appIconImageView.setImageDrawable(installedApp.getIcon());
            appIconImageView.setContentDescription(installedApp.getName());
            appIconImageView.setTag(installedApp.getName());
            appIconImageView.setOnClickListener(viewPermissions);

            TextView appNameTextView = appItem.findViewById(R.id.appNameTextView);
            appNameTextView.setText(installedApp.getName());
            appNameTextView.setTag(installedApp.getName());
            appNameTextView.setOnClickListener(viewPermissions);

            appMenuInner.addView(appItem);
            appItem.invalidate();
        }

        permissionModal.setVisibility(View.GONE);
        appsTabProgressBar.setVisibility(View.GONE);
        appMenu.setVisibility(View.VISIBLE);
    }

    protected View.OnClickListener viewPermissions = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showPermissions((String) v.getTag());
        }
    };

    private void showPermissions(String appName) {
        LinearLayout permissionLayout = rootView.findViewById(R.id.permissionLayout);
        permissionLayout.removeAllViews();

        LinearLayout permissionModalCloseLayout = rootView.findViewById(R.id.permissionModalCloseLayout);
        permissionModalCloseLayout.setOnClickListener(hidePermissions);

        ImageView permissionModalCloseImageView = rootView.findViewById(R.id.permissionModalCloseImageView);
        permissionModalCloseImageView.setOnClickListener(hidePermissions);

        for (InstalledApp installedApp : installedApps) {
            if (installedApp.getName().equals(appName)) {
                TextView permissionAppInfoNameTextView = rootView.findViewById(R.id.permissionAppInfoNameTextView);
                permissionAppInfoNameTextView.setText(appName);

                ImageView permissionAppInfoIconImageView = rootView.findViewById(R.id.permissionAppInfoIconImageView);
                permissionAppInfoIconImageView.setImageDrawable(installedApp.getIcon());

                ImageView permissionAppInfoEnabledImageView = rootView.findViewById(R.id.permissionAppInfoEnabledImageView);
                if (installedApp.isEnabled())
                    permissionAppInfoEnabledImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_check_green));
                else
                    permissionAppInfoEnabledImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_x_red));

                LayoutInflater inflater = getLayoutInflater();
                ArrayList<String> permissions = installedApp.getPermissions();
                if (permissions.size() > 0) {
                    for (String permission : permissions) {
                        LinearLayout permissionItem = (LinearLayout) inflater.inflate(R.layout.permission_item_template, rootView, false);

                        ImageView permissionGrantedImageView = permissionItem.findViewById(R.id.permissionGrantedImageView);
                        if (packageManager.checkPermission(permission, installedApp.getName()) == PackageManager.PERMISSION_GRANTED) {
                            permissionGrantedImageView.setImageDrawable(permissionLayout.getContext().getDrawable(R.drawable.ic_check_green));
                            permissionGrantedImageView.setContentDescription("Enabled");
                        } else {
                            permissionGrantedImageView.setImageDrawable(permissionLayout.getContext().getDrawable(R.drawable.ic_x_red));
                            permissionGrantedImageView.setContentDescription("Disabled");
                        }

                        String permissionShort = permission;
                        if (permission.indexOf('.') > -1) {
                            permissionShort = permission.substring(permission.lastIndexOf('.') + 1);
                        }

                        TextView permissionNameTextView = permissionItem.findViewById(R.id.permissionNameTextView);
                        permissionNameTextView.setText(permissionShort);

                        permissionLayout.addView(permissionItem);
                        permissionItem.invalidate();
                    }
                } else {
                    inflater.inflate(R.layout.permission_item_template, permissionLayout);
                }
                break;
            }
        }

        appMenu.setVisibility(View.GONE);
        permissionModal.setVisibility(View.VISIBLE);
    }

    protected View.OnClickListener hidePermissions = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            permissionModal.setVisibility(View.GONE);
            appMenu.setVisibility(View.VISIBLE);
            LinearLayout permissionLayout = rootView.findViewById(R.id.permissionLayout);
            permissionLayout.removeAllViews();
        }
    };
}
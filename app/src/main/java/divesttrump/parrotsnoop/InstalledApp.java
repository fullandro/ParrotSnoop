package divesttrump.parrotsnoop;


import android.graphics.drawable.Drawable;

import java.util.ArrayList;


class InstalledApp {

    private String name;
    private boolean enabled;
    private Drawable icon;
    private ArrayList<String> permissions;


    InstalledApp(String packageName, boolean packageEnabled, Drawable packageIcon, ArrayList<String> packagePermissions) {
        name = packageName;
        enabled = packageEnabled;
        icon = packageIcon;
        permissions = packagePermissions;
    }

    String getName() {
        return name;
    }

    boolean isEnabled() {
        return enabled;
    }

    Drawable getIcon() {
        return icon;
    }

    ArrayList<String> getPermissions() {
        return permissions;
    }
}

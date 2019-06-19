package divesttrump.parrotsnoop;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;


public class ParrotSnoopPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<Fragment> fragments = new ArrayList<>();

    ParrotSnoopPagerAdapter(MainActivity mainActivity, FragmentManager fragmentManager, Bundle savedInstanceState) {
        super(fragmentManager);

        fragments.add(AppsTab.instantiate(mainActivity.getApplicationContext(), AppsTab.class.getName(), savedInstanceState));
        fragments.add(CaptureTab.instantiate(mainActivity.getApplicationContext(), CaptureTab.class.getName(), savedInstanceState));
        fragments.add(SettingsTab.instantiate(mainActivity.getApplicationContext(), SettingsTab.class.getName(), savedInstanceState));
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }
}
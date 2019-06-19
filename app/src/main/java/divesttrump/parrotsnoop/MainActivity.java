package divesttrump.parrotsnoop;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {

    private Bundle savedInstanceState;
    private BottomNavigationView navigation;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ParrotSnoop);
        super.onCreate(savedInstanceState);

        this.savedInstanceState = savedInstanceState;

        loadUI();
    }

    private void loadUI() {
        setContentView(R.layout.activity_main);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(navigationListener);

        ParrotSnoopPagerAdapter pagerAdapter = new ParrotSnoopPagerAdapter(this, getSupportFragmentManager(), this.savedInstanceState);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(false, new ParrotSnoopPageTransformer());
        viewPager.addOnPageChangeListener(swipeListener);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_apps:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_capture:
                    viewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_settings:
                    viewPager.setCurrentItem(2);
                    return true;
            }
            return false;
        }
    };

    private ViewPager.OnPageChangeListener swipeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {
            navigation.getMenu().getItem(0).setChecked(false);
            navigation.getMenu().getItem(1).setChecked(false);
            navigation.getMenu().getItem(2).setChecked(false);
            navigation.getMenu().getItem(i).setChecked(true);
            navigation.invalidate();
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Db.destroyInstance();
    }
}
package divesttrump.parrotsnoop;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;


public class SettingsTab extends Fragment {

    private Settings settings;
    private SettingsViewModel settingsViewModel;

    private ViewGroup rootView;
    private ProgressBar settingsTabProgressBar;
    private LinearLayout settingsLayout;

    private String[] screen_refresh;
    private NumberPicker screenRefreshPicker;
    private String[] display_packets;
    private NumberPicker displayPacketsPicker;
    private String[] store_packets;
    private NumberPicker storePacketsPicker;

    private Button clearCaptureButton;
    private FrameLayout notificationFrame;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        rootView = (ViewGroup) inflater.inflate(R.layout.settings_tab, container, false);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            settingsViewModel = ViewModelProviders.of(activity).get(SettingsViewModel.class);
            settingsViewModel.initDb(getContext());
            settingsViewModel.getSettings().observe(this, new Observer<Settings>() {
                @Override
                public void onChanged(@Nullable Settings settings) {
                    populateSettings(settings);
                }
            });
        }

        loadUI();

        return rootView;
    }

    private void loadUI() {
        settingsTabProgressBar = rootView.findViewById(R.id.settingsTabProgressBar);

        settingsLayout = rootView.findViewById(R.id.settingsLayout);

        screen_refresh = getResources().getStringArray(R.array.screen_refresh);
        screenRefreshPicker = rootView.findViewById(R.id.screenRefreshPicker);
        screenRefreshPicker.setMinValue(0);
        screenRefreshPicker.setMaxValue(screen_refresh.length - 1);
        screenRefreshPicker.setDisplayedValues(screen_refresh);
        screenRefreshPicker.setWrapSelectorWheel(true);
        screenRefreshPicker.setOnValueChangedListener(screenRefreshListener);

        display_packets = getResources().getStringArray(R.array.display_packets);
        displayPacketsPicker = rootView.findViewById(R.id.displayPacketsPicker);
        displayPacketsPicker.setMinValue(0);
        displayPacketsPicker.setMaxValue(display_packets.length - 1);
        displayPacketsPicker.setDisplayedValues(display_packets);
        displayPacketsPicker.setWrapSelectorWheel(true);
        displayPacketsPicker.setOnValueChangedListener(displayPacketsListener);

        store_packets = getResources().getStringArray(R.array.store_packets);
        storePacketsPicker = rootView.findViewById(R.id.storePacketsPicker);
        storePacketsPicker.setMinValue(0);
        storePacketsPicker.setMaxValue(store_packets.length - 1);
        storePacketsPicker.setDisplayedValues(store_packets);
        storePacketsPicker.setWrapSelectorWheel(true);
        storePacketsPicker.setOnValueChangedListener(storePacketsListener);

        clearCaptureButton = rootView.findViewById(R.id.clearCaptureButton);
        clearCaptureButton.setOnClickListener(clearCaptureListener);
    }

    private void populateSettings(@Nullable Settings settings) {
        if (settings != null) {
            this.settings = settings;

            for (int i = 0; i < screen_refresh.length; i++) {
                if (screen_refresh[i].equals(String.valueOf(settings.getScreenRefresh() / 1000))) {
                    screenRefreshPicker.setValue(i);
                    break;
                }
            }
            for (int i = 0; i < display_packets.length; i++) {
                if (display_packets[i].equals(String.valueOf(settings.getMaxDisplayPackets()))) {
                    displayPacketsPicker.setValue(i);
                    break;
                }
            }
            for (int i = 0; i < store_packets.length; i++) {
                if (store_packets[i].equals(String.valueOf(settings.getMaxPackets()))) {
                    storePacketsPicker.setValue(i);
                    break;
                }
            }

            settingsTabProgressBar.setVisibility(View.GONE);
            settingsLayout.setVisibility(View.VISIBLE);
        }
    }

    private NumberPicker.OnValueChangeListener screenRefreshListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int selectedValue = Integer.parseInt(screen_refresh[newVal]);
            settings.setScreenRefresh(selectedValue * 1000);
            updateSettings();
        }
    };

    private NumberPicker.OnValueChangeListener displayPacketsListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int selectedValue = Integer.parseInt(display_packets[newVal]);
            settings.setMaxDisplayPackets(selectedValue);
            updateSettings();
        }
    };

    private NumberPicker.OnValueChangeListener storePacketsListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int selectedValue = Integer.parseInt(store_packets[newVal]);
            settings.setMaxPackets(selectedValue);
            updateSettings();
        }
    };

    private void updateSettings() {
        settingsViewModel.setSettings(settings);
    }

    private View.OnClickListener clearCaptureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            clearCapture();
        }
    };
    private void clearCapture() {
        clearCaptureButton.setEnabled(false);
        new CaptureClear(this).execute();
    }
    protected void clearCaptureResult(String result) {
        notificationFrame = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.notification_view, rootView, false);
        TextView notificationTextView = notificationFrame.findViewById(R.id.notificationTextView);
        notificationTextView.setText(result);
        Animation notificationAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.notification);
        notificationAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                clearNotification();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rootView.addView(notificationFrame);
        notificationFrame.startAnimation(notificationAnimation);
        notificationFrame.setVisibility(View.VISIBLE);
    }
    private void clearNotification() {
        notificationFrame.setVisibility(View.GONE);
        rootView.removeView(notificationFrame);
        notificationFrame = null;
        clearCaptureButton.setEnabled(true);
    }
}
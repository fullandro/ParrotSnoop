package divesttrump.parrotsnoop;


import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class CaptureTab extends Fragment {

    private Handler handler;

    private volatile boolean capturing;
    private Date captureTime;
    private static final int VPN_REQUEST_CODE = 0x0F;
    public static final int REQUEST_WRITE_STORAGE = 112;

    private int captureDelay = 5000;
    private int displayPackets = 100;

    private ViewGroup rootView;
    private ProgressBar captureTabProgressBar;
    private ScrollView captureDetailsScrollView;
    private TextView captureTextView;
    private LinearLayout captureLayout;
    private Button captureButton;
    private Button exportButton;
    private ImageView captureStatusImageView;
    private FrameLayout notificationFrame;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        capturing = false;

        rootView = (ViewGroup) inflater.inflate(R.layout.capture_tab, container, false);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            final SettingsViewModel settingsViewModel = ViewModelProviders.of(activity).get(SettingsViewModel.class);
            settingsViewModel.initDb(getContext());
            settingsViewModel.getSettings().observe(this, new Observer<Settings>() {
                @Override
                public void onChanged(@Nullable Settings settings) {
                    loadSettings(settings);
                }
            });
        }

        loadUI();

        return rootView;
    }

    private void loadSettings(@Nullable Settings settings) {
        if (settings != null) {
            displayPackets = settings.getMaxDisplayPackets();
            captureDelay = settings.getScreenRefresh();

            captureTabProgressBar.setVisibility(View.GONE);
            captureLayout.setVisibility(View.VISIBLE);
        }
    }

    private void loadUI() {
        captureTabProgressBar = rootView.findViewById(R.id.captureTabProgressBar);

        captureDetailsScrollView = (ScrollView) getLayoutInflater().inflate(R.layout.capture_tab_details, rootView, false);
        captureDetailsScrollView.setVisibility(View.GONE);
        rootView.addView(captureDetailsScrollView);

        captureTextView = rootView.findViewById(R.id.captureTextView);
        captureTextView.setOnClickListener(captureTextViewHide);

        captureLayout = rootView.findViewById(R.id.captureLayout);

        captureButton = rootView.findViewById(R.id.captureButton);
        captureButton.setOnClickListener(captureToggle);

        exportButton = rootView.findViewById(R.id.exportButton);
        exportButton.setOnClickListener(exportClick);

        captureStatusImageView = rootView.findViewById(R.id.captureStatusImageView);

        LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(vpnStateReceiver, new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));

        LocalBroadcastManager.getInstance(rootView.getContext()).sendBroadcast(new Intent(LocalVPNService.BROADCAST_VPN_STATE).putExtra("STATE", LocalVPNService.VpnState.STATUS_REPORT.getNumber()));

        handler = new Handler();
        handler.postDelayed(captureRunnable, captureDelay);
    }

    protected void setPackets(List<DbPacket> packets, int totalDbPackets) {
        int displayed = 0;

        if (packets != null) {
            LinearLayout captureView = rootView.findViewById(R.id.captureView);
            captureView.removeAllViews();

            for (DbPacket dbPacket : packets) {
                String packetDescription = dbPacket.getShortDescription();

                TextView packetTextView = new TextView(getContext());
                packetTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                packetTextView.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                packetTextView.setText(packetDescription);
                packetTextView.setTag(dbPacket.getLongDescription());
                packetTextView.setOnClickListener(captureTextViewShow);

                captureView.addView(packetTextView);
            }
            captureView.invalidate();

            displayed = packets.size();
        }
        TextView displayedValue = rootView.findViewById(R.id.captureDisplayedValue);
        displayedValue.setText(String.valueOf(displayed));
        TextView totalValue = rootView.findViewById(R.id.captureTotalValue);
        totalValue.setText(String.valueOf(totalDbPackets));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(LocalVPNService.BROADCAST_VPN_STATE).putExtra("STATE", LocalVPNService.VpnState.STATUS_REPORT.getNumber()));
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(rootView.getContext()).sendBroadcast(new Intent(LocalVPNService.BROADCAST_VPN_STATE).putExtra("STATE", LocalVPNService.VpnState.STATUS_REPORT.getNumber()));
    }


    private Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (capturing) updateCapture();
            handler.postDelayed(captureRunnable, captureDelay);
        }
    };
    private void updateCapture() {
        new CaptureGet(this, captureTime, captureDelay, displayPackets).execute();
    }

    private void startVPN() {
        Intent vpnIntent = VpnService.prepare(getContext());
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null);
    }

    private void stopVPN() {
        LocalBroadcastManager.getInstance(rootView.getContext()).sendBroadcast(new Intent(LocalVPNService.BROADCAST_VPN_STATE).putExtra("STATE", LocalVPNService.VpnState.STOPPING.getNumber()));
    }

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVpnState(intent);
        }
    };
    private void updateVpnState(Intent intent) {
        if (LocalVPNService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
            int vpnState = intent.getIntExtra("STATE", 2);
            if (vpnState == LocalVPNService.VpnState.CLOSED.getNumber()) {
                capturing = false;
                captureButton.setText(R.string.capture_button_description_stopped);
                captureButton.setEnabled(true);
                exportButton.setEnabled(true);
                captureStatusImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_parrot_red));
            } else if (vpnState == LocalVPNService.VpnState.STARTING.getNumber()) {
                capturing = false;
                captureButton.setText(R.string.capture_button_description_started);
                captureButton.setEnabled(false);
                exportButton.setEnabled(false);
                captureStatusImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_parrot_purple));
            } else if (vpnState == LocalVPNService.VpnState.RUNNING.getNumber()) {
                captureTime = Calendar.getInstance().getTime();
                capturing = true;
                captureButton.setText(R.string.capture_button_description_started);
                captureButton.setEnabled(true);
                exportButton.setEnabled(false);
                captureStatusImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_parrot_green));
            } else if (vpnState == LocalVPNService.VpnState.STOPPING.getNumber()) {
                capturing = false;
                captureButton.setText(R.string.capture_button_description_stopped);
                captureButton.setEnabled(false);
                exportButton.setEnabled(false);
                captureStatusImageView.setImageDrawable(rootView.getContext().getDrawable(R.drawable.ic_parrot_purple));
            }
            captureButton.invalidate();
            exportButton.invalidate();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            rootView.getContext().startService(new Intent(rootView.getContext(), LocalVPNService.class));
        }
    }

    private View.OnClickListener captureToggle = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (capturing)
                stopVPN();
            else
                startVPN();
        }
    };

    protected View.OnClickListener captureTextViewShow = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            captureLayout.setVisibility(View.GONE);
            String packetText = (String) v.getTag();
            captureTextView.setText(packetText);
            captureDetailsScrollView.setVisibility(View.VISIBLE);
        }
    };

    private View.OnClickListener captureTextViewHide = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            captureDetailsScrollView.setVisibility(View.GONE);
            captureTextView.setText("");
            captureLayout.setVisibility(View.VISIBLE);
        }
    };

    private View.OnClickListener exportClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setEnabled(false);
            if (rootView.getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                exportToFile();
            else
                requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    };
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToFile();
            }
        }
    }

    private void exportToFile() {
        new CaptureExport(this).execute();
    }

    protected void exportComplete(String fileName) {
        notificationFrame = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.notification_view, rootView, false);
        TextView notificationTextView = notificationFrame.findViewById(R.id.notificationTextView);
        notificationTextView.setText(fileName);
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
        exportButton.setEnabled(true);
    }
}
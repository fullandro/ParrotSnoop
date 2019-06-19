package divesttrump.parrotsnoop;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LocalVPNService extends VpnService {

    private static final String TAG = "ParrotSnoopVPN";

    private static final String VPN_ADDRESS_V4 = "10.0.0.2";
    private static final String VPN_ADDRESS_V6 = "FD:10::2";
    private static final String VPN_ROUTE_V4 = "0.0.0.0";
    private static final String VPN_ROUTE_V6 = "::";

    public static final String BROADCAST_VPN_STATE = "divesttrump.parrotsnooptesting.VPN_STATE";

    private ParcelFileDescriptor vpnInterface = null;

    private PendingIntent pendingIntent;

    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;

    private Selector udpSelector;
    private Selector tcpSelector;

    private int vpnState;
    enum VpnState {
        CLOSED(0),
        STARTING(1),
        RUNNING(2),
        STOPPING(3),
        STATUS_REPORT(4);

        private int vpnState;

        VpnState(int vpnState) {
            this.vpnState = vpnState;
        }

        public int getNumber() {
            return this.vpnState;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        vpnState = VpnState.STARTING.getNumber();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("STATE", vpnState));

        setupVPN();
        try {
            if (vpnInterface != null) {
                FileDescriptor vpnFileDescriptor = null;
                try {
                    vpnFileDescriptor = vpnInterface.getFileDescriptor();
                } catch (Exception e) {
                    Log.e(TAG, "Interface not ready." + e.getMessage());
                }
                if (vpnFileDescriptor != null) {
                    udpSelector = Selector.open();
                    tcpSelector = Selector.open();
                    deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
                    deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
                    networkToDeviceQueue = new ConcurrentLinkedQueue<>();

                    executorService = Executors.newFixedThreadPool(5);
                    executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector));
                    executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, udpSelector, this));
                    executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector));
                    executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));

                    executorService.submit(new VPNRunnable(vpnInterface.getFileDescriptor(), deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue, getApplicationContext()));

                    vpnState = VpnState.RUNNING.getNumber();
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("STATE", vpnState));
                    LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver, new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));

                    Log.i(TAG, "Started VPN");
                } else {
                    Log.e(TAG, "Invalid file descriptor.");
                    stopVPN();
                }
            } else {
                Log.e(TAG, "Invalid file descriptor.");
                stopVPN();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error starting service", e);
            cleanup();
        }
    }

    private void setupVPN() {
        if (vpnInterface == null) {
            try {
                Builder builder = new Builder();
                builder.addAddress(VPN_ADDRESS_V4, 32);
                builder.addAddress(VPN_ADDRESS_V6, 128);
                builder.addRoute(VPN_ROUTE_V4, 0);
                builder.addRoute(VPN_ROUTE_V6, 0);
                vpnInterface = builder.setSession(getString(R.string.app_name)).setConfigureIntent(pendingIntent).establish();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void stopVPN() {
        vpnState = VpnState.STOPPING.getNumber();
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        stopSelf();
    }

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalVPNService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getIntExtra("STATE", 2) == VpnState.STOPPING.getNumber()) {
                    stopVPN();
                } else if (intent.getIntExtra("STATE", 2) == VpnState.STATUS_REPORT.getNumber()) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("STATE", vpnState));
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (executorService != null)
            executorService.shutdownNow();

        cleanup();

        vpnState = VpnState.CLOSED.getNumber();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("STATE", vpnState));
        Log.i(TAG, "Stopped");
    }

    private void cleanup() {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        ByteBufferPool.clear();
        closeResources(udpSelector, tcpSelector, vpnInterface);
    }

    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                if (resource != null)
                    resource.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static class VPNRunnable implements Runnable {

        private static final String TAG = VPNRunnable.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
        private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
        private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

        private Db db;

        VPNRunnable(FileDescriptor vpnFileDescriptor, ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue, ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue, ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue, Context context) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
            db = Db.getAppDatabase(context);
        }

        @Override
        public void run() {
            Log.i(TAG, "Started");

            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();

            try {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived;
                while (!Thread.interrupted()) {
                    if (dataSent)
                        bufferToNetwork = ByteBufferPool.acquire();
                    else
                        bufferToNetwork.clear();

                    int readBytes = vpnInput.read(bufferToNetwork);
                    if (readBytes > 0) {
                        dataSent = true;
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);

                        DbPacket dbPacket = new DbPacket();
                        dbPacket.setIpVersion(packet.getIpVersion());
                        dbPacket.setTransportType(packet.getTransportType());
                        dbPacket.setSourceAddress(packet.getSourceAddress());
                        dbPacket.setDestinationAddress(packet.getDestinationAddress());
                        dbPacket.setPayloadSize(packet.getPayloadSize());
                        dbPacket.setPayloadContents(packet.getPayload());
                        db.dbPacketDao().insert(dbPacket);

                        if (packet.isUDP()) {
                            deviceToNetworkUDPQueue.offer(packet);
                        } else if (packet.isTCP()) {
                            deviceToNetworkTCPQueue.offer(packet);
                        } else {
                            Log.w(TAG, "Unknown packet type");
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }

                    ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                    if (bufferFromNetwork != null) {
                        bufferFromNetwork.flip();
                        while (bufferFromNetwork.hasRemaining())
                            vpnOutput.write(bufferFromNetwork);
                        dataReceived = true;

                        ByteBufferPool.release(bufferFromNetwork);
                    } else {
                        dataReceived = false;
                    }

                    if (!dataSent && !dataReceived)
                        Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "Stopping");
            } catch (IOException e) {
                Log.w(TAG, e.toString(), e);
            } finally {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }
}
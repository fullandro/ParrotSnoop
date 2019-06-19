package divesttrump.parrotsnoop;


import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;


class TCB {

    String ipAndPort;
    long mySequenceNum;
    long theirSequenceNum;
    long myAcknowledgementNum, theirAcknowledgementNum;
    TCBStatus status;

    public enum TCBStatus {
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,
    }

    Packet referencePacket;

    SocketChannel channel;
    boolean waitingForNetworkData;
    SelectionKey selectionKey;

    private static final int MAX_CACHE_SIZE = 50;

    private static final LRUCache<String, TCB> tcbCache = new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, TCB>() {
        @Override
        public void cleanup(Map.Entry<String, TCB> eldest) {
            eldest.getValue().closeChannel();
        }
    });

    static TCB getTCB(String ipAndPort) {
        synchronized (tcbCache) {
            return tcbCache.get(ipAndPort);
        }
    }

    static void putTCB(String ipAndPort, TCB tcb) {
        synchronized (tcbCache) {
            tcbCache.put(ipAndPort, tcb);
        }
    }

    TCB(String ipAndPort, long mySequenceNum, long theirSequenceNum, long myAcknowledgementNum, long theirAcknowledgementNum, SocketChannel channel, Packet referencePacket) {
        this.ipAndPort = ipAndPort;

        this.mySequenceNum = mySequenceNum;
        this.theirSequenceNum = theirSequenceNum;
        this.myAcknowledgementNum = myAcknowledgementNum;
        this.theirAcknowledgementNum = theirAcknowledgementNum;

        this.channel = channel;
        this.referencePacket = referencePacket;
    }

    static void closeTCB(TCB tcb) {
        tcb.closeChannel();
        synchronized (tcbCache) {
            tcbCache.remove(tcb.ipAndPort);
        }
    }

    static void closeAll() {
        synchronized (tcbCache) {
            Iterator<Map.Entry<String, TCB>> it = tcbCache.entrySet().iterator();
            while (it.hasNext()) {
                it.next().getValue().closeChannel();
                it.remove();
            }
        }
    }

    private void closeChannel() {
        try {
            channel.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
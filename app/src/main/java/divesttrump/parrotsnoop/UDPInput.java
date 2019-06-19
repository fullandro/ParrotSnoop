package divesttrump.parrotsnoop;


import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


public class UDPInput implements Runnable {

    private static final String TAG = UDPInput.class.getSimpleName();
    private static final int HEADER_SIZE_V4 = Packet.IP4_HEADER_SIZE + Packet.UDP_HEADER_SIZE;
    private static final int HEADER_SIZE_V6 = Packet.IP6_HEADER_SIZE + Packet.UDP_HEADER_SIZE;

    private Selector selector;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;

    UDPInput(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector) {
        this.outputQueue = outputQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "Started");
            while (!Thread.interrupted()) {
                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext() && !Thread.interrupted()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid() && key.isReadable()) {
                        keyIterator.remove();

                        ByteBuffer receiveBuffer = ByteBufferPool.acquire();

                        int headerSize = HEADER_SIZE_V4;
                        int ipVersion = (receiveBuffer.duplicate().get() >> 4);
                        if (ipVersion == 6) headerSize = HEADER_SIZE_V6;

                        receiveBuffer.position(headerSize);

                        DatagramChannel inputChannel = (DatagramChannel) key.channel();

                        int readBytes = inputChannel.read(receiveBuffer);

                        Packet referencePacket = (Packet) key.attachment();
                        referencePacket.updateUDPBuffer(receiveBuffer, readBytes);
                        receiveBuffer.position(headerSize + readBytes);

                        outputQueue.offer(receiveBuffer);
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Stopping");
        } catch (IOException e) {
            Log.w(TAG, e.toString(), e);
        }
    }
}
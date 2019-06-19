package divesttrump.parrotsnoop;


import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;


class ByteBufferPool {

    private static final int BUFFER_SIZE = 16384;
    private static ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    static ByteBuffer acquire() {
        ByteBuffer buffer = pool.poll();
        if (buffer == null)
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        return buffer;
    }

    static void release(ByteBuffer buffer) {
        buffer.clear();
        pool.offer(buffer);
    }

    static void clear() {
        pool.clear();
    }
}
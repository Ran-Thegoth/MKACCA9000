package rs.fncore2.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

public class BufferFactory {
    public static final int RECORD_SIZE = 1024;
    public static final int DOCUMENT_SIZE = 32768;
    private static LinkedBlockingQueue<ByteBuffer> RECORD_BUFFERS = new LinkedBlockingQueue<>();
    private static LinkedBlockingQueue<ByteBuffer> DOCUMENT_BUFFERS = new LinkedBlockingQueue<>();

    public static ByteBuffer allocateRecord() {
        ByteBuffer result = RECORD_BUFFERS.poll();

        if (result == null) {
            result = ByteBuffer.allocate(RECORD_SIZE);
            result.order(ByteOrder.LITTLE_ENDIAN);
        }
        result.clear();
        return result;
    }

    public static ByteBuffer allocateDocument() {
        ByteBuffer result = DOCUMENT_BUFFERS.poll();

        if (result == null) {
            result = ByteBuffer.allocate(DOCUMENT_SIZE);
            result.order(ByteOrder.LITTLE_ENDIAN);
        }
        result.clear();
        return result;
    }

    public static void release(ByteBuffer buffer) {
        buffer.clear();
        if (buffer.capacity() == DOCUMENT_SIZE) {
            while (DOCUMENT_BUFFERS.size() > 9)
                DOCUMENT_BUFFERS.poll();
            DOCUMENT_BUFFERS.add(buffer);

        } else {
            while (RECORD_BUFFERS.size() > 9)
                RECORD_BUFFERS.poll();
            RECORD_BUFFERS.add(buffer);
        }
    }
}

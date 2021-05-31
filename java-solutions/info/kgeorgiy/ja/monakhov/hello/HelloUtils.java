package info.kgeorgiy.ja.monakhov.hello;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

public class HelloUtils {
    public final static long SELECT_TIMEOUT_MILLISECONDS = 100;

    private HelloUtils() {}

    public static DatagramChannel newDatagramChannel() throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        return channel;
//        return DatagramChannel.open();
    }

    public static int getBufferSize(final DatagramSocket socket) throws SocketException {
        return Math.max(socket.getReceiveBufferSize(), socket.getSendBufferSize());
    }

    public static byte[] getBytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String newString(final ByteBuffer buffer) {
        return new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
    }
}

package info.kgeorgiy.ja.monakhov.hello;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class HelloUtils {
    public HelloUtils() {}

    public static String getBody(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(final String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public static DatagramPacket newResponseDatagramPacket(final int bufferSize) {
        return new DatagramPacket(new byte[bufferSize], bufferSize);
    }

    public static DatagramPacket newRequestDatagramPacket(final String host, final int port, final int bufferSize) throws UnknownHostException {
        return new DatagramPacket(new byte[bufferSize], bufferSize, InetAddress.getByName(host), port);
    }

}


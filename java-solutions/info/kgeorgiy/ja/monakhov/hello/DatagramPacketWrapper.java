package info.kgeorgiy.ja.monakhov.hello;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class DatagramPacketWrapper {
    private final DatagramPacket packet;

    public DatagramPacketWrapper(final int bufferSize) {
        packet = new DatagramPacket(new byte[bufferSize], bufferSize);
    }

    public DatagramPacketWrapper(final String host, final int port, final int bufferSize) throws UnknownHostException {
        packet = new DatagramPacket(new byte[bufferSize], bufferSize, InetAddress.getByName(host), port);
    }

    public void setData(final byte[] buffer) {
        packet.setData(buffer);
    }

    public void setData(final String s) {
        packet.setData(s.getBytes(StandardCharsets.UTF_8));
    }

    public String getData() {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public DatagramPacket getPacket() {
        return packet;
    }
}

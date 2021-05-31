package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class HelloUtils {
    public final static long SELECT_TIMEOUT_MILLISECONDS = 100;

    private HelloUtils() {}

    public static DatagramChannel newDatagramChannel() throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        return channel;
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

    public static void serverMain(final HelloServer server, final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("You must pass 2 arguments: port threads");
        }
        final int port, threads;
        try {
            port = parsePort(args[0]);
            threads = Integer.parseInt(args[1]);
            server.start(port, threads);

        } catch (final NumberFormatException e) {
            System.err.println(e.getMessage());
        }
    }

    private static int parsePort(final String s) {
        final int port = Integer.parseInt(s);
        if (port < 0 || port > 65535) {
            throw new NumberFormatException("Port must be between 0 and 65535");
        }
        return port;
    }



    public static void clientMain(final HelloClient client, final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("You must pass 5 arguments: host port prefix threads requests");
            return;
        }
        final String host, prefix;
        final int port, threads, requests;
        try {
            host = args[0];
            port = parsePort(args[1]);
            prefix = args[2];
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);

            client.run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println(e.getMessage());
        }
    }
}

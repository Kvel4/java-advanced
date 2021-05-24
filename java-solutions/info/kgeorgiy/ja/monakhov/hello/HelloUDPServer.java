package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private final List<PortHandler> activeHandlers = new ArrayList<>();

    @Override
    public void start(final int port, final int threads) {
        try {
            activeHandlers.add(new PortHandler(port, threads));
        } catch (final SocketException e) {
            System.err.println("Unable to start socket on provided port: " + e.getMessage());
        }
    }

    public static void main(final String[] args) throws IllegalArgumentException {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("You must pass 2 arguments: port threads");
        }
        new HelloUDPServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    @Override
    public void close() {
        activeHandlers.forEach(PortHandler::close);
    }

    private static class PortHandler implements AutoCloseable {
        private final ExecutorService executor;

        private final DatagramSocket socket;

        private final int bufferSize;

        public PortHandler(final int port, final int threads) throws SocketException {
            executor = Executors.newFixedThreadPool(threads);

            socket = new DatagramSocket(port);
            bufferSize = socket.getReceiveBufferSize();

            for (int i = 0; i < threads; i++) {
                executor.submit(this::listen);
            }
        }

        private void listen() {

            while (!socket.isClosed() && !Thread.interrupted()) {
                // :NOTE: Переиспользование
                final DatagramPacketWrapper packet = new DatagramPacketWrapper(bufferSize);
                try {
                    socket.receive(packet.getPacket());
                    writeResponse(packet);
                    socket.send(packet.getPacket());
                } catch (final IOException ignored) { }
            }
        }

        private void writeResponse(final DatagramPacketWrapper packet) {
            final String response = "Hello, " + packet.getData();
            packet.setData(response);
        }

        @Override
        public void close(){
            socket.close();
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService isn't shutdown");
                }
            } catch (final InterruptedException ignored) { }
        }
    }
}

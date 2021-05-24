package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        new Sender(host, port, prefix, threads, requests).run();
    }

    public static void main(final String[] args) throws IllegalArgumentException {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("You must pass 5 arguments: host port prefix threads requests");
        }
        new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }

    private static class Sender {
        private final ExecutorService executor;

        private final String prefix;
        private final String host;
        private final int port;
        private final int threads;
        private final int requests;

        public Sender(final String host, final int port, final String prefix, final int threads, final int requests) {
            executor = Executors.newFixedThreadPool(threads);
            this.prefix = prefix;
            this.host = host;
            this.port = port;
            this.threads = threads;
            this.requests = requests;
        }

        public void run() {
            // :NOTE: IntStream
            IntStream.range(0, threads).forEach(i -> executor.submit(() -> sendAndReceive(i)));


            // :NOTE: Не дождались
            executor.shutdown();
            try {
                while (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS));
            } catch (final InterruptedException e) {
                executor.shutdownNow();
                System.err.println("Execution was interrupted: " + e.getMessage());
            }
        }


        private void sendAndReceive(final int i) {
            try (final DatagramSocket socket = new DatagramSocket()) {
                final int receiveBuffer = socket.getReceiveBufferSize();
                final int sendBuffer = socket.getSendBufferSize();
                socket.setSoTimeout(100);

                try {
                    // :NOTE: Новые?
                    final DatagramPacketWrapper packet = new DatagramPacketWrapper(host, port, sendBuffer);
                    for (int j = 0; j < requests; j++) {
                        final String request = generateRequest(i, j);
                        while (!socket.isClosed()) {
                            if (Thread.interrupted()) { return; }
                            try {
                                packet.setData(request);
                                socket.send(packet.getPacket());
                                packet.setData(new byte[receiveBuffer]);
                                socket.receive(packet.getPacket());

                                final String response = packet.getData();
                                if (isValid(request, response)) {
                                    System.out.println(response);
                                    break;
                                }
                                System.err.println("Incorrect response. Resending");
                            } catch (final IOException e) {
                                System.err.println(e.getMessage() + ". Resending");
                            }
                        }
                    }
                } catch (final UnknownHostException e) {
                    System.err.println("Unable to find host to make a request: " + e.getMessage());
                }
            } catch (final SocketException e) {
                System.err.println("Unable to create socket: " + e.getMessage());
            }
        }

        private static boolean isValid(final String requestBody, final String responseBody) {
            return responseBody.contains(requestBody);
        }

        private String generateRequest(final int thread, final int request) {
            return prefix + thread + "_" + request;
        }
    }
}


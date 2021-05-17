package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            new Sender(host, port, prefix, threads, requests).run();
        } catch (final InterruptedException e) {
            System.err.println("Execution was interrupted: " + e.getMessage());
        }
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

        public void run() throws InterruptedException {
            // :NOTE: IntStream
            for (int i = 0; i < threads; i++) {
                final int finalI = i;
                executor.submit(() -> sendAndReceive(finalI));
            }

            // :NOTE: Не дождались
            executor.shutdown();
            if (!executor.awaitTermination((long) threads * requests, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                System.err.println("Resend time exceeded. Unable to process all requests");
            }
        }


        private void sendAndReceive(final int i) {
            try (final DatagramSocket socket = new DatagramSocket()) {
                final int bufferSize = socket.getReceiveBufferSize();
                socket.setSoTimeout(100);

                try {
                    for (int j = 0; j < requests; j++) {
                        // :NOTE: Новые?
                        final DatagramPacket request = HelloUtils.newRequestDatagramPacket(host, port, bufferSize);
                        final DatagramPacket response = HelloUtils.newResponseDatagramPacket(bufferSize);
                        request.setData(generateBody(i, j));
                        final String requestBody = HelloUtils.getBody(request);
                        while (!socket.isClosed()) {
                            if (Thread.interrupted()) {
                                socket.close();
                                return;
                            }
                            try {
                                socket.send(request);
                                socket.receive(response);
                                final String responseBody = HelloUtils.getBody(response);
                                if (isValid(requestBody, responseBody)) {
                                    System.out.println(responseBody);
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

        private byte[] generateBody(final int thread, final int request) {
            return HelloUtils.toBytes(prefix + thread + "_" + request);
        }
    }
}


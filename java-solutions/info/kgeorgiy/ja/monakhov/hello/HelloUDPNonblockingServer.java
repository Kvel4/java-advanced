package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HelloUDPNonblockingServer implements HelloServer {
    private final List<PortHandler> activeHandlers = new ArrayList<>();

    public static void main(final String[] args) throws IllegalArgumentException {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("You must pass 2 arguments: port threads");
        }
        new HelloUDPNonblockingServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            activeHandlers.add(new PortHandler(port, threads));
        } catch (final SocketException e) {
            System.err.println("Unable to start socket on provided port: " + e.getMessage());
        } catch (final IOException e) {
            System.err.println("Unable to open selector");
        }
    }

    @Override
    public void close() {
        activeHandlers.forEach(PortHandler::close);
    }

    private static class PortHandler implements AutoCloseable {
        private final static int BUFFER_SIZE = Math.max(SocketOptions.SO_SNDBUF, SocketOptions.SO_RCVBUF);
        private final static long SELECT_TIMEOUT_MILLISECONDS = 100;
        private final int QUEUE_CAPACITY;

        private final ExecutorService listener;
        private final ExecutorService executor;

        private final SocketAddress address;

        private final Selector selector;

        private final BufferManager bufferManager;

        public PortHandler(final int port, final int threads) throws IOException {
            QUEUE_CAPACITY = threads;

            listener = Executors.newSingleThreadExecutor();
            executor = Executors.newFixedThreadPool(threads);

            address = new InetSocketAddress(port);
            selector = Selector.open();
            bufferManager = new BufferManager();

            listener.submit(this::listen);
        }

        private void listen() {
            try (final DatagramChannel channel = DatagramChannel.open()) {
                channel.configureBlocking(false);
                channel.bind(address);
                channel.register(selector, SelectionKey.OP_READ);

                while (channel.isOpen() && !Thread.interrupted()) {
                    selector.select(key -> {
                        try {
                            if (key.isReadable()) {
                                final ByteBuffer buffer = bufferManager.getReceiveBuffer();
                                final SocketAddress address = channel.receive(buffer);

                                executor.submit(() -> process(key, buffer, address));
                            }
                            if (key.isWritable()) {
                                final ResponseBuffer response = bufferManager.getResponseBuffer();
                                final ByteBuffer buffer = response.getBuffer();

                                channel.send(buffer, response.getAddress());
                                bufferManager.addReceiveBuffer(buffer);
                                key.interestOpsOr(SelectionKey.OP_READ);
                            }
                            if (bufferManager.isReceiveBufferEmpty()) {
                                key.interestOpsAnd(~SelectionKey.OP_READ);
                            }
                            synchronized (key) {
                                if (bufferManager.isResponseBufferEmpty()) {
                                    key.interestOpsAnd(~SelectionKey.OP_WRITE);
                                }
                            }
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, SELECT_TIMEOUT_MILLISECONDS);
                }
            } catch (final IOException e) {
                System.err.println("Unable to open datagram channel:" + e.getMessage());
            } catch (final UncheckedIOException e) {
                System.err.println("Some errors occurred while reading/writing to datagram channel: " + e.getMessage());
            }
        }

        private void process(final SelectionKey key, final ByteBuffer buffer, final SocketAddress address) {
            buffer.flip();
            final String response = "Hello, " + HelloUtils.newString(buffer);
            buffer.clear().put(HelloUtils.getBytes(response)).flip();
            bufferManager.addResponseBuffer(buffer, address);
            synchronized (key) {
                key.interestOpsOr(SelectionKey.OP_WRITE);
            }
        }

        @Override
        public void close() {
            try {
                selector.close();
            } catch (final IOException e) {
                System.err.println("Unable to close selector" + e.getMessage());
            }
            listener.shutdownNow();
            try {
                if (!listener.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Port listener isn't shutdown");
                }
            } catch (final InterruptedException ignored) {
            }
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("Executor isn't shutdown");
                }
            } catch (final InterruptedException ignored) {
            }
        }

        private class BufferManager {
            private final ArrayBlockingQueue<ResponseBuffer> responseBuffers = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
            private final ArrayBlockingQueue<ByteBuffer> receiveBuffers = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

            public BufferManager() {
                for (int i = 0; i < QUEUE_CAPACITY; i++) {
                    addReceiveBuffer(ByteBuffer.allocate(BUFFER_SIZE));
                }
            }

            public void addReceiveBuffer(final ByteBuffer buffer) {
                try {
                    receiveBuffers.add(buffer.clear());
                } catch (final IllegalStateException ignored) {
                }
            }

            public void addResponseBuffer(final ByteBuffer buffer, final SocketAddress address) {
                try {
                    responseBuffers.add(new ResponseBuffer(buffer, address));
                } catch (final IllegalStateException ignored) {
                }
            }

            public ByteBuffer getReceiveBuffer() {
                try {
                    return receiveBuffers.poll(1, TimeUnit.NANOSECONDS);
                } catch (final InterruptedException ignored) { }
                return null;
            }

            public ResponseBuffer getResponseBuffer() {
                try {
                    return responseBuffers.poll(1, TimeUnit.NANOSECONDS);
                } catch (final InterruptedException ignored) { }
                return null;
            }

            public boolean isReceiveBufferEmpty() {
                return receiveBuffers.size() == 0;
            }

            public boolean isResponseBufferEmpty() {
                return responseBuffers.size() == 0;
            }
        }

        private static class ResponseBuffer {
            private final ByteBuffer buffer;
            private final SocketAddress address;

            public ResponseBuffer(final ByteBuffer buffer, final SocketAddress address) {
                this.buffer = buffer;
                this.address = address;
            }

            public ByteBuffer getBuffer() {
                return buffer;
            }

            public SocketAddress getAddress() {
                return address;
            }
        }
    }
}

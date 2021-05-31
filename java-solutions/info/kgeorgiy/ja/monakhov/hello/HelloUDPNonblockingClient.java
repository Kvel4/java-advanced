package info.kgeorgiy.ja.monakhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class HelloUDPNonblockingClient implements HelloClient {
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            new Sender(host, port, prefix, threads, requests).run();
        } catch (final IOException e) {
            System.err.println("Unable to open selector: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        HelloUtils.clientMain(new HelloUDPNonblockingClient(), args);
    }

    private static class Sender {
        private final Selector selector;

        private final SocketAddress address;

        private final String prefix;
        private final int threads;
        private final int requests;

        public Sender(final String host, final int port, final String prefix, final int threads, final int requests) throws IOException {
            selector = Selector.open();
            address = new InetSocketAddress(host, port);

            this.prefix = prefix;
            this.threads = threads;
            this.requests = requests;
        }

        public void run() {
            try {
                IntStream.range(0, threads).forEach(i -> {
                    try {
                        final DatagramChannel channel = HelloUtils.newDatagramChannel();
                        channel.connect(address);
                        channel.register(selector, SelectionKey.OP_WRITE, new Context(channel, prefix, i));
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (final UncheckedIOException e) {
                System.err.println("Unable to open and configure datagram channels: " + e.getMessage());
                return;
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                final AtomicBoolean isEmpty = new AtomicBoolean(true);
                try {
                    selector.select(key -> {
                        // :NOTE: Сразу создать false? И она вообще не меняется нигде, зачем она?
                        isEmpty.set(false);
                        try {
                            final Context context = (Context) key.attachment();
                            if (key.isReadable()) {
                                read(key, context);
                                if (context.getRequestNumber() == requests) {
                                    key.channel().close();
                                    return;
                                }
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                            if (key.isWritable()) {
                                write(key, context);
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }, HelloUtils.SELECT_TIMEOUT_MILLISECONDS);
                } catch (final IOException e) {
                    System.err.println("IO exception occurred while selection: " + e.getMessage());
                    return;
                } catch (final UncheckedIOException e) {
                    System.err.println("IO exception occurred while read/write to the channel: " + e.getMessage());
                    return;
                }

                if (isEmpty.get()) {
                    selector.keys().forEach(key -> {
                        if (isReadInterest(key)) {
                            try {
                                write(key, (Context) key.attachment());
                            } catch (final IOException ignored) {
                            }
                        }
                    });
                }
            }
        }

        private void read(final SelectionKey key, final Context context) throws IOException {
            final ByteBuffer buffer = context.getClearBuffer();
            final DatagramChannel channel = (DatagramChannel) key.channel();

            channel.read(buffer);
            buffer.flip();
            final String request = context.getRequestBody();
            final String response = HelloUtils.newString(buffer);

            if (isValid(request, response)) {
                System.out.println(response);
                context.incrementRequestNumber();
            }
        }

        private void write(final SelectionKey key, final Context context) throws IOException {
            final ByteBuffer buffer = context.getClearBuffer();
            final DatagramChannel channel = (DatagramChannel) key.channel();

            buffer.put(HelloUtils.getBytes(context.getRequestBody())).flip();
            channel.write(buffer);
            buffer.rewind();
        }

        private static boolean isValid(final String requestBody, final String responseBody) {
            return responseBody.contains(requestBody);
        }

        private static boolean isReadInterest(final SelectionKey key) {
            return (key.interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ;
        }

        private static class Context {
            private final ByteBuffer buffer;
            private final String prefix;
            private final int thread;
            private int requestNumber;

            private Context(final DatagramChannel channel, final String prefix, final int thread) throws SocketException {
                this.prefix = prefix;
                this.thread = thread;
                this.requestNumber = 0;
                buffer = ByteBuffer.allocate(HelloUtils.getBufferSize(channel.socket()));
            }

            public ByteBuffer getBuffer() {
                return buffer;
            }

            public ByteBuffer getClearBuffer() {
                return buffer.clear();
            }

            public String getRequestBody() {
                return prefix + thread + "_" + requestNumber;
            }

            public int getRequestNumber() {
                return requestNumber;
            }

            public void incrementRequestNumber() {
                requestNumber++;
            }
        }
    }
}

package info.kgeorgiy.ja.monakhov.concurrent;

import java.util.*;
import java.util.function.Function;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import info.kgeorgiy.ja.monakhov.concurrent.IterativeUtils.*;

import static info.kgeorgiy.ja.monakhov.concurrent.IterativeUtils.newHandler;

public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Thread> queue = new ArrayDeque<>();
    private final Thread extractor;
    private final int threadsNumber;

    public ParallelMapperImpl(final int threadsNumber) {
        this.threadsNumber = threadsNumber;

        extractor = new Thread(() -> {
            while (true) {
                final Thread handler;
                synchronized (queue) {
                    while (queue.size() == 0) {
                        try {
                            queue.wait();
                        } catch (final InterruptedException e) {
                            return;
                        }
                    }
                    handler = queue.poll();
                }
                handler.start();
                try {
                    handler.join();
                } catch (final InterruptedException e) {
                    handler.interrupt();
                    try {
                        handler.join();
                    } catch (final InterruptedException ignored) { }
                }
            }
        });
        extractor.start();
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final ResultWrapper<R> resultWrapper = new ResultWrapper<>(args.size());

        synchronized (queue) {
            queue.add(new Thread(newHandler(threadsNumber, f, args, resultWrapper)));
            queue.notify();
        }

        return resultWrapper.getResult();
    }

    @Override
    public synchronized void close() {
        extractor.interrupt();
    }
}

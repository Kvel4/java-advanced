package info.kgeorgiy.ja.monakhov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;


public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private static final int MAX_SIZE = 1_000_000;
    private final Thread[] threads;

    public ParallelMapperImpl(final int threadsNumber) {
        threads = new Thread[threadsNumber];

        for (int i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(() -> {
                Runnable task;
                while (!Thread.interrupted()) {
                    synchronized (queue) {
                        try {
                            while (queue.isEmpty()) queue.wait();
                        } catch (final InterruptedException e) {
                            return;
                        }
                        task = queue.poll();
                        queue.notifyAll();
                    }
                    task.run();
                }
            });
            threads[i].start();
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final ResultWrapper<R> resultWrapper = new ResultWrapper<>(args.size());

        // :NOTE: too wide of a synchronize
        synchronized (queue) {
            for (int i = 0; i < args.size(); i++) {
                while (queue.size() == MAX_SIZE) {
                    queue.wait();
                }
                final int finalI = i;
                queue.add(() -> resultWrapper.set(finalI, f.apply(args.get(finalI))));
                queue.notifyAll();
            }
        }

        return resultWrapper.getResult();
    }

    @Override
    public synchronized void close() {
        ThreadsUtils.waitForInterruption(threads);
    }

    public static class ResultWrapper<R> {
        private final List<R> result;
        private int cnt;

        public ResultWrapper(final int length) {
            this.result = new ArrayList<>(Collections.nCopies(length, null));
            cnt = 0;
        }

        public synchronized void set(final int i, final R el) {
            result.set(i, el);
            if (++cnt == result.size()) {
                notify();
            }
        }

        public synchronized List<R> getResult() throws InterruptedException {
            while (cnt != result.size()) {
                wait();
            }
            return result;
        }
    }
}

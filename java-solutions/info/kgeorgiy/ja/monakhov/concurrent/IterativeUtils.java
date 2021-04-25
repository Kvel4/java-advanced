package info.kgeorgiy.ja.monakhov.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class IterativeUtils {
    public static <T,R> Thread newHandlerThread(final Function<? super T, ? extends R> f,
                                                final int threadsNumber,
                                                final List<? extends T> args,
                                                final ResultWrapper<R> result) {
        return new Thread(() -> {
            final int size = args.size();
            final int activeThreads = Math.min(threadsNumber, size);
            final Thread[] threads = new Thread[threadsNumber];
            int batchSize = (size + (activeThreads - 1)) / activeThreads;

            if ((activeThreads - 1) * batchSize >= size) batchSize -= 1;

            for (int i = 0; i < activeThreads; i++) {
                final int from = batchSize * i;
                final int to = i == activeThreads - 1 ? args.size() : batchSize * (i + 1);

                threads[i] = new Thread(() -> {
                    for (int j = from; j < to; j++) {
                        if (Thread.interrupted()) return;
                        result.set(j, f.apply(args.get(j)));
                    }
                });
                threads[i].start();
            }

            try {
                for (int i = 0; i < activeThreads; i++) {
                    threads[i].join();
                }
            } catch (final InterruptedException e) {
                for (int i = 0; i < activeThreads; i++) {
                    threads[i].interrupt();
                }
            }
        });
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

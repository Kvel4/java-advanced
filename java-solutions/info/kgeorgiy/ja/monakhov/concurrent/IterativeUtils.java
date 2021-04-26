package info.kgeorgiy.ja.monakhov.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class IterativeUtils {
    public static <T,R> Runnable newHandler(final int threadsNumber,
                                            final Function<? super T, ? extends R> f,
                                            final List<? extends T> args,
                                            final ResultWrapper<R> result) {
        return () -> {
            final int size = args.size();
            final int activeThreads = Math.min(threadsNumber, size);
            final Thread[] threads = new Thread[activeThreads];
            final int batchSize = size / activeThreads;
            int remainder = size % activeThreads;

            int from = 0, to = 0;
            for (int i = 0; i < activeThreads; i++) {
                from = to;
                to = from + batchSize + (remainder-- > 0 ? 1 : 0);

                final int finalFrom = from;
                final int finalTo = to;
                threads[i] = new Thread(() -> {
                    for (int j = finalFrom; j < finalTo; j++) {
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
                for (int i = 0; i < activeThreads; i++) {
                    try {
                        threads[i].join();
                    } catch (final InterruptedException ignored) { }
                }
            }
        };
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

package info.kgeorgiy.ja.monakhov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T, R> R parallelOperation(final int threads,
                                       final List<? extends T> values,
                                       final Function<List<? extends T>, R> converter,
                                       final Function<List<? extends R>, R> collector)
            throws InterruptedException {
        final int size = values.size();
        final int activeThreads = Math.min(threads, size);
        final int batchSize = size / activeThreads;
        int remainder = size % activeThreads;

        int from = 0, to = 0;
        final List<List<? extends T>> args = new ArrayList<>();
        for (int i = 0; i < activeThreads; i++) {
            from = to;
            to = from + batchSize + (remainder-- > 0 ? 1 : 0);
            args.add(values.subList(from, to));
        }

        if (mapper == null) {
            final List<R> result = new ArrayList<>(Collections.nCopies(activeThreads, null));
            final Thread[] workers = new Thread[activeThreads];
            for (int i = 0; i < activeThreads; i++) {
                final int finalI = i;x
                workers[i] = new Thread(() -> result.set(finalI, converter.apply(args.get(finalI))));
                workers[i].start();
            }

            try {
                for (int i = 0; i < activeThreads; i++) {
                    workers[i].join();
                }
            } catch (final InterruptedException e) {
                ThreadsUtils.waitForInterruption(workers);
                throw e;
            }
            return collector.apply(result);
        } else {
            return collector.apply(mapper.map(converter, args));
        }
    }


    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) throw new NoSuchElementException("Values can't be empty");
        final Function<List<? extends T>, T> max = list -> Collections.max(list, comparator);
        return parallelOperation(threads, values, max, max);
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        if (values.isEmpty()) return true;
        return parallelOperation(threads, values,
                list -> list.stream().allMatch(predicate),
                list -> list.stream().allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        if (values.isEmpty()) return "";
        return parallelOperation(threads, values,
                list -> list.stream().map(Object::toString).collect(Collectors.joining("")),
                list -> String.join("", list));
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return listOperation(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return listOperation(threads, values, stream -> stream.map(f));
    }

    private <T, U> List<U> listOperation(final int threads, final List<? extends T> values, final Function<Stream<? extends T>, Stream<? extends U>> f) throws InterruptedException {
        if (values.isEmpty()) return new ArrayList<>();
        return parallelOperation(threads, values,
                list -> f.apply(list.stream()).collect(Collectors.toList()),
                list -> list.stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }
}

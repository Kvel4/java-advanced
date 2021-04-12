package info.kgeorgiy.ja.monakhov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class IterativeParallelism implements ListIP {
    public static void main(String[] args) throws InterruptedException {
        List<Integer> l = List.of(4, 10, 2, 1, -5, 2, 2, 9);
        IterativeParallelism ip = new IterativeParallelism();
        System.out.println(ip.maximum(4, l, Comparator.naturalOrder()));
    }

    private <T, R> R baseOperation(final int threads,
                                   final List<? extends T> values,
                                   final Function<List<? extends T>, R> converter,
                                   final Function<List<? extends R>, R> collector) throws InterruptedException {
        return collect(threads, convert(threads, values, converter), collector);
    }

    private <R> R collect(final int threads,
                          final List<? extends R> values,
                          final Function<? super List<? extends R>, R> collector) throws InterruptedException {
//        List<R> result = parallelOperation(threads, values, collector);
//        while (result.size() != 1) {
//            result = parallelOperation(threads, result, collector);
//        }
//        return result.get(0);
        return collector.apply(values);
    }

    private <T, R> List<R> convert(final int threads,
                                   final List<? extends T> values,
                                   final Function<List<? extends T>, R> converter)
            throws InterruptedException {
        return parallelOperation(threads, values, converter);
    }

    private <T, R> List<R> parallelOperation(final int threads,
                                             final List<? extends T> values,
                                             final Function<List<? extends T>, R> operation)
            throws InterruptedException {
        int size = values.size();
        int activeThreads = Math.min(threads, (size + 1) / 2);
        int batchSize = (size + (activeThreads - 1)) / activeThreads;

        Thread[] workers = new Thread[activeThreads];
        List<R> result = new ArrayList<>(Collections.nCopies(activeThreads, null));

        for (int i = 0; i < activeThreads; i++) {
            final int i1 = i;
            workers[i] = new Thread(() -> {
                final int from = batchSize * i1;
                final int to = i1 == activeThreads - 1 ? values.size() : batchSize * (i1 + 1);
                result.set(i1, operation.apply(values.subList(from, to)));
            });
            workers[i].start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        return result;
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return baseOperation(threads, values,
                list -> Collections.max(list, comparator),
                list -> Collections.max(list, comparator));
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return baseOperation(threads, values,
                list -> Collections.min(list, comparator),
                list -> Collections.min(list, comparator));
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return baseOperation(threads, values,
                list -> list.stream().allMatch(predicate),
                list -> list.stream().allMatch(el -> el == true));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return baseOperation(threads, values,
                list -> list.stream().anyMatch(predicate),
                list -> list.stream().anyMatch(el -> el == true));
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return baseOperation(threads, values,
                list -> list.stream().map(Object::toString).collect(Collectors.joining("")),
                list -> String.join("", list));
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return baseOperation(threads, values,
                list -> list.stream().filter(predicate).collect(Collectors.toList()),
                list -> list.stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return baseOperation(threads, values,
                list -> list.stream().map(f).collect(Collectors.toList()),
                list -> list.stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }
}

package info.kgeorgiy.ja.monakhov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawler implements Crawler {
    private final int perHost;

    private final ExecutorService downloaderService;
    private final ExecutorService extractorService;

    private final Downloader downloader;

    private final ConcurrentMap<String, HostDownloader> hosts = new ConcurrentHashMap<>();

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        downloaderService = Executors.newFixedThreadPool(downloaders);
        extractorService = Executors.newFixedThreadPool(extractors);
        this.downloader = downloader;
        this.perHost = perHost;
    }

    @Override
    public Result download(final String url, final int depth) {
        return new Task(url, depth).download();
    }

    @Override
    public void close() {
        downloaderService.shutdown();
        extractorService.shutdown();
        try {
            if (!downloaderService.awaitTermination(10, TimeUnit.SECONDS)){
                downloaderService.shutdownNow();
                if (!downloaderService.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("DownloaderService isn't shutdown");
                }
            }
            if (!extractorService.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("ExtractorService isn't shutdown");
            }

        } catch (final InterruptedException e) {
            downloaderService.shutdownNow();
            extractorService.shutdownNow();
        }
    }

    private static int getArgument(final String[] args, final int index) {
        return index < args.length ? Integer.parseInt(args[index]) : 10;
    }

    public static void main(final String[] args) {
        if (args == null || args.length < 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Invalid input format:" + System.lineSeparator() +
                    "1. You must pass from 2 to 5 arguments" + System.lineSeparator() +
                    "2. Arguments can't be null");
        } else {
            try {
                final String url = args[0];
                final int depth = getArgument(args, 1);

                try (final Crawler crawler = new WebCrawler(new CachingDownloader(), getArgument(args, 2),
                        getArgument(args, 3), getArgument(args, 4))) {
                    crawler.download(url, depth);
                }
            } catch (final NumberFormatException e) {
                System.err.println("Invalid input format: depth, downloaders, extractors and perHost must be integers.");
            } catch (final IOException e) {
                System.err.println("Error in caching downloader initialization occurred: " + e.getMessage());
            }
        }
    }

    private class HostDownloader {
        private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger counter = new AtomicInteger();

        public void add(final Runnable task) {
            if (counter.incrementAndGet() <= perHost) {
                downloaderService.submit(task);
            } else {
                queue.add(task);
            }
        }

        public void runNext() {
            final Runnable task = queue.poll();
            if (task != null) {
                downloaderService.submit(task);
            } else {
                counter.decrementAndGet();
            }
        }
    }

    private class Task {
        private final int maxDepth;

        private final List<String> downloaded = Collections.synchronizedList(new ArrayList<>());
        private final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();

        private final Set<String> used = Collections.newSetFromMap(new ConcurrentHashMap<>());
        private final Set<String> nextLevelUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());

        public Task(final String url, final int maxDepth) {
            this.maxDepth = maxDepth;
            nextLevelUrls.add(url);
        }

        public Result download() {
            for (int depth = 1; depth <= maxDepth && !nextLevelUrls.isEmpty(); depth++) {
                final List<String> currentLevelUrls = new CopyOnWriteArrayList<>(nextLevelUrls);
                final boolean extract = depth != maxDepth;

                used.addAll(currentLevelUrls);
                nextLevelUrls.clear();

                final CountDownLatch latch = new CountDownLatch(currentLevelUrls.size());
                currentLevelUrls.forEach(url -> {
                    try {
                        final HostDownloader hostDownloader = hosts.computeIfAbsent(URLUtils.getHost(url), key -> new HostDownloader());
                        hostDownloader.add(() -> download(url, latch, hostDownloader, extract));
                    } catch (final MalformedURLException e) {
                        errors.put(url, e);
                    }
                });
                try {
                    latch.await();
                } catch (final InterruptedException e) {
                    break;
                }
            }

            return new Result(downloaded, errors);
        }

        private void download(final String url, final CountDownLatch latch, final HostDownloader host, final boolean extract) {
            try {
                final Document document = downloader.download(url);
                downloaded.add(url);
                if (extract) {
                    extractorService.submit(() -> extract(document, latch));
                } else {
                    latch.countDown();
                }
            } catch (final IOException e) {
                errors.put(url, e);
                latch.countDown();
            } finally {
                host.runNext();
            }
        }

        private void extract(final Document document, final CountDownLatch latch) {
            try {
                document.extractLinks().forEach(
                        el -> { if (!used.contains(el)) nextLevelUrls.add(el); });
            } catch (final IOException ignored) {
            } finally {
                latch.countDown();
            }
        }
    }
}

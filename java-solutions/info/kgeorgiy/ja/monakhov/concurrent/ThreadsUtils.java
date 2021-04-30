package info.kgeorgiy.ja.monakhov.concurrent;

public class ThreadsUtils {
    // static Utils
    private ThreadsUtils() {}

    public static void waitForInterruption(final Thread[] threads) {
        for (final Thread thread : threads) {
            thread.interrupt();
        }
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (final InterruptedException ignored) { }
        }

    }
}

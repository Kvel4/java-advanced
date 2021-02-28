package info.kgeorgiy.ja.monakhov.walk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class WalkFileVisitor extends SimpleFileVisitor<Path> {
    private final BufferedWriter bufferedWriter;

    public  WalkFileVisitor(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final byte[] buff = new byte[4096];
        final long highBits = 0xFF00000000000000L;
        long hash = 0;

        try (final InputStream fileReader = Files.newInputStream(file)) {
            int cnt;
            while ((cnt = fileReader.read(buff)) != -1) {
                for (int i = 0; i < cnt; i++) {
                    hash = (hash << 8) + (buff[i] & 0xff);
                    final long high = hash & highBits;
                    if (high != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        } catch (final IOException e) {
            hash = 0;
        }

        write(hash, file);
        return FileVisitResult.CONTINUE;

    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(0, file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        try(var stream = Files.list(dir)) {
            if (!stream.iterator().hasNext()) {
                write(0, dir);
            }
        }
        return FileVisitResult.CONTINUE;
    }


    private void write(long hash, Path file) throws IOException {
        bufferedWriter.write(String.format("%016x", hash) + " " + file.toString());
        bufferedWriter.newLine();
    }
}

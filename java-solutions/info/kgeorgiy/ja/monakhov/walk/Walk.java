package info.kgeorgiy.ja.monakhov.walk;


import java.io.*;
import java.nio.file.*;

public class Walk {
    private final String inputFileName;
    private final String outputFileName;

    private Walk(final String inputFileName, final String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    private void walk() throws WalkException {
        final Path inputFilePath;
        try {
            inputFilePath = Path.of(inputFileName);
        } catch (final InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to input file", e);
        }
        // :NOTE: копипаста
        final Path outputFilePath;
        try {
            outputFilePath = Path.of(outputFileName);
        } catch (final InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to output file", e);
        }

        final Path outputDirectory = outputFilePath.getParent();
        if (outputDirectory != null) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (final IOException e) {
                throw new WalkException("Unable to create output directory", e);
            }
        }

        try (final BufferedReader inputFileReader = Files.newBufferedReader(inputFilePath);
             final BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFilePath)) {
            String fileName;
            while ((fileName = inputFileReader.readLine()) != null) {
                outputFileWriter.write(getHash(fileName) + " " + fileName);
                outputFileWriter.newLine();
            }
        } catch (final NoSuchFileException e) {
            throw new WalkException(getFileTitle(e) + " doesn't exist", e);
        } catch (final AccessDeniedException e) {
            throw new WalkException("You have no access to " + getFileTitle(e), e);
        } catch (final IOException e) {
            // :NOTE: Подробности
            throw new WalkException("Some errors occurred while processing input/output file" , e);
        }
    }

    private String getFileTitle(final FileSystemException e) {
        return e.getFile().equals(inputFileName) ? "input file" : "output file";
    }

    private String getHash(final String fileName) {
        final byte[] buff = new byte[4096];
        long hash = 0;
        final long highBits = 0xFF00000000000000L;

        try (final InputStream fileReader = Files.newInputStream(Path.of(fileName))) {
            int amn;
            while ((amn = fileReader.read(buff)) != -1) {
                for (int i = 0; i < amn; i++) {
                    hash = (hash << 8) + (buff[i] & 0xff);
                    final long high = hash & highBits;
                    if (high != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        } catch (final IOException | InvalidPathException e) {
            hash = 0;
        }
        return String.format("%016x", hash);
    }

    public static void main(final String[] args) {
        if (args == null) {
            System.err.println("You must pass arguments to program");
        } else if (args.length != 2) {
            System.err.println("Arguments size must be 2");
        } else if (args[0] == null || args[1] == null) {
            System.err.println("Arguments can't be null");
        } else {
            try {
                new Walk(args[0], args[1]).walk();
            } catch (final WalkException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
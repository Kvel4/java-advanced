package info.kgeorgiy.ja.monakhov.walk;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class Walk {
    private final String inputFileName;
    private final String outputFileName;

    private Walk(final String inputFileName, final String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
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

    private void walk() throws WalkException {
        final Path inputFilePath, outputFilePath;
        String fileTitle = "input file";
        try {
            inputFilePath = Path.of(inputFileName);
            fileTitle = "output file";
            outputFilePath = Path.of(outputFileName);
        } catch (final InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to " + fileTitle, e);
        }

        final Path outputDirectory = outputFilePath.getParent();
        if (outputDirectory != null) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException ignored) {
            }
        }


        fileTitle = "input file";
        try (final BufferedReader inputFileReader = Files.newBufferedReader(inputFilePath)) {
            try (final BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFilePath)) {
                String fileName;
                while ((fileName = inputFileReader.readLine()) != null) {
                    outputFileWriter.write(getHash(fileName) + " " + fileName);
                    outputFileWriter.newLine();
                }
            } catch (NoSuchFileException e) {
                throw new WalkException("Unable to create output file", e);
            } catch (IOException e) {
                fileTitle = "output file";
                throw e;
            }
        } catch (final NoSuchFileException e) {
            throw new WalkException("input file doesn't exist", e);
        } catch (final AccessDeniedException e) {
            throw new WalkException("You have no access to " + fileTitle, e);
        } catch (final IOException e) {
            throw new WalkException("Some errors occurred while processing " + fileTitle, e);
        }
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
}
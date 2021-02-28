package info.kgeorgiy.ja.monakhov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class RecursiveWalk {
    private final String inputFileName;
    private final String outputFileName;
    private String fileTitle;

    private RecursiveWalk(final String inputFileName, final String outputFileName) {
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
                new RecursiveWalk(args[0], args[1]).walk();
            } catch (final WalkException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void walk() throws WalkException {
        final Path inputFilePath, outputFilePath;

        setFileTitle("input file");
        try {
            inputFilePath = Path.of(inputFileName);
            setFileTitle("output file");
            outputFilePath = Path.of(outputFileName);
        } catch (final InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to " + fileTitle, e);
        }

        final Path outputDirectory = outputFilePath.getParent();
        if (outputDirectory != null) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (final IOException ignored) {
            }
        }

        try (final BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFilePath)) {
            setFileTitle("input file");
            try (final BufferedReader inputFileReader = Files.newBufferedReader(inputFilePath)) {
                String path;
                FileVisitor<Path> fileVisitor = new WalkFileVisitor(outputFileWriter);
                while ((path = inputFileReader.readLine()) != null) {
                    setFileTitle("output file");
                    try {
                        Files.walkFileTree(Path.of(path), fileVisitor);
                    } catch (InvalidPathException e) {
                        outputFileWriter.write(String.format("%016x", 0) + " " + path);
                        outputFileWriter.newLine();
                    }
                    setFileTitle("input file");
                }
            }
        } catch (final IOException e) {
            throw new WalkException("Some errors occurred while processing " + fileTitle + ": " + e.getMessage(), e);
        }
    }

    private void setFileTitle(String fileTitle) {
        this.fileTitle = fileTitle;
    }
}

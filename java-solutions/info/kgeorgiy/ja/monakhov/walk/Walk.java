package info.kgeorgiy.ja.monakhov.walk;


import java.io.*;
import java.nio.file.*;

public class Walk {
    private String inputFileName;
    private String outputFileName;

    private Walk(String inputFileName, String outputFileName) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
    }

    private void walk() throws WalkException {
        Path inputFilePath, outputFilePath;
        try {
            inputFilePath = Path.of(inputFileName);
        } catch (InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to input file", e);
        }
        try {
            outputFilePath = Path.of(outputFileName);
        } catch (InvalidPathException e) {
            throw new WalkException("Unsupported symbol in path to output file", e);
        }

        Path outputDirectory = outputFilePath.getParent();
        if (outputDirectory != null) {
            try {
                Files.createDirectories(outputDirectory);
            } catch (IOException e) {
                throw new WalkException("Unable to create output directory", e);
            }
        }

        try (BufferedReader inputFileReader = Files.newBufferedReader(inputFilePath);
             BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFilePath)) {
            String fileName;
            while ((fileName = inputFileReader.readLine()) != null) {
                outputFileWriter.write(getHash(fileName) + " " + fileName);
                outputFileWriter.newLine();
            }
        } catch (NoSuchFileException e) {
            throw new WalkException(getFileTitle(e) + " doesn't exist", e);
        } catch (AccessDeniedException e) {
            throw new WalkException("You have no access to " + getFileTitle(e), e);
        } catch (IOException e) {
            throw new WalkException("Some errors occurred while processing input/output file" , e);
        }
    }

    private String getFileTitle(FileSystemException e) {
        String fileName = e.getFile();
        if (fileName.equals(inputFileName)) return "input file";
        return "output file";
    }

    private String getHash(String fileName) {
        int buffSize = 4096;
        byte[] buff = new byte[buffSize];
        long hash = 0L;
        long highBits = 0xFF00000000000000L;

        try (InputStream fileReader = Files.newInputStream(Path.of(fileName))) {
            int amn;
            while ((amn = fileReader.read(buff)) != -1) {
                for (int i = 0; i < amn; i++) {
                    hash = (hash << 8) + (buff[i] & 0xff);
                    long high = hash & highBits;
                    if (high != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        } catch (IOException | InvalidPathException e) {
            hash = 0L;
        }
        return String.format("%016x", hash);
    }

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("You must pass arguments to program");
        } else if (args.length != 2) {
            System.err.println("Arguments size must be 2");
        } else if (args[0] == null || args[1] == null) {
            System.err.println("Arguments can't be null");
        } else {
            try {
                Walk walk = new Walk(args[0], args[1]);
                walk.walk();
            } catch (WalkException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
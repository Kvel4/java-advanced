package info.kgeorgiy.ja.monakhov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private final static String TAB = "    ";
    private final static String SPACE = " ";
    private final static String COMMA = ",";
    private final static String LINEBREAK = System.lineSeparator();
    private final static String TABLINE = LINEBREAK + TAB;
    private final static String JAVA = ".java";

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        // check token and mb path
        if (token.isInterface() && !Modifier.isPrivate(token.getModifiers())) {
            write(generateClass(token), getPath(token, root));
        } else {
            throw new ImplerException("Implemented token must be an interface");
        }
    }

    private Path getPath(Class<?> token, Path root) {
        String packageName = token.getPackageName().replace('.', File.separatorChar);
        Path dir = root.resolve(packageName);

        try {
            Files.createDirectories(dir);
        } catch (final IOException ignored) {
        }

        return dir.resolve(token.getSimpleName() + "Impl" + JAVA);
    }

    private void write(String data, Path file) throws ImplerException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            bufferedWriter.write(data);
        } catch (IOException e) {
            throw new ImplerException("Some errors occurred while writing in output file", e);
        }
    }

    private String generateClass(Class<?> token) {
        return String.format("package %s;" + LINEBREAK + "%s {" + TABLINE + "%s" + LINEBREAK + "}",
                generatePackage(token), generateHeader(token), generateClassBody(token));
    }

    private String generatePackage(Class<?> token) {
        return token.getPackageName();
    }

    private String generateImports(Class<?> token) {
        return generateSequence(token.getDeclaredClasses(), this::generateImport, LINEBREAK);
    }

    private String generateImport(Class<?> clazz) {
        return "import" + SPACE + clazz.getCanonicalName();
    }



    private String generateHeader(Class<?> token) {
        return String.format("public class %s implements %s", token.getSimpleName() + "Impl", token.getCanonicalName());
    }

    private String generateClassBody(Class<?> token) {
//        return generateFields(token) + TABLINE + generateMethods(token);
        return generateMethods((token));
    }

//    private String generateFields(Class<?> token) {
//        return generateSequence(token.getFields(), this::generateField, LINEBREAK + TAB);
//    }
//
//    private String generateField(Field field) {
//         can be static
//        return String.format("public %s %s", field.getType().getCanonicalName(), field.getName());
//    }
//
//    private String generateConstructors(Class<?> token) {
//        return null;
//    }
//
//    private String generateConstructors(Class<?> token) {
//        return null;
//    }

    private String generateMethods(Class<?> token) {
        return generateSequence(token.getMethods(), this::generateMethod, LINEBREAK + TAB);
    }

    private String generateMethod(Method method) {
        String parameters = generateSequence(method.getParameters(), this::getParameter, COMMA + SPACE);
        Class<?> returnType = method.getReturnType();
        // can be static
        return String.format("public %s %s(%s) { %s }", returnType.getCanonicalName(), method.getName(), parameters, generateMethodBody(returnType));
    }

    private <T> String generateSequence(T[] elements, Function<T, String> converter, String separator) {
        return Arrays.stream(elements).map(converter).collect(Collectors.joining(separator));
    }

    private String generateMethodBody(Class<?> clazz) {
        return String.format("return %s;", getDefaultValue(clazz));
    }

    private String getParameter(Parameter parameter) {
        return parameter.getType().getCanonicalName() + SPACE + parameter.getName();
    }

    private <T> String getDefaultValue(Class<T> clazz) {
        if (clazz == void.class) return "";
        if (clazz == char.class) return "'\u0000'";
        if (clazz == float.class) return "0.0f";
        return clazz.isPrimitive() ? String.valueOf(Array.get(Array.newInstance(clazz, 1), 0)) : null;
    }
}
package info.kgeorgiy.ja.monakhov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class providing api for implementing classes and interfaces
 * @author Monakhov Daniil (d.monakhov0@gmail.com)
 */
public class Implementor implements JarImpler {
    /**
     * {@link SimpleFileVisitor} for deleting files and directories
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Creates new instance of {@link Implementor}
     */
    public Implementor() {}

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        final ImplementorInstance instance = new ImplementorInstance(token);

        if (instance.isImplementableClass()) {
            write(instance.generateFile(), javaFilePath(token, root));
        } else {
            throw new ImplerException("Provided class/interface is not implementable");
        }
    }

    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path dir;
        try {
            dir = Files.createTempDirectory(Path.of("."), null);
        } catch (final IOException e) {
            throw new ImplerException("Unable to create temporary directory", e);
        }
        try {
            implement(token, dir);
            compile(token, javaFilePath(token, dir));
            writeJar(jarFile, classFilePath(token, dir), pathInsideJar(token));
        } finally {
            clean(dir);
        }
    }

    /**
     * Writes provided {@code classFile} into new jar.
     * @param jarFile {@link Path} where jar file will be created
     * @param classFile {@link Path} to classfile to write into jar
     * @param pathInsideJar {@link String} represents package structure inside jar
     * @throws ImplerException if it is impossible to write
     */
    private void writeJar(final Path jarFile, final Path classFile, final String pathInsideJar) throws ImplerException {
        try (final JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            try {
                jarOutputStream.putNextEntry(new ZipEntry(pathInsideJar));
                Files.copy(classFile, jarOutputStream);
            } catch (final IOException e) {
                throw new ImplerException("Some error occurred while writing in jar file", e);
            }
        } catch (final IOException e) {
            throw new ImplerException("Unable to create jar", e);
        }
    }

    /**
     * Write provided data into provided file
     * @param data {@link String} to write
     * @param file {@link Path} to file
     * @throws ImplerException if it is impossible to write
     */
    private static void write(final String data, final Path file) throws ImplerException {
        try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            try {
                bufferedWriter.write(data);
            } catch (final IOException e) {
                throw new ImplerException("Some errors occurred while writing in output file", e);
            }
        } catch (final IOException e) {
            throw new ImplerException("Unable to create output file", e);
        }
    }

    /**
     * Compile implementation file.
     * @param token implemented class to get dependencies from
     * @param file {@link Path} implementation file
     * @throws ImplerException if compilation failed
     */
    private static void compile(final Class<?> token, final Path file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        final String[] args;

        try {
            args = new String[]{
                    "-cp",
                    Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                    file.toString()
            };
        } catch (final URISyntaxException e) {
            throw new ImplerException("Unable to access dependencies directory", e);
        }

        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Generated class doesn't compile");
        }

    }

    /**
     * Recursively delete all files in directory and the directory itself
     * @param root {@link Path} to directory to clear
     * @throws ImplerException if such a directory doesn't exist
     */
    private static void clean(final Path root) throws ImplerException {
        try {
            if (Files.exists(root)) {
                Files.walkFileTree(root, DELETE_VISITOR);
            }
        } catch (final IOException e) {
            throw new ImplerException("Unable to delete tmp directory", e);
        }
    }

    /**
     * Creates directory with path equivalent {@code root}/{@link Class#getPackageName() package}
     * @param token {@link Class} token to get package of
     * @param root {@link Path} to root directory to create package directory in
     * @return {@link Path} to created directory. Path can not exist if you don't have enough rights
     */
    private static Path createPackageDirectory(final Class<?> token, final Path root) {
        final String packageName = token.getPackageName().replace(".", "/");
        final Path dir = root.resolve(packageName);

        try {
            Files.createDirectories(dir);
        } catch (final IOException ignored) {
        }

        return dir;
    }

    /**
     * Constructs {@link Path} to file in the following format: {@code root}/{@link Class#getPackageName() package}/{@code fileName}
     * @param token {@link Class} to get {@code package} and {@code fileName} from
     * @param root {@link Path} to the root directory
     * @param suffix {@link String} to define file extension
     * @return {@link Path} to file
     */
    private static Path filePath(final Class<?> token, final Path root, final String suffix) {
        return createPackageDirectory(token, root).resolve(token.getSimpleName() + "Impl" + suffix);
    }

    /**
     * Useful method to call {@link #filePath(Class, Path, String)} with ".java" suffix
     * @param token {@link Class} to get information for path building
     * @param root {@link Path} to the root directory
     * @return {@link Path} to file
     */
    private static Path javaFilePath(final Class<?> token, final Path root) {
        return filePath(token, root, ".java");
    }

    /**
     * Useful method to call {@link #filePath(Class, Path, String)} with ".class" suffix
     * @param token {@link Class} to get information for path building
     * @param root {@link Path} to the root directory
     * @return {@link Path} to file
     */
    private static Path classFilePath(final Class<?> token, final Path root) {
        return filePath(token, root, ".class");
    }

    /**
     * Provides {@link String string path} to compiled file with {@link File#separator} changed by "/"
     * @param token {@link Class} to get information for path building
     * @return {@link String} with path to compiled file
     */
    private static String pathInsideJar(final Class<?> token) {
        return classFilePath(token, Path.of("")).toString().replace(File.separator, "/");
    }

    /**
     * Transform {@code string} to unicode representation.
     * @param string {@link String} to transform
     * @return unicode {@code string}
     */
    private static String toUnicode(final String string) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char symbol = string.charAt(i);
            stringBuilder.append(symbol < 128 ? symbol : "\\u" + String.format("%04x", (int) symbol));
        }
        return stringBuilder.toString();
    }

    /**
     * Runs {@link Implementor} in two possible ways. If arguments are incorrect write a message into {@link System#out standart output}
     *  <br> 2 arguments: {@code className rootPath} - runs {@link #implement(Class, Path)}
     *  <br> 3 arguments: {@code -jar className jarPath} - runs {@link #implementJar(Class, Path)}
     * @param args arguments for running an application
     */
    public static void main(final String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Two or three arguments expected");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("All arguments must be non-null");
            return;
        }
        final JarImpler implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } else if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
                System.out.println("First argument to evaluate jar mode must be -jar");
            }
        } catch (final InvalidPathException e) {
            System.out.println("Incorrect path to root: " + e.getMessage());
        } catch (final ClassNotFoundException e) {
            System.out.println("Incorrect class name: " + e.getMessage());
        } catch (final ImplerException e) {
            System.out.println("Some error occurred while implementation: " + e.getMessage());
        }
    }

    /**
     * Class to implement one single provided token. Exists only for encapsulation and convenience purposes.
     */
    private static class ImplementorInstance {

        /**
         * Intended for generated classes.
         */
        private final static String TAB = "    ";
        /**
         * Space for generated classes.
         */
        private final static String SPACE = " ";
        /**
         * Comma for generated classes
         */
        private final static String COMMA = ",";
        /**
         * Line separator for generated classes
         */
        private final static String NEWLINE = System.lineSeparator();
        /**
         * Line separator with intend for generated classes
         */
        private final static String TABLINE = NEWLINE + TAB;
        /**
         * Double Line separator with intend for generated classes
         */
        private final static String TABLINE2 = TABLINE + TABLINE;
        /**
         * {@link Class class} to implement
         */
        private final Class<?> token;
        /**
         * {@link String name}  of the class containing {@code token} implementation
         */
        private final String className;


        /**
         * Creates new {@link ImplementorInstance} which encapsulates token implementation
         * @param token {@link Class class} to implement
         */
        public ImplementorInstance(final Class<?> token) {
            this.token = token;
            className = token.getSimpleName() + "Impl";
        }

        /**
         * Generates java code containing correct token implementation
         * @return {@link String} represents implemented file
         */
        private String generateFile() {
            return toUnicode(generatePackage() + generateClass());
        }

        /**
         * Generates package declaration based on token package
         * @return {@link String} represents package declaration
         */
        private String generatePackage() {
            final String packageName = token.getPackageName();
            return packageName.isEmpty() ? "" : String.format("package %s;%n%n", packageName);
        }

        /**
         * Generate java class implementing token
         * @return {@link String} with implementation
         */
        private String generateClass() {
            return String.format("%s {%n    %s}", generateClassHeader(), generateClassBody());
        }

        /**
         * Generates class header
         * <pre> {@code public class className implements/extends token} </pre>
         * @return {@link String} with class header
         */
        private String generateClassHeader() {
            final String inheritanceType = token.isInterface() ? "implements" : "extends";
            return String.format("public class %s %s %s", className, inheritanceType, token.getCanonicalName());
        }

        /**
         * Generates {@link Constructor constructors} and {@link Method methods} that need to be implemented for correct inheritance
         * @return {@link String} with {@link Constructor constructors} and {@link Method methods}
         */
        private String generateClassBody() {
            String constructors = generateConstructors();
            final String methods = generateMethods();
            if (!constructors.isEmpty() && !methods.isEmpty()) constructors += TABLINE2;
            return constructors + methods + NEWLINE;
        }

        /**
         * Generates implementation of {@link Constructor constructors} that need to be implemented.
         * @return {@link String} with constructors declaration separated by new line with tab intend
         */
        private String generateConstructors() {
            return generateSequence(getConstructors(), this::generateConstructor, TABLINE2);
        }

        /**
         * Provide all {@link Constructor constructors} that need to be implemented.
         * @return Array of {@link Constructor}
         */
        private Constructor<?>[] getConstructors() {
            return Arrays.stream(token.getDeclaredConstructors()).filter(this::isNotPrivate).toArray(Constructor[]::new);
        }

        /**
         * Generates {@code constructor} declaration
         * <pre> {@code
         * accessModifier className(parameters) throws exceptions {
         *      super(parameters);
         * }
         * } </pre>
         * @param constructor {@link Constructor} which our constructor will refer to
         * @return {@link String} with {@link Constructor} declaration
         */
        private String generateConstructor(final Constructor<?> constructor) {
            return String.format("%s%s(%s) %s{ %s}",
                    generateAccessModifier(constructor),
                    className,
                    generateParameters(constructor),
                    generateExceptions(constructor),
                    generateConstructorBody(constructor));
        }

        /**
         * Generates {@code constructor} body which refers to {@code super} constructor
         * @param constructor {@link Constructor} which our constructor will refer to
         * @return {@link String} with {@code super} reference
         */
        private String generateConstructorBody(final Constructor<?> constructor) {
            return String.format("super(%s); ",
                    generateSequence(constructor.getParameters(), Parameter::getName, COMMA + SPACE));
        }

        /**
         * Generates implementation of {@link Method methods} that need to be implemented.
         * @return {@link String} with methods declaration separated by new line with tab intend
         */
        private String generateMethods() {
            final boolean isAbstract = Modifier.isAbstract(token.getModifiers());
            return isAbstract ? generateSequence(getMethods(), MethodWrapper::toString, TABLINE2) : "";
        }

        /**
         * Provide all {@link Method methods} that need to be implemented. Methods wrapped in {@link MethodWrapper}
         * @return Array of {@link MethodWrapper}
         */
        private MethodWrapper[] getMethods() {
            final Set<MethodWrapper> methods = new HashSet<>();
            addMethods(methods, token.getMethods());

            Class<?> superToken = token;
            while (superToken != null) {
                addMethods(methods, superToken.getDeclaredMethods());
                superToken = superToken.getSuperclass();
            }
            return methods.stream().filter(methodWrapper -> isImplementableMethod(methodWrapper.getMethod())).toArray(MethodWrapper[]::new);
        }

        /**
         * Wraps {@code methods} into {@link MethodWrapper} and add them to provided {@code uniqueMethods} set
         * @param uniqueMethods {@link MethodWrapper} set of methods with unique signature
         * @param methods {@link Method methods} to add to set
         */
        private void addMethods(final Set<MethodWrapper> uniqueMethods, final Method[] methods) {
            Arrays.stream(methods).map(MethodWrapper::new).forEach(uniqueMethods::add);
        }

        /**
         * Generates {@code method} declaration.
         * <pre> {@code
         * accessModifier returnType methodName(parameters) throws exceptions {
         *      return defaultValue;
         * }
         * } </pre>
         * @param method {@link Method} to generate declaration of
         * @return {@link String} with {@code method} declaration
         */
        private String generateMethod(final Method method) {
            final Class<?> returnType = method.getReturnType();
            return String.format("%s%s %s(%s) %s{ %s}",
                    generateAccessModifier(method),
                    returnType.getCanonicalName(),
                    method.getName(),
                    generateParameters(method),
                    generateExceptions(method),
                    generateMethodBody(returnType));
        }

        /**
         * Generates method body which return default value
         * @param clazz {@link Class} represents methods return type
         * @return {@link String} with default value
         */
        private String generateMethodBody(final Class<?> clazz) {
            return String.format("return %s; ", generateDefaultValue(clazz));
        }

        /**
         * Generates access modifier of {@link Executable executable};
         * "public", "protected", "" stands for package-private
         * @param executable {@link Executable} to get access modifier from
         * @return {@link String} with access modifier
         */
        private String generateAccessModifier(final Executable executable) {
            final int modifiers = executable.getModifiers();
            if (Modifier.isPublic(modifiers)) return "public ";
            if (Modifier.isProtected(modifiers)) return "protected ";
            return "";
        }

        /**
         * Generates {@link Parameter parameters} accepted by {@link Executable executable}
         * @param executable {@link Executable} to generate parameters from
         * @return {@link String} of parameters joined with {@value COMMA} + {@value SPACE}
         */
        private String generateParameters(final Executable executable) {
            return generateSequence(executable.getParameters(), this::generateParameter, COMMA + SPACE);
        }

        /**
         * Generates {@link Parameter} declaration
         * @param parameter to create {@link String} from
         * @return {@link String} with parameter declaration
         */
        private String generateParameter(final Parameter parameter) {
            return parameter.getType().getCanonicalName() + SPACE + parameter.getName();
        }

        /**
         * Generates {@link Exception exceptions} thrown by {@code executable}
         * @param executable {@link Executable} to get {@link Exception exceptions} thrown by
         * @return {@link String} {@code "throws exceptions"}; or {@link String empty string} if executable doesnt throws anything
         */
        private String generateExceptions(final Executable executable) {
            final Class<?>[] exceptionTypes = executable.getExceptionTypes();

            return exceptionTypes.length == 0 ? "" :
                    "throws " + generateSequence(exceptionTypes, Class::getCanonicalName, COMMA + SPACE) + SPACE;
        }

        /** Takes given array of type {@code <T>} converts it with provided {@link Function function} {@code <T> ->} {@link String}
         *  {@code converter} and collect all elements separated by {@code separator}
         *
         * @param elements array of elements to convert
         * @param converter {@link Function function} from {@code <T>} to {@link String} to convert element
         * @param separator {@link String} to separate converted elements
         * @param <T> specify elements type to convert
         * @return {@link String string} of converted elements separated by {@code separator}
         */
        private <T> String generateSequence(final T[] elements, final Function<T, String> converter, final String separator) {
            return Arrays.stream(elements).map(converter).collect(Collectors.joining(separator));
        }

        /** Generates default value of given {@link Class class}.
         * @param clazz {@link Class class} to get default value of
         * @return {@code "null"} for all non primitive classes, {@code "false"} for {@code boolean},
         * {@code ""} for void and {@code "0"} for all remaining primitive classes
         */
        private String generateDefaultValue(final Class<?> clazz) {
            if (clazz == void.class) return "";
            if (clazz == boolean.class) return "false";
            return clazz.isPrimitive() ? "0" : null;
        }

        /**
         * Checks whether {@link Executable executable} have private access modifier
         * @param executable {@link Executable} to get access modifier
         * @return {@code true} if executable is private {@code false} otherwise
         */
        private boolean isNotPrivate(final Executable executable) {
            return !Modifier.isPrivate(executable.getModifiers());
        }

        /**
         * Checks whether {@link Method method} needs to be implemented
         * @param method {@link Method} to check
         * @return {@code true} if method needs to be implemented {@code false} otherwise
         */
        private boolean isImplementableMethod(final Method method) {
            final int modifiers = method.getModifiers();
            return Modifier.isAbstract(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers);
        }

        /**
         * Checks whether {@link Class token} can be implemented
         * @return {@code true} if token can be implemented {@code false} otherwise
         */
        private boolean isImplementableClass() {
            final int modifiers = token.getModifiers();
            return !Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers)
                    && !token.isPrimitive() && !token.isEnum() && token != Enum.class
                    && (token.isInterface() || isInstantiated());
        }

        /**
         * Checks whether {@link Class token} can be instantiated
         * @return {@code true} if token can be instantiated {@code false} otherwise
         */
        private boolean isInstantiated() {
            return getConstructors().length != 0;
        }

        /**
         * Wrapper class for {@link Method methods}. Stands for searching unique methods via
         * {@link MethodWrapper#hashCode()} and {@link MethodWrapper#equals(Object)}
         */
        private class MethodWrapper {
            /**
             * Wrapped method
             */
            private final Method method;
            /**
             * {@link String} with method implementation
             */
            private final String generated;

            /**
             * Instantiates a new Method wrapper.
             *
             * @param method the method
             */
            public MethodWrapper(final Method method) {
                this.method = method;
                this.generated = generateMethod(method);
            }

            /**
             * Gets method.
             *
             * @return the method
             */
            public Method getMethod() {
                return method;
            }

            @Override
            public String toString() {
                return generated;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                final MethodWrapper that = (MethodWrapper) o;
                final Method thatMethod = that.getMethod();
                return method.getName().equals(thatMethod.getName()) &&
                        Arrays.equals(method.getParameterTypes(), thatMethod.getParameterTypes());
            }

            @Override
            public int hashCode() {
                return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
            }
        }
    }
}
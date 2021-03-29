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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Implementor implements Impler {
    private final static String JAVA = ".java";

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        ImplementorInstance instance = new ImplementorInstance(token);

        if (instance.isImplementableClass()) {
            write(instance.generateFile(), generateFilePath(instance, root));
        } else {
            throw new ImplerException("Provided class/interface is not implementable");
        }
    }

    private static Path generateFilePath(ImplementorInstance instance, Path root) {
        String packageName = instance.getToken().getPackageName().replace('.', File.separatorChar);
        Path dir = root.resolve(packageName);

        try {
            Files.createDirectories(dir);
        } catch (final IOException ignored) {
        }

        return dir.resolve(instance.getClassName() + JAVA);
    }

    private static void write(String data, Path file) throws ImplerException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file)) {
            try {
                bufferedWriter.write(data);
            } catch (IOException e) {
                throw new ImplerException("Some errors occurred while writing in output file", e);
            }
        } catch (IOException e) {
            throw new ImplerException("Unable to create output file", e);
        }
    }

    private static class ImplementorInstance {
        private final static String TAB = "    ";
        private final static String SPACE = " ";
        private final static String COMMA = ",";
        private final static String NEWLINE = System.lineSeparator();
        private final static String TABLINE = NEWLINE + TAB;
        private final static String TABLINE2 = TABLINE + TABLINE;

        private final Class<?> token;
        private final String className;

        public ImplementorInstance(final Class<?> token) {
            this.token = token;
            className = token.getSimpleName() + "Impl";
        }

        private String generateFile() {
            return generatePackage() + generateClass();
        }

        private String generatePackage() {
            String packageName = token.getPackageName();
            return packageName.isEmpty() ? "" : String.format("package %s;%n%n", packageName);
        }

        private String generateClass() {
            return String.format("%s {%n    %s}", generateClassHeader(), generateClassBody());
        }

        private String generateClassHeader() {
            String inheritanceType = token.isInterface() ? "implements" : "extends";
            return String.format("public class %s %s %s", className, inheritanceType, token.getCanonicalName());
        }

        private String generateClassBody() {
            boolean isAbstract = Modifier.isAbstract(token.getModifiers());
            return generateConstructors() + (isAbstract ? generateMethods() : "") + NEWLINE;
        }

        private String generateConstructors() {
            return generateSequence(getConstructors(), this::generateConstructor, TABLINE2) + TABLINE;
        }

        private Constructor<?>[] getConstructors() {
            return Arrays.stream(token.getDeclaredConstructors()).filter(this::isNonPrivate).toArray(Constructor[]::new);
        }

        private String generateConstructor(Constructor<?> constructor) {
            return String.format("%s%s(%s) %s{ %s}",
                    generateAccessModifier(constructor),
                    className,
                    generateParameters(constructor),
                    generateExceptions(constructor),
                    generateConstructorBody(constructor));
        }

        private String generateConstructorBody(final Constructor<?> constructor) {
            return String.format("super(%s); ",
                    generateSequence(constructor.getParameters(), Parameter::getName, COMMA + SPACE));
        }

        private String generateMethods() {
            return generateSequence(getMethods(), MethodWrapper::toString, TABLINE2);
        }

        private MethodWrapper[] getMethods() {
            Set<MethodWrapper> methods = new HashSet<>();
            addMethods(methods, token.getMethods());

            Class<?> superToken = token;
            while (superToken != null) {
                addMethods(methods, superToken.getDeclaredMethods());
                superToken = superToken.getSuperclass();
            }
            return methods.stream().filter(methodWrapper -> isImplementableMethod(methodWrapper.getMethod())).toArray(MethodWrapper[]::new);
        }

        private void addMethods(Set<MethodWrapper> uniqueMethods, Method[] methods) {
            Arrays.stream(methods).map(MethodWrapper::new).forEach(uniqueMethods::add);
        }

        private String generateMethod(Method method) {
            Class<?> returnType = method.getReturnType();
            return String.format("%s%s %s(%s) %s{ %s}",
                    generateAccessModifier(method),
                    returnType.getCanonicalName(),
                    method.getName(),
                    generateParameters(method),
                    generateExceptions(method),
                    generateMethodBody(returnType));
        }

        private String generateMethodBody(Class<?> clazz) {
            return String.format("return %s; ", generateDefaultValue(clazz));
        }

        private String generateAccessModifier(Executable executable) {
            int modifiers = executable.getModifiers();
            if (Modifier.isPublic(modifiers)) return "public ";
            if (Modifier.isProtected(modifiers)) return "protected ";
            return "";
        }

        private String generateParameters(Executable executable) {
            return generateSequence(executable.getParameters(), this::getParameter, COMMA + SPACE);
        }

        private String getParameter(Parameter parameter) {
            return parameter.getType().getCanonicalName() + SPACE + parameter.getName();
        }

        private String generateExceptions(Executable executable) {
            Class<?>[] exceptionTypes = executable.getExceptionTypes();

            return exceptionTypes.length == 0 ? "" :
                    "throws " + generateSequence(exceptionTypes, Class::getCanonicalName, COMMA + SPACE) + SPACE;
        }

        private <T> String generateSequence(T[] elements, Function<T, String> converter, String separator) {
            return Arrays.stream(elements).map(converter).collect(Collectors.joining(separator));
        }

        private String generateDefaultValue(Class<?> token) {
            if (token == void.class) return "";
            if (token == char.class) return "'\u0000'";
            if (token == float.class) return "0.0f";
            return token.isPrimitive() ? String.valueOf(Array.get(Array.newInstance(token, 1), 0)) : null;
        }

        private boolean isNonPrivate(Executable executable) {
            return !Modifier.isPrivate(executable.getModifiers());
        }

        private boolean isImplementableMethod(Method method) {
            int modifiers = method.getModifiers();
            return Modifier.isAbstract(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers);
        }

        private boolean isImplementableClass() {
            int modifiers = token.getModifiers();
            return !Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers)
                    && !token.isPrimitive() && !token.isEnum() && token != Enum.class
                    && (token.isInterface() || isInstantiated());
        }

        private boolean isInstantiated() {
            return getConstructors().length != 0;
        }

        public Class<?> getToken() {
            return token;
        }

        public String getClassName() {
            return className;
        }

        private class MethodWrapper {
            private final Method method;
            private final String generated;

            public MethodWrapper(final Method method) {
                this.method = method;
                this.generated = generateMethod(method);
            }

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
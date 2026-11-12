package pbt.components.sut;

import pbt.descriptor.SutSpec;
import pbt.spi.sut.SutException;
import pbt.spi.sut.SutRunner;
import pbt.spi.sut.SutRunnerProvider;
import pbt.spi.type.Tuple;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Invokes a Java method (static or instance) via reflection.
 *
 * <p>The class and method are resolved once when the runner is built. The method must take
 * at least one parameter. If several public methods share the name, the parameter types in the
 * descriptor's {@code method} (for example {@code "sort(List)"} or {@code "repeat(String, int)"})
 * select one; a type may be a simple or fully qualified name, generics are ignored. Ambiguity
 * is an error. A method with several parameters is called with the values of a {@link Tuple} input.
 *
 * <p>When the spec lists a {@code searchPath}, the class is loaded from those directories and
 * jars by a classloader of its own, released by {@link SutRunner#close()}. Instance methods are
 * called on a fresh instance per call, so SUT state does not leak between inputs.
 */
public final class JavaSutRunnerProvider implements SutRunnerProvider {

    private static String parseMethodName(String method) {
        int paren = method.indexOf('(');
        return (paren < 0 ? method : method.substring(0, paren)).trim();
    }

    /**
     * The declared parameter types without generics, or null when the signature names none.
     */
    private static List<String> parseParameterTypes(String method) {
        int open = method.indexOf('(');
        int close = method.lastIndexOf(')');
        if (open < 0 || close < open) return null;
        String inner = method.substring(open + 1, close).trim();
        if (inner.isEmpty() || inner.equals("...")) return null;
        List<String> types = new ArrayList<>();
        int depth = 0;
        int partStart = 0;
        for (int i = 0; i <= inner.length(); i++) {
            char c = i < inner.length() ? inner.charAt(i) : ',';
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                String type = inner.substring(partStart, i).trim();
                int generics = type.indexOf('<');
                types.add((generics < 0 ? type : type.substring(0, generics)).trim());
                partStart = i + 1;
            }
        }
        return types;
    }

    private static boolean matches(Method m, List<String> parameterTypes) {
        Class<?>[] actual = m.getParameterTypes();
        if (actual.length != parameterTypes.size()) return false;
        for (int i = 0; i < actual.length; i++) {
            String wanted = parameterTypes.get(i);
            if (!actual[i].getSimpleName().equals(wanted) && !actual[i].getName().equals(wanted)) return false;
        }
        return true;
    }

    private static Method findMethod(Class<?> cls, String name, List<String> parameterTypes) {
        List<Method> candidates = Arrays.stream(cls.getMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() >= 1)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "no public method named '" + name + "' with parameters in " + cls.getName());
        }
        if (parameterTypes != null) {
            candidates = candidates.stream().filter(m -> matches(m, parameterTypes)).toList();
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("no public method '" + name + "("
                        + String.join(",", parameterTypes) + ")' in " + cls.getName());
            }
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("ambiguous method '" + name + "' in " + cls.getName()
                    + "; name the parameter types, e.g. \"" + name + "(List)\"");
        }
        return candidates.get(0);
    }

    /**
     * A {@link Tuple} is spread over the parameters; any other input is the single argument.
     */
    private static Object[] arguments(Method method, Object input) {
        Object[] args = input instanceof Tuple tuple ? tuple.values().toArray() : new Object[]{input};
        if (args.length != method.getParameterCount()) {
            throw new IllegalArgumentException(method.getName() + " takes " + method.getParameterCount()
                    + " argument(s) but the input provides " + args.length
                    + (input instanceof Tuple ? "" : "; use a 'tuple' generator for several parameters"));
        }
        return args;
    }

    private static URL[] urls(List<String> searchPath) {
        URL[] urls = new URL[searchPath.size()];
        for (int i = 0; i < urls.length; i++) {
            Path path = Path.of(searchPath.get(i)).toAbsolutePath();
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("SUT search path entry does not exist: " + path);
            }
            try {
                urls[i] = path.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("invalid SUT search path entry: " + path, e);
            }
        }
        return urls;
    }

    private static void closeQuietly(URLClassLoader loader) {
        if (loader == null) return;
        try {
            loader.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Loads classes from its own URLs before asking the parent, so two versions of a class with
     * the same name can coexist and a version on the project's classpath cannot shadow the SUT's.
     * JDK classes always come from the parent.
     */
    private static final class ChildFirstClassLoader extends URLClassLoader {

        ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null && !name.startsWith("java.")) {
                    try {
                        loaded = findClass(name);
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                if (loaded == null) {
                    return super.loadClass(name, resolve);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }
    }

    @Override
    public String name() {
        return "java";
    }

    @Override
    public SutRunner build(SutSpec spec) {
        String signature = spec.method();
        if (signature == null) throw new IllegalArgumentException("SutSpec.method must not be null");
        String methodName = parseMethodName(signature);
        List<String> parameterTypes = parseParameterTypes(signature);

        boolean isolated = spec.searchPath() != null && !spec.searchPath().isEmpty();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        URLClassLoader isolatedLoader = isolated ? new ChildFirstClassLoader(urls(spec.searchPath()), parent) : null;
        Class<?> cls;
        try {
            cls = Class.forName(spec.className(), true, isolated ? isolatedLoader : parent);
        } catch (ClassNotFoundException e) {
            closeQuietly(isolatedLoader);
            throw new IllegalArgumentException("SUT class not found: " + spec.className()
                    + (isolated ? " on search path " + spec.searchPath() : ""), e);
        }
        Method method;
        try {
            method = findMethod(cls, methodName, parameterTypes);
        } catch (IllegalArgumentException e) {
            closeQuietly(isolatedLoader);
            throw e;
        }
        method.setAccessible(true);
        Constructor<?> constructor = null;
        if (!spec.isStatic()) {
            try {
                constructor = cls.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                closeQuietly(isolatedLoader);
                throw new IllegalArgumentException(
                        "SUT class " + cls.getName() + " needs a no-arg constructor for an instance method", e);
            }
            constructor.setAccessible(true);
        }
        Constructor<?> instanceConstructor = constructor;
        return new SutRunner() {
            @Override
            public Object run(Object input) {
                try {
                    Object target = instanceConstructor == null ? null : instanceConstructor.newInstance();
                    return method.invoke(target, arguments(method, input));
                } catch (InvocationTargetException e) {
                    Throwable raised = e.getCause();
                    throw new SutException(raised.getClass().getName(), raised.getMessage(), raised);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("SUT invocation failed: " + cls.getName() + "#" + methodName, e);
                }
            }

            @Override
            public void close() {
                closeQuietly(isolatedLoader);
            }
        };
    }
}

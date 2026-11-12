package pbt.components.sut;

import pbt.descriptor.SutSpec;
import pbt.spi.sut.SutException;
import pbt.spi.sut.SutRunner;
import pbt.spi.sut.SutRunnerProvider;
import pbt.spi.type.Tuple;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * is an error. A method with several parameters is called with the values of a {@link Tuple} input. Instance
 * methods are called on a fresh instance per call, so SUT state does not leak between inputs.
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

        Class<?> cls;
        try {
            cls = Class.forName(spec.className(), true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("SUT class not found: " + spec.className(), e);
        }
        Method method = findMethod(cls, methodName, parameterTypes);
        method.setAccessible(true);
        Constructor<?> constructor = null;
        if (!spec.isStatic()) {
            try {
                constructor = cls.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                        "SUT class " + cls.getName() + " needs a no-arg constructor for an instance method", e);
            }
            constructor.setAccessible(true);
        }
        Constructor<?> instanceConstructor = constructor;
        return input -> {
            try {
                Object target = instanceConstructor == null ? null : instanceConstructor.newInstance();
                return method.invoke(target, arguments(method, input));
            } catch (InvocationTargetException e) {
                Throwable raised = e.getCause();
                throw new SutException(raised.getClass().getName(), raised.getMessage(), raised);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("SUT invocation failed: " + cls.getName() + "#" + methodName, e);
            }
        };
    }
}

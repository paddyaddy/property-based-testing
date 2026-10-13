package pbt.components.sut;

import pbt.descriptor.SutSpec;
import pbt.spi.sut.SutRunner;
import pbt.spi.sut.SutRunnerProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Invokes a Java method (static or instance) via reflection.
 *
 * <p>The class and method are resolved once when the runner is built. The method must take
 * exactly one parameter. If several public methods share the name, the parameter type in the
 * descriptor's {@code method} (for example {@code "sort(List)"}) selects one; the type may be
 * a simple or fully qualified name, generics are ignored. Ambiguity is an error. Instance
 * methods are called on a fresh instance per call, so SUT state does not leak between inputs.
 */
public final class JavaSutRunnerProvider implements SutRunnerProvider {

    private static String parseMethodName(String method) {
        int paren = method.indexOf('(');
        return (paren < 0 ? method : method.substring(0, paren)).trim();
    }

    /**
     * The declared parameter type without generics, or null when the signature names none.
     */
    private static String parseParameterType(String method) {
        int open = method.indexOf('(');
        int close = method.lastIndexOf(')');
        if (open < 0 || close < open) return null;
        String inner = method.substring(open + 1, close).trim();
        if (inner.isEmpty() || inner.equals("...")) return null;
        int generics = inner.indexOf('<');
        return (generics < 0 ? inner : inner.substring(0, generics)).trim();
    }

    private static Method findMethod(Class<?> cls, String name, String parameterType) {
        List<Method> candidates = Arrays.stream(cls.getMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1) // TODO make count != 1 work
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "no public single-argument method named '" + name + "' in " + cls.getName());
        }
        if (parameterType != null) {
            candidates = candidates.stream()
                    .filter(m -> {
                        Class<?> p = m.getParameterTypes()[0];
                        return p.getSimpleName().equals(parameterType) || p.getName().equals(parameterType);
                    })
                    .toList();
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("no public method '" + name + "(" + parameterType
                        + ")' in " + cls.getName());
            }
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("ambiguous method '" + name + "' in " + cls.getName()
                    + "; name the parameter type, e.g. \"" + name + "(List)\"");
        }
        return candidates.get(0);
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
        String parameterType = parseParameterType(signature);

        Class<?> cls;
        try {
            cls = Class.forName(spec.className(), true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("SUT class not found: " + spec.className(), e);
        }
        Method method = findMethod(cls, methodName, parameterType);
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
                return method.invoke(target, input);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("SUT invocation failed: " + cls.getName() + "#" + methodName, e);
            }
        };
    }
}

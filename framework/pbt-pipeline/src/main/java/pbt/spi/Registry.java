package pbt.spi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Resolves component keys to provider instances for one provider type.
 *
 * <p>Providers are discovered with {@link ServiceLoader}, so builtin components and
 * any component a user project puts on the classpath are found the same way and
 * indexed by {@link NamedProvider#name()}. Resolution tries that short-name index
 * first; if the key is instead the fully qualified class name of a matching
 * provider, that class is loaded and instantiated. Nothing about the concrete
 * providers is compiled into the resolver.
 *
 * @param <P> the provider type this registry resolves
 */
public final class Registry<P extends NamedProvider> {

    private final Class<P> providerType;
    private final Map<String, P> byName;

    private Registry(Class<P> providerType, Map<String, P> byName) {
        this.providerType = providerType;
        this.byName = byName;
    }

    /**
     * Loads every provider of {@code providerType} visible to the context classloader.
     */
    public static <P extends NamedProvider> Registry<P> load(Class<P> providerType) {
        return load(providerType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads every provider of {@code providerType} visible to {@code classLoader}.
     */
    public static <P extends NamedProvider> Registry<P> load(Class<P> providerType, ClassLoader classLoader) {
        Map<String, P> byName = new LinkedHashMap<>();
        for (P provider : ServiceLoader.load(providerType, classLoader)) {
            String name = provider.name();
            if (name == null || name.isBlank()) {
                throw new ProviderException(providerType.getSimpleName() + " "
                        + provider.getClass().getName() + " returned a blank name()");
            }
            P previous = byName.putIfAbsent(name, provider);
            if (previous != null) {
                throw new ProviderException("two " + providerType.getSimpleName()
                        + " providers claim the name '" + name + "': "
                        + previous.getClass().getName() + " and " + provider.getClass().getName());
            }
        }
        return new Registry<>(providerType, byName);
    }

    /**
     * Resolves {@code key}: a registered short name first, then the fully qualified
     * class name of a matching provider.
     *
     * @throws ProviderException if the key is neither
     */
    public P get(String key) {
        if (key == null || key.isBlank()) {
            throw new ProviderException("a " + providerType.getSimpleName() + " key must not be blank");
        }
        P provider = byName.get(key);
        if (provider != null) {
            return provider;
        }
        P byClassName = instantiateClassName(key);
        if (byClassName != null) {
            return byClassName;
        }
        throw new ProviderException("unknown " + providerType.getSimpleName() + " '" + key
                + "'. Registered: " + byName.keySet());
    }

    /**
     * True when {@code key} resolves without instantiating or throwing.
     */
    public boolean has(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (byName.containsKey(key)) {
            return true;
        }
        if (key.indexOf('.') < 0) {
            return false;
        }
        try {
            Class<?> type = Class.forName(key, false, Thread.currentThread().getContextClassLoader());
            return providerType.isAssignableFrom(type);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * The registered short names, in discovery order.
     */
    public Set<String> names() {
        return Set.copyOf(byName.keySet());
    }

    private P instantiateClassName(String key) {
        if (key.indexOf('.') < 0) {
            return null; // short names have no dots; only a class name qualifies for lookup
        }
        Class<?> type;
        try {
            type = Class.forName(key, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
        if (!providerType.isAssignableFrom(type)) {
            throw new ProviderException(key + " is not a " + providerType.getName());
        }
        try {
            try {
                Method factory = type.getDeclaredMethod("provider");
                if (Modifier.isStatic(factory.getModifiers())) {
                    return providerType.cast(factory.invoke(null));
                }
            } catch (NoSuchMethodException ignored) {
            }
            return providerType.cast(type.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new ProviderException("cannot instantiate " + providerType.getSimpleName() + " " + key, e);
        }
    }
}

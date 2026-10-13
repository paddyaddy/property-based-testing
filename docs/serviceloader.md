# Component discovery (ServiceLoader)

Every pluggable component (generator, shrinker, validator, transform, SUT runner,
value type) is resolved at runtime. Nothing about the concrete
components is compiled into `pbt-pipeline`; the core only knows the provider
contracts.

## How resolution works

Each component kind has a provider interface that extends `pbt.spi.NamedProvider`
and reports a short `name()`. A `pbt.spi.Registry<P>` loads all providers of a kind
with `java.util.ServiceLoader` and indexes them by name:

```java
Registry<GeneratorProvider> generators = Registry.load(GeneratorProvider.class);
GeneratorProvider provider = generators.get(key);
```

`get(key)` resolves in two steps:

1. A registered short name (for example `intList`). This is the normal path for
   builtin and user components alike.
2. Otherwise, if `key` is the fully qualified class name of a matching provider,
   that class is loaded and instantiated. Keys without a dot are treated as short
   names only.

An unknown key fails with a `ProviderException` that lists the registered names. A
duplicate name across two providers of the same kind fails fast at load time.

## Adding a component

A component is discovered the moment its provider is on the classpath and listed in
a service file. There is no registration call and no framework edit.

1. Implement the provider interface for the kind and give it a stable `name()`:

   ```java
   package com.example;

   public final class ReverseGenerator implements GeneratorProvider {
       @Override public String name() { return "reverse"; }
       // ... build method for the component ...
   }
   ```

2. List it in a service file named after the fully qualified provider interface (the SPIs live in sub-packages such as `pbt.spi.generator`, `pbt.spi.shrinker`, `pbt.spi.sut`):

   ```
   src/main/resources/META-INF/services/pbt.spi.generator.GeneratorProvider
   ```
   ```
   com.example.ReverseGenerator
   ```

3. Select it from a descriptor by its short name (`"kind": "reverse"`).

## Selecting a component without a service file

Any provider can also be selected by its fully qualified class name, which is
useful for a one-off or a quick experiment:

```json
"generator": { "kind": "com.example.ReverseGenerator" }
```

The class must implement the expected provider interface, and either declare a
`public static T provider()` factory method (the Java SPI convention for providers
with constructor dependencies) or have a public no-argument constructor.

# Custom generator

Interface: `pbt.spi.generator.GeneratorProvider`, producing a `Generator<T>`.

```java
package com.example;

public final class EvenIntGeneratorProvider implements GeneratorProvider {

    @Override
    public String name() { return "evenInt"; }

    @Override
    public Generator<Integer> build(JsonObject params) {
        int max = params.has("max") ? params.get("max").getAsInt() : 100;
        return rnd -> rnd.nextInt(max / 2 + 1) * 2;
    }
}
```

- `params` is the `"params"` object of the descriptor's `generator`; it is empty when omitted.
- The `Random` passed to `generate` is seeded from the suite `seed`. Draw all randomness from it
  so runs stay reproducible.
- `Generator.describe(T)` is optional and only affects reporting.
- Register `com.example.EvenIntGeneratorProvider` in
  `META-INF/services/pbt.spi.generator.GeneratorProvider`.

```json
"generator": { "kind": "evenInt", "params": { "max": 50 } }
```

A new type also needs a shrinker and a value type with the same name, see
[README.md](README.md#adding-a-new-value-type).

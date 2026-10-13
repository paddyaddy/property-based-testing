# Custom shrinker

Interface: `pbt.spi.shrinker.ShrinkerProvider`, producing a `Shrinker<T>`. The shrinker is
matched to the suite's generator by identical `name()` and receives the same `params`.

```java
public final class EvenIntShrinkerProvider implements ShrinkerProvider {

    @Override
    public String name() { return "evenInt"; }

    @Override
    public Shrinker<Integer> build(JsonObject params) {
        return v -> v == 0 ? Stream.empty() : Stream.of(0, v / 2 / 2 * 2).filter(c -> c != v);
    }
}
```

- `shrink(value)` returns candidates that are smaller than `value` and never `value` itself.
  A shrinker that returns the input would loop forever.
- Order matters: the minimizer takes the **first** candidate that still fails, so put
  the most aggressive candidates first.
- Candidates must stay valid inputs of the generator's type (and, ideally, its params).
- Register in `META-INF/services/pbt.spi.shrinker.ShrinkerProvider`.

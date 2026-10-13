# Custom value type

Interface: `pbt.spi.type.ValueTypeProvider`. It tells the framework how to serialize and
show values of a type. It is matched to the suite's generator by identical `name()`.

```java
public final class EvenIntValueTypeProvider implements ValueTypeProvider {

    @Override public String name() { return "evenInt"; }

    @Override public Type javaType() { return Integer.class; }

    @Override public String describe(Object value) { return String.valueOf(value); }
}
```

- `javaType()` is a `java.lang.reflect.Type`, so generic types work via Gson's `TypeToken`
  (the built-in `intList` uses `new TypeToken<List<Integer>>() {}.getType()`).
  It is used to write the witness in the result JSON and to deserialize the output of
  non-Java SUTs (`outputType`).
- `describe(value)` is the human-readable form in the shrink log and the task report.
- Register in `META-INF/services/pbt.spi.type.ValueTypeProvider`.

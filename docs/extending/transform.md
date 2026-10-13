# Custom transform

Interface: `pbt.spi.transform.TransformProvider`, returning a stateless `Transform`. A
metamorphic property names it with `"transform"`; the pipeline runs the SUT on the source
input and on the follow-up input and hands both to the validator.

```java
public final class DoubleTransformProvider implements TransformProvider {

    @Override public String name() { return "double"; }

    @Override
    public Transform get() {
        return (source, rnd) -> ((Integer) source) * 2;
    }
}
```

- `apply(source, rnd)` takes and returns `Object`; cast to the suite's input type.
- Draw randomness only from the given `Random` to keep witnesses reproducible.
- The validator decides which relation must hold between the two outputs, so the transform
  and the validator's relation have to agree (for example `negate` goes with a symmetry
  relation). For ASP validators this is the `.lp` file.
- Register in `META-INF/services/pbt.spi.transform.TransformProvider`.

```json
{ "id": "twice", "type": "METAMORPHIC", "transform": "double", "validator": { ... } }
```

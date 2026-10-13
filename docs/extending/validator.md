# Custom validator

Interface: `pbt.spi.validator.ValidatorProvider`, producing a `Validator`.
`build(params, generatorKind)` gets the full `"validator"` object of the property
(including `"kind"`) and the suite's generator name.

```java
public final class SortedOutputValidatorProvider implements ValidatorProvider {

    @Override public String name() { return "sorted"; }

    @Override
    public Validator build(JsonObject params, String generatorKind) {
        return ctx -> {
            List<Integer> out = (List<Integer>) ctx.sourceOutput();
            for (int i = 1; i < out.size(); i++) {
                if (out.get(i - 1) > out.get(i)) {
                    return CheckResult.fail("not_sorted", "out=" + out);
                }
            }
            return CheckResult.pass();
        };
    }
}
```

- `CheckContext` carries `sourceInput`, `sourceOutput`, `followUpInput`, `followUpOutput`.
  The follow-up fields are `null` for invariant properties.
- Return `CheckResult.pass()` or `CheckResult.fail(signature[, diagnostics])`. The signature is a
  short stable label for the kind of failure.
- Read your own settings from `params`, for example `params.get("relation")`.
- For logic that is easier to state declaratively, the built-in `asp` validator runs a `.lp`
  file on the rendered facts; see [../asp-oracle.md](../asp-oracle.md).
- Register in `META-INF/services/pbt.spi.validator.ValidatorProvider`.

```json
"validator": { "kind": "sorted" }
```

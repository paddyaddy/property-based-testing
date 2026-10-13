# Custom SUT runner

Interface: `pbt.spi.sut.SutRunnerProvider`, producing a `SutRunner` from a `SutSpec`. The
`"type"` field of a SUT in the descriptor selects the provider.

```java
public final class HttpSutRunnerProvider implements SutRunnerProvider {

    @Override public String name() { return "http"; }

    @Override
    public SutRunner build(SutSpec spec) {
        URI uri = URI.create(spec.file());   // reuse the generic fields of SutSpec
        return input -> callEndpoint(uri, input);
    }
}
```

- `SutRunner.run(input)` executes one call and returns the output. Throw an unchecked
  exception if the call cannot be executed.
- `SutSpec` offers `className`, `method`, `isStatic`, `args`, `file`, `interpreter`, `timeoutSeconds` and
  `outputType`. Use `outputType` with a `ValueTypeProvider` to deserialize the output if it
  does not come back as a Java object.
- If the runner holds resources (a process, a connection), override `close()`.
  The pipeline calls it when the runner is no longer needed.
- Register in `META-INF/services/pbt.spi.sut.SutRunnerProvider`.
- The Python runner is a complete example of a process-based runner; its protocol is in
  [../interop.md](../interop.md).

```json
"suts": { "reference": { "type": "http", "file": "http://localhost:8080/sort", "outputType": "intList" } }
```

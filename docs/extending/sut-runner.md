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

- `SutRunner.run(input)` executes one call and returns the output. If the system under test
  itself fails (it raises an exception or error, or runs out of time), throw a `SutException`
  with the exception type: the pipeline records it and the oracle sees it as an exception fact.
  Any other exception means the call could not be executed (wrong shape, dead process, bad
  configuration) and aborts the run.
- The input is a `Tuple` when the suite uses the `tuple` generator, one value per parameter;
  spread it over the call's arguments. Any other input is the single argument.
- `SutSpec` offers `className`, `method`, `isStatic`, `args`, `file`, `interpreter`, `timeoutSeconds`,
  `outputType`, `searchPath`, `module` and `workingDir`. Use `outputType` with a `ValueTypeProvider`
  to deserialize the output if it does not come back as a Java object. The paths arrive resolved
  against the project directory. Read `searchPath` as "where this runner loads the SUT's code
  from", in whatever form that takes for the runner.
- If the runner holds resources (a process, a connection), override `close()`.
  The pipeline calls it when the runner is no longer needed.
- Register in `META-INF/services/pbt.spi.sut.SutRunnerProvider`.
- The Python runner is a complete example of a process-based runner; its protocol is in
  [../interop.md](../interop.md).

```json
"suts": { "reference": { "type": "http", "file": "http://localhost:8080/sort", "outputType": "intList" } }
```

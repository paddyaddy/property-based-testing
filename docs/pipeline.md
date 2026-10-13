# Pipeline

`PipelineRunner` runs a suite descriptor: it generates inputs, optionally executes the SUT, validates each property, and minimizes any failing witness. It is fully in-memory and has no side effects unless an `artifactsDir` is configured.

## Stages

For each property in the descriptor the runner executes these stages in order:

```
generate → [applicability] → [transform] → execute → validate → minimize
```

`[applicability]` and `[transform]` are bracketed because they are conditional.

### Generate

`Generator.generate(rnd)` is called `runsPerProperty` times to produce the input sequence. Two modes:

- **Shared** (default): inputs are generated once with `seed` and reused across all properties in the suite. Deterministic and fast.
- **Regenerate**: each property gets its own sequence, seeded with `seed XOR hash(propertyId)`. Used when properties have incompatible preconditions and need different distributions. Opt in by setting `"regenerate": true` on the `"generator"` object in the descriptor.

### Applicability (conditional)

When a property declares an `"applicability"` key, its value is a classpath path to a `.lp` file. The runner loads that program, renders the input as ASP facts with prefix `"in"` (using the suite's fact generator), and runs the solver. Inputs that produce the `skip` atom are skipped entirely — they count neither as a pass nor as a failure. The `skipped` count is recorded in `PropertyResult` and persisted in the artifact. Skipped inputs are not replaced, so a property with a filter checks fewer than `runsPerProperty` inputs; compare `generated`, `runs` and `skipped`. A property that declares `applicability` fails with an error if no `AspEngine` service is available.

### Transform (metamorphic only)

For a `METAMORPHIC` property, `Transform.apply(source, rnd)` turns each source input into a follow-up input. The `rnd` is seeded per-input so witnesses stay reproducible after minimization.

### Execute

`SutRunner.run(input)` is called on the source input to produce `sourceOutput`. For metamorphic properties it is also called on the follow-up input to produce `followUpOutput`. If no `SutRunner` is configured (e.g. when using the trivial validator in unit tests), both outputs remain `null`.

Exceptions from the runner are caught and leave the corresponding output as `null`.

### Validate

`Validator.check(CheckContext)` decides pass or fail for one check. The context carries all four values:

| Field | Invariant | Metamorphic |
|---|---|---|
| `sourceInput` | the generated input | the source input |
| `sourceOutput` | SUT output (or null) | SUT output for source input |
| `followUpInput` | null | the transformed input |
| `followUpOutput` | null | SUT output for follow-up input |

The validator returns `CheckResult(passed, signature, diagnostics)`. On failure the signature is recorded. `diagnostics` is a free-form string filled by the validator (e.g. the assembled ASP facts); it is empty for validators that produce no extra detail.

### Minimize

When any input causes a validation failure the runner applies a greedy shrink loop:

1. Call `Shrinker.shrink(current)` to get a stream of smaller candidates.
2. Skip candidates that the property's applicability filter rejects, so the witness is always an input the property is in scope for.
3. Take the first candidate that also fails validation. For metamorphic properties the follow-up input of a candidate is derived with an RNG seeded from the candidate itself, so the result does not depend on how many candidates were tried before it.
4. Repeat until no smaller failing candidate exists, or until `maxShrinkSteps` candidates have been evaluated (default 1000; the log records when the cap is hit).

The final value is the *witness*: the smallest known counterexample, stored in `PropertyResult.witness()`.

## Results

`PipelineRunner.run(descriptor)` returns a `SuiteResult(name, List<PropertyResult>)`.

`PropertyResult` carries:

| Field | Type | Meaning |
|---|---|---|
| `propertyId` | `String` | matches the descriptor `"id"` |
| `passed` | `boolean` | true when zero failures were found |
| `generated` | `int` | number of inputs generated (`runsPerProperty`) |
| `runs` | `int` | number of inputs checked (`generated` minus `skipped`) |
| `skipped` | `int` | inputs excluded by the applicability filter |
| `failures` | `int` | number of failing checked inputs |
| `witness` | `Optional<Object>` | minimized counterexample, empty on pass |
| `runLog` | `List<String>` | one formatted entry per run (input, outputs, facts, result) plus shrinking steps when the property failed; consumed by the Gradle plugin to populate IDE test output |

`SuiteResult.passed()` is `true` when all properties passed.

## SUT execution overload

`PipelineRunner.run(descriptor, sutRunner)` accepts an explicit `SutRunner`. This is the form used by the Gradle plugin, which builds a runner from the `SutSpec` in the descriptor. Calling `run(descriptor)` is equivalent to `run(descriptor, null)`.

## Artifact persistence

Persistence is opt-in in `PipelineRunner` (the Gradle task always enables it, see [gradle-plugin.md](gradle-plugin.md)). Construct the runner with a non-null `artifactsDir` path to persist per-property JSON after each run:

```java
new PipelineRunner(generators, shrinkers, validators, transforms, valueTypes,
        runsPerProperty, seed, Path.of("build/pbt-artifacts"));
// optional last argument: maximum candidates evaluated per witness, default PipelineRunner.DEFAULT_MAX_SHRINK_STEPS
```

Each property writes `<artifactsDir>/<propertyId>.json` containing a `PropertyResultArtifact` (a serialization-friendly projection of `PropertyResult`). The `witness` field is the Gson-serialized JSON of the counterexample, typed via `ValueTypeProvider.javaType()` so it round-trips cleanly (e.g. an `intList` witness is a JSON array, not a `toString()` string).

## Seeding and reproducibility

Every random sequence is derived from the suite-level `seed`. The same seed on the same descriptor with the same component versions produces identical results. To re-run a failing case directly, copy the witness from the report and pass it as the sole input.

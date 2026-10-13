# Interface Catalogue

All pluggable contracts in `pbt-pipeline` and their built-in implementations from `pbt-components`.

---

## Core extension points

### `NamedProvider`
| | |
|---|---|
| **Contract** | Declares a short string key (`name()`) that descriptor files use to select the component. |
| **Use case** | Base interface for every ServiceLoader-discovered provider. |
| **Implementations** | All provider types below extend this. |

The `Registry<P extends NamedProvider>` discovers implementations via `ServiceLoader`, indexes them by `name()`, and also accepts a fully-qualified class name as a fallback (with optional `public static T provider()` factory method support).

---

## Generation

### `Generator<T>`
| | |
|---|---|
| **Contract** | `T generate(Random rnd)` — produces one test input; `default String describe(T value)` for reporting. |
| **Use case** | Produce the input sequence that the pipeline feeds to the SUT and validator. |

### `GeneratorProvider`
| | |
|---|---|
| **Contract** | `Generator<?> build(JsonObject params)` — constructs a configured generator. |
| **Use case** | ServiceLoader entry point for `Generator` implementations. `params` comes from the `"generator"` object in the suite descriptor. |
| **Implementations** | `IntGeneratorProvider` (`"int"`) · `IntListGeneratorProvider` (`"intList"`) · `BooleanGeneratorProvider` (`"boolean"`) · `LongGeneratorProvider` (`"long"`) · `DoubleGeneratorProvider` (`"double"`, params: `min`/`max`) · `StringGeneratorProvider` (`"string"`, params: `minLen`/`maxLen`/`chars`) |

---

## Shrinking

### `Shrinker<T>`
| | |
|---|---|
| **Contract** | `Stream<T> shrink(T value)` — proposes smaller candidates; must not include `value` itself. |
| **Use case** | Greedy minimization loop: the pipeline replaces a failing witness with the first smaller candidate that still fails. |

### `ShrinkerProvider`
| | |
|---|---|
| **Contract** | `Shrinker<?> build(JsonObject params)` — same `params` shape as the corresponding generator. |
| **Use case** | ServiceLoader entry point for `Shrinker` implementations. Matched to the active generator by identical `name()`. |
| **Implementations** | `IntShrinkerProvider` (`"int"`, toward 0) · `IntListShrinkerProvider` (`"intList"`, block removal + element shrinking) · `BooleanShrinkerProvider` (`"boolean"`, toward `false`) · `LongShrinkerProvider` (`"long"`, toward 0) · `DoubleShrinkerProvider` (`"double"`, toward 0.0 by halving) · `StringShrinkerProvider` (`"string"`, drop chars + shift toward first char in set) |

---

## Metamorphic transforms

### `Transform`
| | |
|---|---|
| **Contract** | `Object apply(Object source, Random rnd)` — derives a follow-up input from a source input. `rnd` is seeded per-input for reproducibility. |
| **Use case** | Metamorphic properties: the pipeline calls the SUT on both `source` and `apply(source)` and checks a relation between the two outputs. |

### `TransformProvider`
| | |
|---|---|
| **Contract** | `Transform get()` — stateless; returns the same transform every time. |
| **Use case** | ServiceLoader entry point for `Transform` implementations. Referenced by `"transform"` in a property descriptor. |
| **Implementations** | `ShuffleTransformProvider` (`"shuffle"`, shuffles `List<Integer>`) · `NegateTransformProvider` (`"negate"`) · `BooleanNegateTransformProvider` (`"booleanNegate"`) · `StringReverseTransformProvider` (`"stringReverse"`) |

---

## SUT execution

### `SutRunner`
| | |
|---|---|
| **Contract** | `Object run(Object input)` — executes one SUT call and returns the output. |
| **Use case** | Decouples the pipeline from any specific execution mechanism (local JVM method, subprocess, network call, …). The pipeline calls this for every generated input and captures the result in `CheckContext`. |

### `SutRunnerProvider`
| | |
|---|---|
| **Contract** | `SutRunner build(SutSpec spec)` — constructs a runner from a `SutSpec` descriptor. |
| **Use case** | ServiceLoader entry point for `SutRunner` implementations. The `SutSpec` carries `className`, `method`, `isStatic`, `args`, `file`, `interpreter`, `timeoutSeconds`, `outputType`; each runner reads only the fields it needs. |
| **Implementations** | `JavaSutRunnerProvider` (`"java"`, reflection on static or instance methods; class and method are resolved once at build time, instance methods run on a fresh instance per call) · `PythonSutRunnerProvider` (`"python"`, keeps one persistent `python3` worker per SUT, started via the bundled `sut_harness.py --server`; newline-delimited JSON requests and responses over stdin/stdout) |

---

## Validation

### `Validator`
| | |
|---|---|
| **Contract** | `CheckResult check(CheckContext ctx)` — decides pass/fail for one execution. Returns a `CheckResult(passed, signature, diagnostics)`. |
| **Use case** | Evaluate a property given the full execution context (inputs + SUT outputs). |

### `ValidatorProvider`
| | |
|---|---|
| **Contract** | `Validator build(JsonObject params, String generatorKind)` — `generatorKind` is the suite's generator name, used by ASP validators to pick the right fact renderer. |
| **Use case** | ServiceLoader entry point for `Validator` implementations. Referenced by `"kind"` in a property's `"validator"` object. |
| **Implementations** | `AspValidatorProvider` (`"asp"`, runs clingo against a `.lp` file; sets `diagnostics` to the assembled ASP facts string) |

### `CheckContext` (record)
| | |
|---|---|
| **Fields** | `sourceInput`, `sourceOutput`, `followUpInput`, `followUpOutput` — all `Object`, nulls until the SUT executes or for invariant properties. |
| **Use case** | Carries all execution data into a validator. Convenience factories: `invariant(input)`, `metamorphic(source, followUp)`. |

---

## ASP integration

### `AspEngine`
| | |
|---|---|
| **Contract** | `boolean entails(String program, String queryAtom)` — returns `true` when the program is satisfiable and the atom appears in some answer set. `default boolean isAvailable()` guards against a missing solver binary. |
| **Use case** | Abstraction over clingo (or any ASP solver) so the validator is not coupled to a specific binary. |
| **Implementations** | `CliAspEngine` (shells out to `clingo`, timeout 10 s). Both `PipelineRunner` (applicability) and `AspValidatorProvider` use the first `AspEngine` found via `ServiceLoader`; the validator falls back to `CliAspEngine` if none is registered. |

### `AspFactGeneratorProvider`
| | |
|---|---|
| **Contract** | `String toFacts(Object value, String prefix)` — renders a typed value as ground ASP facts; `null` must return `""`. |
| **Use case** | `default AspFactGeneratorProvider withParams(JsonObject validatorParams)` lets a renderer take parameters from the property's `validator` object; the default returns the same instance. Used by `AspValidatorProvider` to assemble the fact block passed to the engine, and by `PipelineRunner` to render applicability-check inputs. Each fact is prefixed with the caller-supplied role (e.g. `"in"`, `"out"`, `"source_out"`, `"followup_in"`, …). |
| **Implementations** | `IntAspFactGeneratorProvider` (`"int"`, `prefix(v).`) · `IntListAspFactGeneratorProvider` (`"intList"`, `prefix(i,v).`) · `BooleanAspFactGeneratorProvider` (`"boolean"`, `prefix(0/1).`) · `LongAspFactGeneratorProvider` (`"long"`, `prefix(v).`) · `DoubleAspFactGeneratorProvider` (`"double"`, `prefix(N).` where N = round(value × scale); scale is 1000 unless the property's `validator` sets `"scale"`) · `StringAspFactGeneratorProvider` (`"string"`, `prefix("escaped").`) |

---

## Applicability

Applicability is not a pluggable provider — it is an ASP program stored in a `.lp` classpath resource. When a property declares `"applicability": "path/to/filter.lp"`, `PipelineRunner`:

1. Loads the program from the classpath.
2. Renders the input as ASP facts with prefix `"in"` using the suite's `AspFactGeneratorProvider`.
3. Runs the solver on `inputFacts + filterProgram`.
4. Skips the input (counts as neither pass nor failure) if the `skip` atom is derived.

The framework ships `applicability/at_least_two_elements.lp` (requires a list input with ≥ 2 elements) as a ready-made resource in `pbt-components`. Custom filters are `.lp` files on the project classpath — no Java code needed.

---

## Type metadata

### `ValueTypeProvider`
| | |
|---|---|
| **Contract** | `Type javaType()` — Java reflection type for (de)serialization; `String describe(Object value)` — human-readable rendering for reports and witnesses. |
| **Use case** | Centralises per-type concerns. `PipelineRunner` looks up the provider by the suite's generator kind and uses it in two places: `describe()` formats the witness in the shrink log and the Gradle task reporter; `javaType()` drives `ArtifactCodec` so the artifact witness field is valid JSON that round-trips back to the original type. `PythonSutRunnerProvider` also uses `javaType()` to deserialize SUT output from JSON. Matched to the active generator by identical `name()`. |
| **Implementations** | `IntValueTypeProvider` (`"int"`, `Integer.class`) · `IntListValueTypeProvider` (`"intList"`, `List<Integer>` via Gson `TypeToken`) · `BooleanValueTypeProvider` (`"boolean"`, `Boolean.class`) · `LongValueTypeProvider` (`"long"`, `Long.class`) · `DoubleValueTypeProvider` (`"double"`, `Double.class`) · `StringValueTypeProvider` (`"string"`, `String.class`, wraps value in quotes in `describe()`) |

# Code walkthrough: from suite descriptor to test result

Paths are relative to the repo root; `F/` = `framework/`, `C/` = `F/pbt-components/src/main/java/pbt/components`,
`P/` = `F/pbt-pipeline/src/main/java/pbt`, `G/` = `F/pbt-gradle-plugin/src/main/java/pbt/gradle`.

## 0. The input: logic only, no code

- `demo/src/main/resources/suites/sort.json`: SUTs (reference + buggy), generator, five properties.
- `demo/src/main/resources/asp/sort_elements_preserved.lp`: one property, derives `violation`.
- `demo/src/main/resources/suites/sort_python.json`: same `.lp` files, Python SUT. Only `suts` differs.
- `demo/src/main/resources/suites/find_first.json`: a two-parameter SUT (`tuple` generator, `outputKind`).
- `demo/src/main/resources/suites/safe_division.json`: SUT exceptions seen by the oracle.

## 1. Gradle entry point and classloading

- `G/PropertyBasedTestsPlugin.java:27`: registers `propertyBasedTests`;
  lines 44-45 put the chosen source set's (`pbt.sourceSet`, default `main`) output and runtime classpath on the task's
  SUT classpath; line 72 makes `test` depend on it, so `check` runs it.
- `G/PropertyBasedTestsTask.java:181-183`: **the key trick.** A `URLClassLoader`
  over the project classpath, parent = plugin classloader (which holds the built-in components), set
  as thread context classloader. From here on, `ServiceLoader`, `Class.forName` and `.lp` resource
  loading all see built-ins *and* user code/resources.
- Same file: `:193` loads the registries, `:211` reads each descriptor, `:229` builds the SUT runner,
  `:233` runs the pipeline.

## 2. Descriptor and component lookup

- `P/descriptor/SuiteLoader.java:45`: JSON → records, structural validation only (does a component
  exist is decided later).
- `P/spi/Registry.java:35`: `ServiceLoader` discovery, indexed by `name()` (duplicates fail fast).
  `:66` `get(key)`: short name first, then fully-qualified class name (`:116`, `provider()` factory
  right after). Registration = one line in `META-INF/services/<interface>`
  (built-ins: `F/pbt-components/src/main/resources/META-INF/services/`).

## 3. The pipeline: `P/pipeline/PipelineRunner.java`

| Step                | Line                  | What happens                                                                              |
|---------------------|-----------------------|-------------------------------------------------------------------------------------------|
| resolve             | 234-251           | generator, shrinker, value type looked up by the generator `kind` (same name binds them)  |
| generate            | 247                   | one seeded input batch, shared by all properties                                          |
| per property        | 253                   | one loop iteration per property; validator built from `validator.kind` (`:291`)        |
| applicability       | 257, 331-346  | input → `in(...)` facts + filter `.lp`; `skip` derived → input skipped                    |
| transform + execute | 150-181           | metamorphic: follow-up input via transform; SUT called on source (and follow-up); a `SutException` becomes the recorded exception type |
| validate            | 310                   | `validator.check(ctx)`                                                                    |
| minimize            | 183-227, call at 322 | greedy shrink loop, applicability-aware, step-capped, per-candidate seeded transform      |
| persist             | 264                   | `<artifacts>/<propertyId>.json`                                                           |

## 4. Executing the SUT (execution side)

- Java, `C/sut/JavaSutRunnerProvider.java`: class and method resolved once (`:185`, `findMethod` at
  `:79`, which also selects overloads by parameter types), then `method.invoke` per input (`:216`).
  `arguments` (`:104`) spreads a `Tuple` input over the parameters. An exception thrown by the SUT
  becomes a `SutException` carrying its type (`:219`). With a `classpath` in the SUT spec the class is
  loaded by a `ChildFirstClassLoader` (`:143`, created at `:182`), so two versions of one class can coexist.
- Python, `C/sut/PythonSutRunnerProvider.java:103`: one persistent worker per SUT; `run` (`:154`) sends one JSON line
  (`{"input": ...}` or `{"args": [...]}` for a tuple) and reads one back, with a per-call timeout; output
  deserialised via the value type (`:197`). Errors and timeouts become `SutException`.
- Other side: `F/pbt-components/src/main/resources/python/sut_harness.py:168` (`_server_loop`):
  module loaded once, `fn(*args)` per line (`:188`).

## 5. Values → ASP → verdict (logic side)

- `C/asp/facts/IntListAspFactGeneratorProvider.java:21`: `[13,18,-30]` → `in(0,13). in(1,18). in(2,-30).`
- `C/asp/facts/TupleAspFactGeneratorProvider.java:54`: a tuple renders each component with its own
  generator and the position in the prefix: `([1,2], 5)` → `in_0(0,1). in_0(1,2). in_1(5).`
- `C/validator/AspValidatorProvider.java:58`: facts with prefixes `in/out` (invariant) or
  `source_in/source_out/followup_in/followup_out` (metamorphic), plus `exception(...)` facts when the SUT raised
  one, then the relation program; `failOnException` fails early (`:135`); otherwise it fails if `violation`
  is entailed (`:139`). Inputs and outputs may use different fact generators (`outputKind`).
- `C/asp/CliAspEngine.java:62`: program piped into `clingo` on stdin; satisfiable + atom in
  answer set = entailed.

## 6. The result

- Console / IDE: `PropertyBasedTestsTask.java:238` reports each property as a test (fail message
  contains runs, failures, witness); per-run log (inputs, outputs, facts, shrink steps) is the test
  output. Any failure → `GradleException` (`:283`).
- Files in `demo/build/`: `reports/pbt/<suite>/<sut>/<property>.json` (artefact) and
  `test-results/propertyBasedTests/index.html` (HTML test report, linked in the Gradle output).

## Live demo

```bash
cd demo && ./gradlew propertyBasedTests   # build fails on purpose: buggy SUTs are caught
```
Show `sort-buggy/S3_elements_preserved` in the HTML report: facts of one run, then shrinking
`[4,-44,-13,-48,-2,20,-7,-16,36] → … → [0,0]`.

## AI Notice

This summary has been generated with Claude Sonnet 5.5 with the prompt:

```
Give me a short presentation.md, which essentially is a summary of where the interesting logic is located.
This document should essentially guide one through how the code path is from
"here is a suite descriptor" to "here is the asp" to "here is the result of the test" and everything in between.
```

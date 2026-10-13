# Code walkthrough: from suite descriptor to test result

Paths are relative to the repo root; `F/` = `framework/`, `C/` = `F/pbt-components/src/main/java/pbt/components`,
`P/` = `F/pbt-pipeline/src/main/java/pbt`, `G/` = `F/pbt-gradle-plugin/src/main/java/pbt/gradle`.

## 0. The input: logic only, no code

- `demo/src/main/resources/suites/sort.json`: SUTs (reference + buggy), generator, five properties.
- `demo/src/main/resources/asp/sort_elements_preserved.lp`: one property, derives `violation`.
- `demo/src/main/resources/suites/sort_python.json`: same `.lp` files, Python SUT. Only `suts` differs.

## 1. Gradle entry point and classloading

- `G/PropertyBasedTestsPlugin.java:23`: registers `propertyBasedTests`;
  lines 34-35 put the project's compiled classes, resources and `runtimeClasspath` on the task's
  SUT classpath; line 63 hooks it into `check`.
- `G/PropertyBasedTestsTask.java:87-89`: **the key trick.** A `URLClassLoader`
  over the project classpath, parent = plugin classloader (which holds the built-in components), set
  as thread context classloader. From here on, `ServiceLoader`, `Class.forName` and `.lp` resource
  loading all see built-ins *and* user code/resources.
- Same file: `:99` loads the registries, `:117` reads each descriptor, `:135` builds the SUT runner,
  `:139` runs the pipeline.

## 2. Descriptor and component lookup

- `P/descriptor/SuiteLoader.java:50`: JSON → records, structural validation only (does a component
  exist is decided later).
- `P/spi/Registry.java:40`: `ServiceLoader` discovery, indexed by `name()` (duplicates fail fast).
  `:62` `get(key)`: short name first, then fully-qualified class name (`:70`, `provider()` factory at
  `:117`). Registration = one line in `META-INF/services/<interface>`
  (built-ins: `F/pbt-components/src/main/resources/META-INF/services/`).

## 3. The pipeline: `P/pipeline/PipelineRunner.java`

| Step                | Line         | What happens                                                                             |
|---------------------|--------------|------------------------------------------------------------------------------------------|
| resolve             | 115-124      | generator, shrinker, value type looked up by the generator `kind` (same name binds them) |
| generate            | 130          | one seeded input batch, shared by all properties                                         |
| per property        | 161          | validator resolved from `validator.kind` (170)                                           |
| applicability       | 185, 211-225 | input → `in(...)` facts + filter `.lp`; `skip` derived → input skipped                   |
| transform + execute | 260-274      | metamorphic: follow-up input via transform; SUT called on source (and follow-up)         |
| validate            | 191          | `validator.check(ctx)`                                                                   |
| minimize            | 203, 289-330 | greedy shrink loop, applicability-aware, step-capped, per-candidate seeded transform     |
| persist             | 141-145      | `<artifacts>/<propertyId>.json`                                                          |

## 4. Executing the SUT (execution side)

- Java, `C/sut/JavaSutRunnerProvider.java:28-60`: class and method resolved once via the context
  classloader (`:36`), then `method.invoke` per input (`:56`).
- Python, `C/sut/PythonSutRunnerProvider.java:118-158`: one persistent worker per SUT; one JSON line
  out, one JSON line back, with a per-call timeout; output deserialised via the value type (`:158`).
- Other side: `F/pbt-components/src/main/resources/python/sut_harness.py:166` (`_server_loop`):
  module loaded once, `fn(input)` per line (`:186`).

## 5. Values → ASP → verdict (logic side)

- `C/asp/facts/IntListAspFactGeneratorProvider.java:20`: `[13,18,-30]` → `in(0,13). in(1,18). in(2,-30).`
- `C/asp/AspValidatorProvider.java:52-77`: facts with prefixes `in/out` (invariant, `:75`) or
  `source_in/source_out/followup_in/followup_out` (metamorphic, `:70`) + relation program;
  fail if `violation` is entailed (`:61`).
- `C/asp/CliAspEngine.java:48-63`: program piped into `clingo` on stdin; satisfiable + atom in
  answer set = entailed.

## 6. The result

- Console / IDE: `PropertyBasedTestsTask.java:159` reports each property as a test (fail message
  contains runs, failures, witness); per-run log (inputs, outputs, facts, shrink steps) is the test
  output. Any failure → `GradleException` (`:189`).
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

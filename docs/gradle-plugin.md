# Gradle plugin

The `org.tugraz.pbt` plugin adds a `propertyBasedTests` task that discovers suite descriptor JSON files, runs the PBT pipeline against each declared SUT, and fails the build if any property is violated.

## Applying the plugin

The plugin is published as part of the `framework/` build. A consumer project applies it via an included build:

`settings.gradle`:
```groovy
pluginManagement {
    includeBuild '../framework'    // or wherever the framework lives
}
```

`build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.tugraz.pbt'
}
```

The plugin requires the `java` plugin to be applied (it reads the main source set's output classpath).

## Configuration

Use the `pbt {}` extension block to override defaults:

```groovy
pbt {
    suiteDir = layout.projectDirectory.dir('src/main/resources/suites')
    runsPerProperty = 200
    seed = 12345L
}
```

| Property | Default | Meaning |
|---|---|---|
| `suiteDir` | `src/main/resources/suites` | directory scanned for `**/*.json` descriptor files |
| `runsPerProperty` | `100` | number of inputs generated per property |
| `maxShrinkSteps` | `1000` | maximum candidates evaluated while minimizing one witness |
| `seed` | `42` | master random seed for all generators |

Artifact persistence is always enabled. Results are written to
`build/reports/pbt/<suiteName>/<sutName>/<propertyId>.json` — one JSON file per
property per SUT, containing pass/fail, the generated, checked and skipped input counts, failure count, and the minimized
witness (as a string) when the property failed.

## The `propertyBasedTests` task

The task is in the `verification` group and is automatically added to the `check` lifecycle, so `./gradlew check` runs it. It **always re-runs** regardless of whether inputs changed (equivalent to `outputs.upToDateWhen { false }`).

At execution time it:

1. Builds a `URLClassLoader` from the project's compiled main classes, processed resources, and `runtimeClasspath`. This is the classpath the SUT's reflection-based runner uses to load SUT classes.
2. Sets that classloader as the thread's context classloader so `ServiceLoader` finds builtin components from `pbt-components` (shipped with the plugin) and any additional components the project puts on its classpath.
3. Loads each `*.json` from `suiteDir` via `SuiteLoader`.
4. For each SUT declared in a descriptor, builds a `SutRunner` via the `SutRunnerProvider` registry and runs the pipeline.
5. Streams test events via Gradle's `TestEventReporterFactory` API so IntelliJ and other Gradle-aware IDEs receive live pass/fail results in their test view.
6. Logs `[suiteName/sutName] PASSED|FAILED` for every SUT, and the witness for any failing property.
7. Throws a `GradleException` listing all failing properties if any exist.

## IDE test output

When a test is selected in the IDE's test view, clicking on a SUT group shows the full descriptor configuration (suite name, generator, SUT spec, runs per property, seed). Clicking on an individual property shows:

- The property descriptor fields (type, validator config, transform if metamorphic)
- The loaded `.lp` applicability program (when `"applicability"` is declared)
- The loaded `.lp` relation program (for ASP validators)
- One log entry per run: inputs, SUT outputs, ASP facts, and PASS/FAIL result
- For failing properties: the shrinking steps tried and the minimal witness

## Test reports

Two report tasks are registered automatically:

| Task | Output | Consumes |
|---|---|---|
| `propertyBasedTestsReport` | `build/reports/tests/propertyBasedTests/index.html` | binary results from `build/test-results-binary/propertyBasedTests/` |

JUnit XML files are also written to `build/test-results/propertyBasedTests/` for CI tooling.

## Where to put files

By convention:

```
src/main/resources/
  suites/
    mysuite.json          ← suite descriptor
  asp/
    my_property.lp        ← ASP relation programs referenced by "relation": "asp/my_property.lp"
```

Both directories end up on the main source set's resource path and are visible to the context classloader during the task.

## Adding custom components

Put a provider implementation and its `META-INF/services/` entry on the project's `runtimeClasspath`. The task's URLClassLoader inherits from the plugin's classloader (which has all builtin providers), so your provider is discovered alongside the builtins without any framework edit.

Example — adding a custom generator:

```groovy
// build.gradle
dependencies {
    runtimeOnly 'com.example:my-generators:1.0'
}
```

Reference it in a descriptor by its registered short name:

```json
"generator": { "kind": "myCustom" }
```

## Running manually

```
./gradlew propertyBasedTests
```

The task always re-runs. To also open the HTML report:

```
./gradlew propertyBasedTests propertyBasedTestsReport
```

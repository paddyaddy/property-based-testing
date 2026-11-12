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

The plugin requires the `java` plugin to be applied (it reads a source set's output and runtime classpath, `main` by default).

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
| `sourceSet` | `main` | source set whose output and runtime classpath SUTs and custom components are loaded from; must exist when the task runs |
| `suiteDir` | `src/<sourceSet>/resources/suites` | directory scanned for `**/*.json` descriptor files |
| `runsPerProperty` | `100` | number of inputs generated per property |
| `maxShrinkSteps` | `1000` | maximum candidates evaluated while minimizing one witness |
| `seed` | `42` | master random seed for all generators |

Artifact persistence is always enabled. Results are written to
`build/reports/pbt/<suiteName>/<sutName>/<propertyId>.json` — one JSON file per
property per SUT, containing pass/fail, the generated, checked and skipped input counts, failure count, and the minimized
witness (as a string) when the property failed.

## The `propertyBasedTests` task

The task is in the `verification` group. The plugin makes `test` depend on it, so `./gradlew test` and `./gradlew check` run it. It **always re-runs** regardless of whether inputs changed (equivalent to `outputs.upToDateWhen { false }`).

At execution time it:

1. Builds a `URLClassLoader` from the project's compiled main classes, processed resources, and `runtimeClasspath`. This is the classpath the SUT's reflection-based runner uses to load SUT classes, unless a SUT lists its own `classpath` (see [suite-descriptor.md](suite-descriptor.md#sutspec)), in which case that SUT is loaded from it in a classloader of its own.
2. Sets that classloader as the thread's context classloader so `ServiceLoader` finds builtin components from `pbt-components` (shipped with the plugin) and any additional components the project puts on its classpath.
3. Loads each `*.json` from `suiteDir` via `SuiteLoader`.
4. For each SUT declared in a descriptor, builds a `SutRunner` via the `SutRunnerProvider` registry and runs the pipeline.
5. Streams test events via Gradle's `TestEventReporterFactory` API so IntelliJ and other Gradle-aware IDEs receive live pass/fail results in their test view.
6. Logs `[suiteName/sutName] PASSED|FAILED` for every SUT and one summary line per property (`generated`, `checked`, `skipped`, `failures`, plus the `witness` when it failed). The same summary starts the property's output in the HTML report, whose run log lists the input of every run, including skipped ones.
7. Throws a `GradleException` listing all failing properties if any exist.

## IDE test output

The task sets the extra property `idea.internal.test = true`. IntelliJ treats a task as a test task, and shows its results in the test tree, only if it extends Gradle's `Test` or carries this property. After upgrading the plugin, reload the Gradle project in the IDE so the flag is picked up.

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

## Using it in a library, like a test dependency

A library keeps its suites, ASP programs and custom components in a source set of their own, next to `main` and `test`, so that they never mix with the library's unit tests:

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.tugraz.pbt'
}

sourceSets {
    pbt {
        compileClasspath += sourceSets.main.output
        runtimeClasspath += sourceSets.main.output
    }
}

dependencies {
    pbtCompileOnly 'org.tugraz.pbt:pbt-pipeline:0.1.0'   // only if the source set has its own components
}

pbt {
    sourceSet = 'pbt'
}
```

```
src/pbt/
  java/                          custom generators, renderers, ... (optional)
  resources/
    META-INF/services/           their registrations
    asp/                         relation programs
    suites/                      suite descriptors
```

A SUT is then simply a class of the library itself (`"class": "org.example.Foo"`, no `classpath`), and `propertyBasedTests` shows up in the IDE's Gradle view like any other task. To run the same suites against two versions of the library, check out the revision and run the task again.

## Adding custom components

Put a provider implementation and its `META-INF/services/` entry on the project's `runtimeClasspath`. The task's URLClassLoader inherits from the plugin's classloader (which has all builtin providers), so your provider is discovered alongside the builtins without any framework edit.

To compile your own provider against the SPI contracts (`pbt.spi.*`), the project needs `pbt-pipeline` on its compile classpath. With the framework as an included build, declare the include at the top level of `settings.gradle` (not only in `pluginManagement`) so the dependency can be substituted:

```groovy
// settings.gradle
includeBuild '../pbt/framework'

// build.gradle
dependencies {
    compileOnly 'org.tugraz.pbt:pbt-pipeline:0.1.0'
}
```

A type that is not built in needs one provider per concern, all with the same short name: a generator, a `ValueTypeProvider` (required) and, optionally, a shrinker and an `AspFactGeneratorProvider`. When the type is only a variation of a built-in one, the suite's `"factGenerator"` can name an existing fact generator instead.

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

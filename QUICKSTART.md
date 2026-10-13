# Quick start

## Prerequisites

- Java 21 JDK (any vendor) — Gradle 9.3.1 ships via the wrapper and detects the toolchain automatically.
- `clingo` on `PATH` — required for ASP-backed properties. Install from [potassco.org/clingo](https://potassco.org/clingo/). Tests that need it are skipped automatically when it is absent.
- `python3` on `PATH` — required only when a suite targets a Python SUT.

## Run the demo

```bash
cd demo
./gradlew propertyBasedTests
```

The demo runs all suite descriptors under `demo/src/main/resources/suites/` against their reference and buggy SUTs. Expect the reference SUTs to pass and the buggy ones to fail with a minimized witness.

To also generate an HTML report:

```bash
./gradlew propertyBasedTests propertyBasedTestsReport
# report: demo/build/reports/tests/propertyBasedTests/index.html
```

## Add the framework to a new project

### 1. Point plugin management at the framework build

`settings.gradle`:
```groovy
pluginManagement {
    includeBuild '../framework'    // path to the framework directory
}
```

### 2. Apply the plugin

`build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.tugraz.pbt'
}
```

The `java` plugin must be applied. The framework ships its built-in components as a plugin dependency — no extra `dependencies` block needed for them.

### 3. Write a suite descriptor

Place a JSON file under `src/main/resources/suites/` (the default location). Minimal example:

```json
{
  "name": "my-suite",
  "suts": {
    "ref": {
      "type": "java",
      "class": "com.example.MyClass",
      "method": "myMethod(int)",
      "static": true
    }
  },
  "generator": { "kind": "int" },
  "properties": [
    {
      "id": "P1",
      "type": "INVARIANT",
      "validator": { "kind": "trivial" }
    }
  ]
}
```

See [docs/suite-descriptor.md](docs/suite-descriptor.md) for the full field reference and annotated examples.

### 4. Run

```bash
./gradlew propertyBasedTests
```

The task is also wired into `check`, so `./gradlew check` runs it. Results land in `build/reports/pbt/<suite>/<sut>/<propertyId>.json`.

### Optional configuration

```groovy
pbt {
    suiteDir         = layout.projectDirectory.dir('src/main/resources/suites')
    runsPerProperty  = 200
    seed             = 12345L
}
```

See [docs/gradle-plugin.md](docs/gradle-plugin.md) for all options and how to add custom components.

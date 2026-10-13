# Getting started

## Prerequisites

- A JDK for running Gradle. Gradle 9.3.1 ships via the wrapper in each build.
- The build compiles against a Java 21 toolchain, detected from the JDKs installed
  on your machine. Install a Java 21 JDK (any vendor) and Gradle uses it
  automatically. No toolchain download is configured, to keep dependencies minimal.
- `clingo` is required for ASP-backed properties. Install it from
  [potassco.org/clingo](https://potassco.org/clingo/) and make sure it is on `PATH`.
  Tests that need the ASP oracle are skipped automatically when `clingo` is absent.
- `python3` is required for the Python demo suite (`sort_python`) and for any Python SUT.

## Layout

The repository holds two independent Gradle builds:

- `framework/`: the shippable framework (`pbt-pipeline`, `pbt-components`,
  `pbt-gradle-plugin`).
- `demo/`: a consumer that applies the framework's Gradle plugin through an
  included build. It is never part of the framework and is not published.

## Build the framework

```bash
cd framework
./gradlew build
```

## Run the demo task

```bash
cd demo
./gradlew propertyBasedTests
```

This runs six demo suites (`sort`, `sort_python`, `abs_long`, `abs_double`, `not_bool`,
`reverse_string`). Each suite has a `reference` SUT, which passes, and a deliberately
buggy SUT, whose failing properties are reported with a minimized witness. The build
therefore fails by design; the failures are the expected result of the demo.
Requires `clingo` on `PATH` for the ASP-backed properties and `python3` for `sort_python`.

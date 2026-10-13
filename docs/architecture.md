# Architecture

## Two independent builds

The repository contains two Gradle builds that never depend on each other in the
wrong direction.

- `framework/` is the product. It is the only thing you ship.
- `demo/` consumes the framework exactly as an external project would: it applies
  the framework's Gradle plugin via an included build (`includeBuild '../framework'`).
  Deleting `demo/` has no effect on the framework.

Keeping the demo in a separate build is a deliberate check on independence. The
framework cannot accidentally reference demo code, because demo code is not on its
compile classpath.

## Framework modules

The framework build has three modules:

```
pbt-gradle-plugin ──> pbt-components ──> pbt-pipeline
```

- `pbt-pipeline` (core): the pipeline runner, descriptor model
  and loader, service-provider contracts, and artifact codec. It contains no
  concrete component and no oracle.
- `pbt-components`: the pre-built pluggable components (generators, shrinkers,
  validators, transforms, SUT runners, ASP fact generators, value types), each
  registered through `ServiceLoader`.
- `pbt-gradle-plugin`: adds the `propertyBasedTests` task to a consuming project.
  Ships `pbt-components` as a dependency so builtin providers are available
  without extra setup in consuming projects.

## How pluggability works

Every component is resolved at runtime, not compiled into the runner. A descriptor
names a component by a short key; a generic registry looks that key up through
`ServiceLoader`, and falls back to a fully qualified class name when the key is not
a known short name. Builtin components live in `pbt-components`; a user project adds
its own by placing them on the classpath. Nothing but the service-provider
contracts is hardcoded into `pbt-pipeline`.

## The pipeline

The runner composes six stages:

```
generate → [applicability] → [transform] → execute → validate → minimize
```

`[applicability]` and `[transform]` are bracketed because they are conditional:
applicability filters skip inputs that don't meet a precondition (skipped inputs
count neither as passes nor failures); transform only runs for metamorphic properties. Artifact persistence is opt-in in `PipelineRunner` but always on in the Gradle task. See
[pipeline.md](pipeline.md) for full details.

# Extending the framework

Every component kind is a provider interface discovered with `ServiceLoader`
(see [../serviceloader.md](../serviceloader.md)). The steps are always the same:

1. Implement the provider interface and give it a stable `name()`.
2. List the class in `META-INF/services/<fully qualified provider interface>` on the
   project's `runtimeClasspath`.
3. Select it from a suite descriptor by its short name.

Guides:

- [generator.md](generator.md): produce inputs
- [shrinker.md](shrinker.md): minimize failing inputs
- [value-type.md](value-type.md): serialization and rendering of a type
- [transform.md](transform.md): follow-up inputs for metamorphic properties
- [validator.md](validator.md): decide pass or fail
- [sut-runner.md](sut-runner.md): execute the system under test

## Adding a new value type

A new input type needs three providers with the **same `name()`**: a
`GeneratorProvider`, a `ShrinkerProvider` and a `ValueTypeProvider`. The pipeline matches
the shrinker and value type to the suite's generator `kind` by name. For ASP-backed
properties add an `AspFactGeneratorProvider` with that name too
(see [../asp-oracle.md](../asp-oracle.md)).

Full catalogue of the built-in components: [../interfaces.md](../interfaces.md).

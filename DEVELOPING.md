# Developer guide

## Repository layout

```
framework/   the shippable product (three Gradle modules)
demo/        consumer build — applies the plugin exactly as an external project would
docs/        detailed reference documentation
ai-docs/     AI-usage instructions (not part of the product)
```

The two Gradle builds (`framework/` and `demo/`) are intentionally independent. `demo/` consumes `framework/` via `includeBuild`; nothing in `framework/` can reference `demo/`. See [docs/architecture.md](docs/architecture.md) for a full description.

## Framework modules

```
pbt-gradle-plugin ──> pbt-components ──> pbt-pipeline
```

- **`pbt-pipeline`** — pipeline runner, SPI contracts, descriptor model, artifact codec. No concrete components, no oracle.
- **`pbt-components`** — built-in generators, shrinkers, transforms, validators, SUT runners, ASP fact generators, value types. All registered via `ServiceLoader`.
- **`pbt-gradle-plugin`** — adds `propertyBasedTests` to consuming projects; ships `pbt-components` as a dependency.

## Key concepts

**Pipeline** (`generate → [applicability] → [transform] → execute → validate → minimize`): [docs/pipeline.md](docs/pipeline.md)

**SPI contracts** — every component type is a `NamedProvider` discovered by `ServiceLoader`. The registry resolves a short name from the descriptor, falling back to a fully-qualified class name: [docs/serviceloader.md](docs/serviceloader.md)

**All provider interfaces and their built-in implementations**: [docs/interfaces.md](docs/interfaces.md)

**Suite descriptor JSON format**: [docs/suite-descriptor.md](docs/suite-descriptor.md)

**ASP oracle** (fact format, `.lp` conventions, `CliAspEngine`): [docs/asp-oracle.md](docs/asp-oracle.md)

**Gradle plugin internals** (classloader setup, test event reporting, artifact paths): [docs/gradle-plugin.md](docs/gradle-plugin.md)

## Build and test

```bash
# build and test the framework
cd framework
./gradlew check

# build and test the demo (also exercises the plugin end-to-end)
cd demo
./gradlew propertyBasedTests
```

`clingo` must be on `PATH` for ASP-backed tests; they are skipped automatically when absent.

## Adding a new built-in component

1. Implement the SPI interface (e.g. `GeneratorProvider`) in `pbt-components`.
2. Register the implementation in the corresponding `META-INF/services/` file in `pbt-components`.
3. If the new type needs ASP fact rendering, add an `AspFactGeneratorProvider` with the same `name()`.
4. If the new type needs type metadata (serialization, witness formatting), add a `ValueTypeProvider` with the same `name()`.

Look at an existing type (e.g. `IntGeneratorProvider`, `IntShrinkerProvider`, `IntValueTypeProvider`, `IntAspFactGeneratorProvider`) as a template — they are short and self-contained.

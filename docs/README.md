# Documentation

This folder explains how the framework is built and how to extend it. It is
organized so you can read top to bottom to understand the project, then jump to a
specific "extending" guide when you want to add your own component.

## Map

- [getting-started.md](getting-started.md): prerequisites and how to build and run.
- [architecture.md](architecture.md): the two builds, the modules, and how they depend on each other.
- [serviceloader.md](serviceloader.md): how components are discovered by short name, with the class-name fallback.
- [pipeline.md](pipeline.md): the pipeline stages, `PipelineRunner` behavior, `CheckContext`, results, and artifact persistence.
- [interfaces.md](interfaces.md): every service-provider contract with its built-in implementations.
- [suite-descriptor.md](suite-descriptor.md): the full JSON descriptor reference with minimal and fully-annotated examples.
- [asp-oracle.md](asp-oracle.md): the ASP validator, fact generation format, and `.lp` conventions.
- [gradle-plugin.md](gradle-plugin.md): applying the plugin, the `propertyBasedTests` task, and adding custom components.
- [interop.md](interop.md): the subprocess protocol for non-Java systems under test (Python worker).
- [extending/](extending/README.md): short, copyable guides for a custom generator, shrinker, validator, transform, SUT runner, and value type.

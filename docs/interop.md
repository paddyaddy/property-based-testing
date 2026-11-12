# Interop: non-Java systems under test

A suite can test a SUT that is not written in Java. Today the only non-Java runner is
`python`; it talks to a long-lived Python worker over stdin/stdout. The Java side is
`PythonSutRunnerProvider`, the Python side is the bundled `sut_harness.py`.

## Declaring a Python SUT

```json
"suts": {
  "reference": {
    "type": "python",
    "file": "src/main/python/sort.py",
    "method": "reference_sort(list)",
    "outputType": "intList"
  }
}
```

| Field | Meaning |
|---|---|
| `file` | Python source file, loaded as a module named after the file. Exactly one of `file` and `module` is required |
| `module` | Dotted module name, e.g. `youtube_dl.utils`, imported with `importlib`. Use it for code inside a package, where relative imports such as `from .compat import x` need the package |
| `searchPath` | Optional; directories put in front of `sys.path` before loading, in the given order. Two SUTs with different search paths can load two versions of the same module side by side, since each runs in its own worker |
| `workingDir` | Optional; working directory of the worker, default the project directory |
| `method` | Target: `fn(...)`, `Class.method(...)` or `module.fn(...)`; the parameter list is for readability only |
| `static` | Optional; call the method on the class without instantiating it |
| `interpreter` | Optional; interpreter executable (default `python3`), e.g. a virtualenv's `.venv/bin/python` or a wrapper script that runs Python in a container |
| `timeoutSeconds` | Optional; per-call budget, default 10 |
| `args` | Optional; extra arguments appended to the harness command line |
| `outputType` | Required; value-type key (see [interfaces.md](interfaces.md#type-metadata)) used to deserialize the output |

The SUT file needs no framework imports. It is a plain module with a function that takes
one argument (the generated input) and returns one value:

```python
def reference_sort(xs):
    return sorted(xs)
```

`Class.method(...)` targets instantiate the class with a no-argument constructor unless
the method is a `@staticmethod` / `@classmethod` or `static` is set.

Relative `file`, `searchPath`, `workingDir` and `interpreter` paths are resolved against the
project directory. An interpreter without a directory part, such as `python3`, is looked up on
`PATH`. A function inside a package is declared like this:

```json
"under_test": {
  "type": "python",
  "module": "textops.strings",
  "searchPath": ["src/main/python"],
  "method": "reverse(str)",
  "outputType": "string"
}
```

## Process model

`PythonSutRunnerProvider.build` extracts `sut_harness.py` to a temp file and starts
**one worker per SUT**:

```
python3 <harness> (--file <file> | --module <module>) [--path <entry>]... --target <method> [--static] --server [args...]
```

The worker starts in `workingDir`. If the module cannot be loaded or the target does not
exist, the worker exits with a message on stderr, and the run fails with that message; this
is a configuration error, not an outcome of the SUT.

The pipeline calls the worker once per input and closes it when the property run is over.
The interpreter is `python3` from `PATH` unless the SUT sets `interpreter`.

## Wire protocol

Newline-delimited JSON, one request line answered by exactly one response line:

```
→ {"input": [3, 1, 2]}
← {"output": [1, 2, 3]}

→ {"args": ["ab", 3]}
← {"output": "ababab"}

→ {"input": null}
← {"error": "<exception message>", "type": "<exception class>"}
```

A SUT with several parameters (a `tuple` input) receives `{"args": [...]}` and the harness calls the
function with the list spread as positional arguments; every other input arrives as `{"input": ...}`.

- The input is the generated Java value serialized with Gson. The output is
  deserialized with the Java type of `outputType`.
- A Python exception while running the SUT, or a malformed request, produces an
  `{"error": ..., "type": ...}` line. The Java runner turns it into a `SutException`
  (`Python SUT error: ...`) that carries the type, so the oracle sees it as an exception fact.
  The type is the class qualified by its module, such as `youtube_dl.utils.ExtractorError`, like
  the fully qualified names of Java exceptions; built-in exceptions keep their plain name
  (`ValueError`).
- If the worker exits unexpectedly, the runner fails with the captured stderr.
- stdout is reserved for the protocol, so a SUT must not print to it. Use stderr for debugging.
- EOF on stdin ends the worker. Without `--server` the harness runs once:
  one JSON value on stdin, one JSON result on stdout.
- `timeoutSeconds` (default 10) is the budget for one call. When it expires the worker is
  killed, the call fails, and the next call starts a fresh worker. It also bounds how long
  `close()` waits for the worker to exit.

## Supporting another language

Another language needs a `SutRunnerProvider` that starts the process and speaks the
same protocol, plus a worker in that language. See
[extending/sut-runner.md](extending/sut-runner.md). Reusing the line protocol above keeps
the worker small, because it only has to implement the request loop.

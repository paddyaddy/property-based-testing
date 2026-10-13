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
| `file` | Python source file, resolved against the working directory of the JVM running the task |
| `method` | Target: `fn(...)`, `Class.method(...)` or `module.fn(...)`; the parameter list is for readability only |
| `static` | Optional; call the method on the class without instantiating it |
| `interpreter` | Optional; interpreter executable (default `python3`), e.g. a virtualenv's `python` |
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

## Process model

`PythonSutRunnerProvider.build` extracts `sut_harness.py` to a temp file and starts
**one worker per SUT**:

```
python3 <harness> --file <file> --target <method> [--static] --server [args...]
```

The pipeline calls the worker once per input and closes it when the property run is over.
The interpreter is `python3` from `PATH` unless the SUT sets `interpreter`.

## Wire protocol

Newline-delimited JSON, one request line answered by exactly one response line:

```
→ {"input": [3, 1, 2]}
← {"output": [1, 2, 3]}

→ {"input": null}
← {"error": "<exception message>"}
```

- The input is the generated Java value serialized with Gson. The output is
  deserialized with the Java type of `outputType`.
- A Python exception while running the SUT, or a malformed request, produces an
  `{"error": ...}` line. The Java runner turns it into a `RuntimeException`
  (`Python SUT error: ...`).
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

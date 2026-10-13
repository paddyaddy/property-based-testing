#!/usr/bin/env python3
"""Framework harness for Python SUTs that use the testMethod function-call mode.

Invoked by PythonSutRunner — not called by SUT authors directly:

    python3 sut_harness.py --file <path> --target <name(...)> [--static] --server [extra args...]

The runner starts one persistent worker per SUT with ``--server``.  The worker loads
the named file as a module, resolves the callable named by ``--target``, and then
serves newline-delimited JSON requests on stdin (``{"input": ...}``), answering each
with one line on stdout (``{"output": ...}`` or ``{"error": "..."}``) until stdin
closes.

Without ``--server`` the harness runs in single-shot mode: it reads one JSON input
from stdin, invokes the callable with it as the sole argument, writes the JSON result
to stdout and exits.

Target syntax (mirrors the Java runner's ``testMethod`` field):
  - ``fn(...)``            — module-level function named ``fn``
  - ``Class.method(...)``  — method on class ``Class`` in the module
  - ``module.fn(...)``     — module-level function ``fn``, with an explicit
                             module-name qualifier for readability; the prefix
                             is resolved as a class first, and if no class with
                             that name exists the harness falls back to a
                             module-level lookup of the method name.  This lets
                             SUT files be referenced as plain libraries without
                             any harness-specific boilerplate, e.g.
                             ``tokenizer.tokenize_reference(String)``.

Parameter types inside the parens are accepted for readability but are NOT used
for overload resolution (Python has no static overloads); only the head name is
used as the resolver key.

Instance vs static dispatch:
  - ``@staticmethod`` / ``@classmethod`` → called on the class without an instance
  - ``--static`` flag present            → same as above, for explicit override
  - otherwise                            → class instantiated via no-arg constructor,
                                           method called on the resulting instance
"""

import importlib.util
import inspect
import json
import os
import sys


# ---------------------------------------------------------------------------
# Module loading
# ---------------------------------------------------------------------------

def _load_module(file_path):
    """Import *file_path* as a Python module, resolving relative to CWD.

    Raises ``ImportError`` with a descriptive message on failure.
    """
    abs_path = os.path.join(os.getcwd(), file_path)
    if not os.path.isfile(abs_path):
        raise ImportError(f"file not found: {abs_path!r}")
    spec = importlib.util.spec_from_file_location("_sut_module", abs_path)
    if spec is None or spec.loader is None:
        raise ImportError(f"cannot create module spec from: {abs_path!r}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


# ---------------------------------------------------------------------------
# Target parsing and resolution
# ---------------------------------------------------------------------------

def _parse_target(target_sig):
    """Parse ``name(...)`` or ``Class.method(...)`` → ``(class_name_or_None, method_name)``.

    The parenthesised parameter list is descriptive only; the head name is the
    resolver key (mirrors the Java runner).
    """
    # Strip trailing paren group: "fn(a, b)" → "fn", "A.m(x)" → "A.m"
    head = target_sig.split("(")[0].strip()
    if "." in head:
        class_name, method_name = head.rsplit(".", 1)
        return class_name.strip(), method_name.strip()
    return None, head.strip()


def _resolve_callable(module, class_name, method_name, force_static, file_path, target_sig):
    """Return the callable to invoke for the given target.

    Raises ``AttributeError`` / ``TypeError`` with a descriptive message on failure.

    When ``class_name`` is provided but no class (or other callable container)
    with that name exists in the module, the resolver falls back to a module-level
    lookup of ``method_name``.  This allows ``module.function`` notation where the
    dot-prefix is a plain readability qualifier (e.g. ``tokenizer.tokenize_reference``
    referencing the top-level function ``tokenize_reference`` in the loaded file).
    """
    if class_name is None:
        # Module-level function
        fn = getattr(module, method_name, None)
        if fn is None:
            raise AttributeError(
                f"module loaded from {file_path!r} has no attribute {method_name!r}"
            )
        if not callable(fn):
            raise TypeError(
                f"{method_name!r} in module {file_path!r} is not callable"
            )
        return fn

    # Try to resolve class_name as a class (or any attribute) in the module.
    cls = getattr(module, class_name, None)

    if cls is None:
        # class_name not found — treat it as a module-name qualifier and fall
        # back to a module-level lookup of method_name.
        fn = getattr(module, method_name, None)
        if fn is None:
            raise AttributeError(
                f"module loaded from {file_path!r} has no class {class_name!r} "
                f"and no module-level attribute {method_name!r}"
            )
        if not callable(fn):
            raise TypeError(
                f"{method_name!r} in module {file_path!r} is not callable"
            )
        return fn

    # Inspect the raw descriptor to detect staticmethod/classmethod *before*
    # the descriptor protocol kicks in (getattr_static avoids binding).
    raw = inspect.getattr_static(cls, method_name, None)
    if raw is None:
        raise AttributeError(
            f"class {class_name!r} in {file_path!r} has no attribute {method_name!r}"
        )

    is_static_or_class = force_static or isinstance(raw, (staticmethod, classmethod))

    if is_static_or_class:
        # Call on the class itself — already bound via normal attribute access.
        fn = getattr(cls, method_name)
        if not callable(fn):
            raise TypeError(
                f"{class_name}.{method_name} in {file_path!r} is not callable"
            )
        return fn

    # Instance method: construct via no-arg constructor.
    try:
        instance = cls()
    except TypeError as exc:
        raise TypeError(
            f"cannot instantiate {class_name!r} from {file_path!r} via no-arg constructor: {exc}"
        ) from exc
    fn = getattr(instance, method_name)
    if not callable(fn):
        raise TypeError(
            f"{class_name}.{method_name} in {file_path!r} is not callable"
        )
    return fn


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def _server_loop(fn):
    """Persistent worker loop: read JSON-line requests, write JSON-line responses.

    Request:  {"input": <json>}
    Response: {"output": <json>}  or  {"error": "<message>"}

    Exits cleanly on EOF (stdin closed by the runner).
    """
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            request = json.loads(line)
            input_value = request["input"]
        except Exception as exc:
            sys.stdout.write(json.dumps({"error": f"bad request: {exc}"}) + "\n")
            sys.stdout.flush()
            continue
        try:
            result = fn(input_value)
            sys.stdout.write(json.dumps({"output": result}) + "\n")
        except Exception as exc:
            sys.stdout.write(json.dumps({"error": str(exc)}) + "\n")
        sys.stdout.flush()


def main(argv=None):
    if argv is None:
        argv = sys.argv[1:]

    file_path = None
    target_sig = None
    force_static = False
    server_mode = False

    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg == "--file" and i + 1 < len(argv):
            file_path = argv[i + 1]
            i += 2
        elif arg == "--target" and i + 1 < len(argv):
            target_sig = argv[i + 1]
            i += 2
        elif arg == "--static":
            force_static = True
            i += 1
        elif arg == "--server":
            server_mode = True
            i += 1
        else:
            i += 1  # unknown / passthrough args are forwarded but not used

    if not file_path:
        sys.stderr.write("sut_harness: --file is required\n")
        return 1
    if not target_sig:
        sys.stderr.write("sut_harness: --target is required\n")
        return 1

    # --- Load module ---
    try:
        module = _load_module(file_path)
    except Exception as exc:
        sys.stderr.write(
            f"sut_harness: failed to load file {file_path!r} "
            f"(target={target_sig!r}): {exc}\n"
        )
        return 1

    # --- Resolve callable ---
    class_name, method_name = _parse_target(target_sig)
    try:
        fn = _resolve_callable(module, class_name, method_name, force_static,
                               file_path, target_sig)
    except Exception as exc:
        sys.stderr.write(
            f"sut_harness: cannot resolve target {target_sig!r} "
            f"in {file_path!r}: {exc}\n"
        )
        return 1

    # --- Server mode: loop until stdin is closed ---
    if server_mode:
        _server_loop(fn)
        return 0

    # --- Single-shot mode: read one input, write one output ---
    try:
        raw = sys.stdin.read()
        input_value = json.loads(raw)
    except Exception as exc:
        sys.stderr.write(
            f"sut_harness: failed to parse stdin as JSON "
            f"(file={file_path!r}, target={target_sig!r}): {exc}\n"
        )
        return 1

    try:
        result = fn(input_value)
    except Exception as exc:
        sys.stderr.write(
            f"sut_harness: invocation of {target_sig!r} from {file_path!r} raised: {exc}\n"
        )
        return 1

    json.dump(result, sys.stdout)
    return 0


if __name__ == "__main__":
    sys.exit(main())

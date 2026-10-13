# Suite descriptor reference

A suite descriptor is a JSON file that declares everything the framework needs to
run a property-based test suite: the systems under test, the input generator, and
the properties to check. No Java code is required for the descriptor itself.

---

## Minimal example

The smallest valid suite: one SUT, a generator with defaults, and a single
always-passing invariant property.

```json
{
  "name": "sort-minimal",
  "suts": {
    "ref": { "type": "java" }
  },
  "generator": {
    "kind": "intList"
  },
  "properties": [
    {
      "id": "S1_ordered",
      "type": "INVARIANT",
      "validator": { "kind": "trivial" }
    }
  ]
}
```

Required fields: `name`, `suts` (at least one entry with a `type`),
`generator.kind`, `properties` (at least one entry with `id`, `type`, and
`validator.kind`).

---

## Fully-fledged example

All optional fields shown, with inline explanation in comments
(strip comments before using — JSON does not support `//`).

```json
{
  "name": "sort",

  // Human-readable description; shown in reports but not used by the framework.
  "description": "sort(List<Integer>) → List<Integer>",

  // Map of SUT names to their specs. "all" is a reserved name.
  // A name must not contain a comma.
  "suts": {
    "reference": {
      "type": "java",
      "class": "demo.sort.Sorter",     // fully qualified class
      "method": "sort(java.util.List<java.lang.Integer>)",
      "static": false                  // default false; set true for static methods
    },
    "buggy": {
      "type": "java",
      "class": "demo.sort.BuggySorter",
      "method": "sort(java.util.List<java.lang.Integer>)"
      // timeoutSeconds: per-call wall-clock budget (python); omit for runner default
    }
  },

  "generator": {
    "kind": "intList",                 // registry key of the GeneratorProvider

    // When true, inputs are re-drawn independently for every property.
    // When false (default), the same batch is shared across all properties.
    "regenerate": false,

    // Generator-specific params; passed to GeneratorProvider.build().
    "params": {
      "minLen": 1,
      "maxLen": 12,
      "minValue": -50,
      "maxValue": 50
    }
  },

  // Override which AspFactGeneratorProvider renders values to ASP facts.
  // Defaults to the generator kind ("intList" here), so omit when they match.
  "factGenerator": "intList",

  "properties": [
    {
      "id": "S1_ordered",             // unique within the suite; used as artifact label
      "type": "INVARIANT",

      // Human-readable description; for reports only.
      "description": "output is non-decreasing",

      // Validator object: "kind" selects the ValidatorProvider;
      // remaining fields are forwarded to ValidatorProvider.build().
      "validator": {
        "kind": "asp",
        "relation": "sort_ordered.lp", // classpath resource with the ASP rules
        "violation": "violation",      // query atom (default "violation")
        "failureSignature": "out_of_order" // failure label returned by CheckResult
      },

      // Classpath path to an ASP .lp file that acts as a precondition filter.
      // Inputs where the program derives the "skip" atom are excluded entirely.
      "applicability": "applicability/at_least_two_elements.lp"

      // "transform" must be absent for INVARIANT properties.
    },
    {
      "id": "S4_permutation_invariance",
      "type": "METAMORPHIC",
      "description": "sort(shuffle(xs)) == sort(xs)",
      "validator": {
        "kind": "asp",
        "relation": "sort_output_equal.lp"
      },

      // Registry key of the TransformProvider deriving the follow-up input.
      // Required for METAMORPHIC; must be absent for INVARIANT.
      "transform": "shuffle",

      "applicability": "applicability/at_least_two_elements.lp"
    }
  ]
}
```

---

## Field reference

### Top-level

| Field | Required | Default | Description |
|---|---|---|---|
| `name` | yes | — | Suite name; used in reports and artifact paths |
| `description` | no | — | Human-readable; ignored by the framework |
| `suts` | yes | — | Map of SUT name → `SutSpec`; `"all"` is reserved |
| `generator` | yes | — | Input generator configuration |
| `factGenerator` | no | generator `kind` | Override for the `AspFactGeneratorProvider` key |
| `properties` | yes | — | List of properties; must be non-empty |

### `SutSpec`

| Field | Required | Description |
|---|---|---|
| `type` | yes | Runner kind: `"java"` or `"python"` |
| `class` | java | Fully qualified class name |
| `method` | java, python | Java: method name, optionally with the parameter type, e.g. `sort(List)` or `sort(java.util.List<java.lang.Integer>)`; the type is required when the name is overloaded. Python: `fn(...)` or `Class.method(...)` |
| `static` | java | `true` for a static method (default `false`) |
| `file` | python | Script path |
| `args` | python | Extra arguments passed to the harness process |
| `interpreter` | python | Interpreter executable, e.g. a venv's `python`; default `python3` |
| `timeoutSeconds` | no | Per-call budget in seconds (python: the worker is killed and restarted after a timeout); default 10 |
| `outputType` | non-java | Value-type key for deserializing the SUT's output |

### `GeneratorSpec`

| Field | Required | Default | Description |
|---|---|---|---|
| `kind` | yes | — | `GeneratorProvider` registry key |
| `regenerate` | no | `false` | Regenerate inputs per property instead of sharing |
| `params` | no | `{}` | Forwarded verbatim to `GeneratorProvider.build()` |

### `PropertyDescriptor`

| Field | Required | Default | Description |
|---|---|---|---|
| `id` | yes | — | Unique property identifier |
| `type` | yes | — | `INVARIANT` or `METAMORPHIC` |
| `description` | no | — | Human-readable; ignored by the framework |
| `validator` | yes | — | Object with `"kind"` + component-specific fields |
| `transform` | METAMORPHIC | — | `TransformProvider` key; forbidden for `INVARIANT` |
| `applicability` | no | — | Classpath path to a `.lp` file; inputs where the ASP program derives `skip` are excluded (neither pass nor fail) |

### `validator` object (ASP validator)

| Field | Required | Default | Description |
|---|---|---|---|
| `kind` | yes | — | Must be `"asp"` |
| `relation` | yes | — | Classpath path to the `.lp` relation program |
| `violation` | no | `"violation"` | Query atom that signals a broken property |
| `failureSignature` | no | violation atom | Override failure label inside the validator |

---

## Built-in generator kinds

| Kind | Java type | Default params | Notes |
|---|---|---|---|
| `int` | `Integer` | `min=-1000`, `max=1000` | |
| `intList` | `List<Integer>` | `minLen=0`, `maxLen=10`, `minValue=-100`, `maxValue=100` | |
| `boolean` | `Boolean` | — | No params |
| `long` | `Long` | `min=-1000`, `max=1000` | |
| `double` | `Double` | `min=-1000.0`, `max=1000.0` | ASP facts use scale 1000 by default: fact value = round(v × scale); set `"scale"` in the property's `validator` object to change it |
| `string` | `String` | `minLen=0`, `maxLen=10`, `chars=a-z` | ASP facts are quoted strings: `prefix("value").` |

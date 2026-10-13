# ASP oracle

The `"asp"` validator uses [clingo](https://potassco.org/clingo/) to decide whether a property holds. It compiles the SUT's inputs and outputs into ground ASP facts, appends a user-supplied rule program, and asks whether a designated atom (`violation`) is entailed.

## How it works

For each check the validator:

1. Renders the `CheckContext` values as ground ASP facts (see [Fact format](#fact-format) below).
2. Appends the contents of the `.lp` file named by `"relation"`.
3. Calls `clingo --quiet=1 --verbose=0` on the assembled program.
4. If the program is satisfiable **and** the answer set contains `violation`, the check **fails**. Otherwise it passes.

The inversion (`violation` present = property broken) means the `.lp` file expresses a *violation condition*, not an invariant — which maps cleanly onto ASP's closed-world semantics.

## Descriptor configuration

Add a `"validator"` object to a property with `"kind": "asp"` and a `"relation"` path:

```json
{
  "id": "S1_ordered",
  "type": "INVARIANT",
  "validator": {
    "kind": "asp",
    "relation": "asp/sort_ordered.lp"
  }
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `kind` | yes | — | must be `"asp"` |
| `relation` | yes | — | classpath resource path to the `.lp` file |
| `violation` | no | `"violation"` | the query atom |
| `failureSignature` | no | value of `violation` | label recorded in `PropertyResult` on failure |
| `scale` | no | `1000` | fixed-point scale for `double` values: fact = round(value × scale) |

The `"relation"` value is resolved via the context classloader, so placing the file under `src/main/resources/asp/` makes it available at both test time and Gradle task time.

## Fact format

Facts are rendered by an `AspFactGeneratorProvider` matched to the suite's generator kind (e.g. `"intList"` uses `IntListAspFactGeneratorProvider`). The prefix is determined by the property kind:

**Invariant** (`type: INVARIANT`):

```
in(P, V).   % sourceInput[P] = V
out(P, V).  % sourceOutput[P] = V  (empty when SUT not executed)
```

**Metamorphic** (`type: METAMORPHIC`):

```
source_in(P, V).    % sourceInput[P]
source_out(P, V).   % sourceOutput[P]
followup_in(P, V).  % followUpInput[P]
followup_out(P, V). % followUpOutput[P]
```

For scalar types (e.g. `int`) the arity drops the position index:

```
in(V).   % int invariant input
out(V).  % int invariant output
```

For list types each position is a separate fact:

```
out(0, 3).  % out[0] = 3
out(1, 1).  % out[1] = 1
```

## Writing a `.lp` file

The file should contain rules that derive `violation` and a `#show violation/0.` directive. Do not include facts (those are prepended by the framework). Keep the file self-contained: no `#include` or external files.

Minimal skeleton:

```lp
% violation :- <condition that should not hold>.
#show violation/0.
```

A concrete example — output is non-decreasing:

```lp
violation :- out(P1, V1), out(P2, V2), P2 = P1 + 1, V1 > V2.
#show violation/0.
```

A metamorphic example — two outputs must be identical:

```lp
violation :- source_out(P, V1), followup_out(P, V2), V1 != V2.

source_has(P)   :- source_out(P, _).
followup_has(P) :- followup_out(P, _).

violation :- source_out(P, _),   not followup_has(P).
violation :- followup_out(P, _), not source_has(P).

#show violation/0.
```

## Availability check

`CliAspEngine.isAvailable()` checks whether the `clingo` binary is on `PATH`. Tests that need the ASP oracle should guard with `assumeTrue(new CliAspEngine().isAvailable())` so they are skipped cleanly when clingo is absent.

## Fact generator override

By default the fact generator kind matches the suite's generator kind (e.g. generator `"intList"` → fact generator `"intList"`). Override this at the suite level with `"factGenerator"` in the descriptor when the types differ:

```json
{
  "generator": { "kind": "customGen" },
  "factGenerator": "intList"
}
```

# classnames (Java)

A simple utility for conditionally joining class names together.

Java migration of [JedWatson/classnames](https://github.com/JedWatson/classnames)
at git commit [`abd6314`](https://github.com/JedWatson/classnames/commit/abd6314010de053a09df5acc53e474ff65cea470)
— see [Source provenance](#source-provenance--read-before-upgrading), which explains why
this is **not** the same as npm `classnames@2.5.1`.

Behavior is equivalent to that JavaScript source, including its handling of falsy values,
property ordering and whitespace. Equivalence is enforced by a differential test suite
that replays a recorded corpus (see
[Behavioral equivalence](#behavioral-equivalence)).

## Prerequisites

- **JDK 21** or newer
- **Maven 3.9** or newer
- **Node.js 18+** — only if you want to regenerate the equivalence baseline

## Install

```xml
<dependency>
  <groupId>com.github.jedwatson</groupId>
  <artifactId>classnames</artifactId>
  <version>2.6.0-SNAPSHOT</version>
</dependency>
```

The published artifact has **zero runtime dependencies**, matching the source package.

## Build, test, run

```bash
mvn clean verify     # compile, run all tests, package
mvn test             # tests only
mvn clean package    # produces target/classnames-2.6.0-SNAPSHOT.jar
```

## Usage

### `ClassNames` — the default variant

```java
import com.github.jedwatson.classnames.ClassNames;
import static com.github.jedwatson.classnames.ClassNames.classNames;

classNames("foo", "bar");                       // => "foo bar"
classNames("foo", Map.of("bar", true));         // => "foo bar"
classNames(Map.of("foo-bar", true));            // => "foo-bar"
classNames(Map.of("foo-bar", false));           // => ""
classNames(List.of("foo", "bar"));              // => "foo bar"
classNames("foo", null, false, 0, "bar");       // => "foo bar"
```

Falsy arguments are ignored. The falsy set follows JavaScript exactly:
`false`, `0`, `-0`, `""`, `null`, `NaN`. Everything else is truthy — including `" "`,
an empty map and an empty list.

Arrays and lists are flattened recursively:

```java
classNames(List.of("a", List.of("b", List.of("c", Map.of("d", true)))));  // => "a b c d"
```

An object that overrides `toString()` contributes that string:

```java
class Style { public String toString() { return "styled"; } }
classNames(new Style());   // => "styled"
```

### `ClassNamesBind` — CSS modules

The JavaScript package uses `classNames.bind(styles)`. Java has no `this`-rebinding for
static methods, so binding produces an instance instead:

```java
import com.github.jedwatson.classnames.ClassNamesBind;

Map<String, String> styles = Map.of("foo", "abc", "bar", "def");
ClassNamesBind cx = ClassNamesBind.bind(styles);

cx.apply("foo", Map.of("bar", true));   // => "abc def"
cx.apply("foo", "missing");             // => "abc missing"
```

Keys absent from the mapping — or mapped to a falsy value — fall through to the raw key,
matching the source's `this && this[key] || key`.

`ClassNamesBind.classNames(...)` is the unbound static form and behaves like
`ClassNames.classNames(...)`.

### `ClassNamesDedupe` — deduplicating variant

```java
import com.github.jedwatson.classnames.ClassNamesDedupe;

ClassNamesDedupe.classNames("foo", "bar", "foo");                  // => "foo bar"
ClassNamesDedupe.classNames("foo", Map.of("foo", false));          // => ""
ClassNamesDedupe.classNames("foo", Map.of("foo", false),
                            Map.of("foo", true, "bar", true));     // => "foo bar"
```

Note the last example: switching a class off and back on keeps its **original position**,
because the implementation records `false` rather than removing the key — the same
strategy as the source.

As in the JavaScript package, this variant splits string arguments on whitespace, so
`"foo bar"` contributes two separate class names.

## Java-specific notes

| Topic | Behavior |
|---|---|
| Object literals | Pass a `Map<String, ?>`. Use `LinkedHashMap` when key order matters. |
| Arrays | Both Java arrays and `List`/`Iterable` are accepted. |
| POJOs | Public instance fields are enumerated, superclass first. Getters are **not** invoked. |
| Key order | Reproduces JavaScript `for...in`: integer-like keys ascending first, then insertion order. |
| Errors | These methods never throw and never mutate their arguments. |

## Behavioral equivalence

The Java implementation is verified against the original JavaScript, not just against
hand-written expectations.

A 74-case corpus was executed against the real `index.js`, `bind.js` and `dedupe.js`,
recording **296 outputs**. Those recordings are committed as
`src/test/resources/corpus.json` and `expected.json`, and `DifferentialEquivalenceTest`
replays them in Java asserting byte-identical results. It runs as part of `mvn test` and
requires no Node.js.

The corpus covers the truthiness matrix, `for...in` key ordering, whitespace splitting,
`toString` dispatch, deep recursion, Unicode class names and dedupe toggling.

Regenerating the recordings requires the original JavaScript repository at the pinned
commit (see below) plus the `verification/generate-baseline.mjs` generator kept with the
migration tooling. **Do not regenerate against the npm-published `classnames`** — that
release behaves differently, as explained next.

## Source provenance — read before upgrading

This port was migrated from **git commit `abd6314`** of `JedWatson/classnames` (branch
`main`), *not* from the published npm release.

That commit's `package.json` still says `"version": "2.5.1"`, but it contains an
**unreleased refactor that changed behavior**:

| Call | npm `classnames@2.5.1` | git `abd6314` (this port) |
|---|---|---|
| `classNames(42)` | `"42"` | `""` |
| `classNames("a", 1)` | `"a 1"` | `"a"` |
| `dedupe(130)` | `"130"` | `""` |

Upstream dropped number support in that refactor — its own type tests assert
`expectError(classNames(42))`. This port follows the commit it was migrated from.

The artifact is therefore versioned **`2.6.0-SNAPSHOT`**, not `2.5.1`: publishing it as
`2.5.1` would promise npm-2.5.1 behavior it does not have.

### Known difference from the JavaScript source

None in output versus the migrated commit. One upstream quirk is **deliberately
preserved**: `ClassNamesDedupe.classNames("a ")` returns `"a "` — with a trailing space.
That is a defect in the JavaScript implementation, reproduced so the two agree.
See `TRUTH.md` and `MIGRATION-REPORT.md`.

## License

MIT — see [LICENSE](./LICENSE). Original work © Jed Watson.

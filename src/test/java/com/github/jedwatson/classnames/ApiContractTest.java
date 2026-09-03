package com.github.jedwatson.classnames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Migrated from the {@code tsd} type tests
 * ({@code tests/index.test-d.ts}, {@code tests/bind.test-d.ts},
 * {@code tests/dedupe.test-d.ts}).
 *
 * <p>Those files assert two different things:
 *
 * <ol>
 *   <li><strong>Accepted argument shapes.</strong> Migrated faithfully - each shape below
 *       both compiles and runs, which is a strictly stronger check than tsd performed.</li>
 *   <li><strong>Rejected argument shapes</strong>, via {@code expectError}:
 *       {@code classNames(42)} and {@code classNames(Symbol())}.
 *       <strong>This half cannot be preserved.</strong> See below.</li>
 * </ol>
 *
 * <h2>Known difference: compile-time rejection of numbers</h2>
 *
 * <p>The TypeScript declaration types the parameter as
 * {@code Value | Mapping | ArgumentArray}, which excludes {@code number} and
 * {@code symbol}, so {@code classNames(42)} is a <em>compile-time</em> error.
 *
 * <p>Java's variadic {@code Object...} cannot express that constraint. Narrowing the
 * signature to exclude numbers was rejected because it would break the accepted shapes -
 * numbers are legal <em>inside</em> arrays and as object <em>values</em>, and the source's
 * own runtime tests pass {@code 0} as a top-level argument
 * ({@code classNames('a', 0, null, undefined, false, 'b')}).
 *
 * <p><strong>Runtime behavior is identical:</strong> a number argument contributes nothing
 * and yields {@code ""}, verified below and in the differential corpus. Only the
 * compile-time diagnostic is lost. Recorded in MIGRATION-REPORT.md as difference (A).
 */
class ApiContractTest {

    private static Map<String, Object> obj(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    private static List<Object> arr(Object... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    @Nested
    @DisplayName("accepted argument shapes (tests/index.test-d.ts)")
    class AcceptedShapes {

        @Test
        @DisplayName("scalars, nulls and booleans are accepted")
        void scalars() {
            assertEquals("foo", ClassNames.classNames("foo"));
            assertEquals("", ClassNames.classNames((Object) null));
            assertEquals("", ClassNames.classNames(true));
            assertEquals("", ClassNames.classNames(false));
        }

        @Test
        @DisplayName("maps with assorted value types are accepted")
        void maps() {
            assertEquals("conditional", ClassNames.classNames(obj("conditional", true)));
            // `{ conditional: {} }` - an empty map is a truthy value.
            assertEquals("conditional", ClassNames.classNames(obj("conditional", obj())));
        }

        @Test
        @DisplayName("arrays, nested arrays and empty arrays are accepted")
        void arrays() {
            assertEquals("", ClassNames.classNames(arr()));
            assertEquals("bar", ClassNames.classNames(arr(arr("bar", null, null, true, false))));
            assertEquals("bar", ClassNames.classNames(arr("bar", null, null, true, false)));
        }

        @Test
        @DisplayName("mixed variadic arguments are accepted")
        void mixedVariadic() {
            assertEquals("bar", ClassNames.classNames("bar", null, null, true, false));
            assertEquals("bar abc foo", ClassNames.classNames("bar", arr("abc", obj("foo", true))));
            assertEquals("bar abc foo",
                    ClassNames.classNames("bar", arr("abc", obj("foo", true)), obj("def", false)));
            assertEquals("abc foo abc foo",
                    ClassNames.classNames("abc", true, false, null, null, obj("foo", true),
                            arr("abc", true, false, null, null, obj("foo", true))));
        }

        @Test
        @DisplayName("POJOs standing in for TypeScript type/interface shapes are accepted")
        void pojoShapes() {
            // Mirrors `type Foo = { bar: boolean }` and `interface IFoo { bar: boolean }`.
            assertEquals("bar", ClassNames.classNames(new Foo(true)));
            assertEquals("", ClassNames.classNames(new Foo(false)));
        }

        @Test
        @DisplayName("shapes cross-checked against tests/index.js")
        void mirrorsRuntimeTests() {
            assertEquals("c a b", ClassNames.classNames("c", arr("a", "b")));
            assertEquals("b", ClassNames.classNames("", "b", obj(), ""));
            assertEquals("a b", ClassNames.classNames("a", null, null, true, "b"));
            assertEquals("a", ClassNames.classNames("a", arr(arr())));
            assertEquals("a", ClassNames.classNames("a", arr()));
            assertEquals("", ClassNames.classNames(obj(), arr(obj(), arr())));
        }
    }

    @Nested
    @DisplayName("accepted argument shapes (tests/bind.test-d.ts)")
    class BindShapes {

        @Test
        @DisplayName("bind accepts a mapping, and bind(undefined) is tolerated")
        void bindAcceptsMappingOrNull() {
            ClassNamesBind bound = ClassNamesBind.bind(Map.of("foo", "bar"));
            assertEquals("bar", bound.apply("foo"));

            // Mirrors `bind.bind(undefined)` - must not throw.
            ClassNamesBind unbound = ClassNamesBind.bind(null);
            assertNotNull(unbound);
            assertEquals("foo", unbound.apply("foo"));
        }

        @Test
        @DisplayName("bound callable accepts the same shapes as the default export")
        void boundAcceptsAllShapes() {
            ClassNamesBind bound = ClassNamesBind.bind(Map.of("foo", "bar"));

            assertEquals("bar", bound.apply("foo"));
            assertEquals("", bound.apply((Object) null));
            assertEquals("", bound.apply(true));
            assertEquals("", bound.apply(false));
            assertEquals("conditional", bound.apply(obj("conditional", true)));
            assertEquals("", bound.apply(arr()));
            assertEquals("bar", bound.apply(arr(arr("foo", null, null, true, false))));
            assertEquals("abc bar", bound.apply("abc", arr("foo")));
        }
    }

    @Nested
    @DisplayName("accepted argument shapes (tests/dedupe.test-d.ts)")
    class DedupeShapes {

        @Test
        @DisplayName("dedupe accepts the same shapes as the default export")
        void acceptsAllShapes() {
            assertEquals("foo", ClassNamesDedupe.classNames("foo"));
            assertEquals("", ClassNamesDedupe.classNames((Object) null));
            assertEquals("", ClassNamesDedupe.classNames(true));
            assertEquals("", ClassNamesDedupe.classNames(false));
            assertEquals("conditional", ClassNamesDedupe.classNames(obj("conditional", true)));
            assertEquals("conditional", ClassNamesDedupe.classNames(obj("conditional", obj())));
            assertEquals("", ClassNamesDedupe.classNames(arr()));
            assertEquals("bar",
                    ClassNamesDedupe.classNames(arr(arr("bar", null, null, true, false))));
            assertEquals("bar", ClassNamesDedupe.classNames("bar", null, null, true, false));
            assertEquals("bar abc foo",
                    ClassNamesDedupe.classNames("bar", arr("abc", obj("foo", true))));
            assertEquals("bar abc foo", ClassNamesDedupe.classNames(
                    "bar", arr("abc", obj("foo", true)), obj("def", false)));
        }

        @Test
        @DisplayName("dedupe accepts POJO shapes")
        void pojoShapes() {
            assertEquals("bar", ClassNamesDedupe.classNames(new Foo(true)));
            assertEquals("", ClassNamesDedupe.classNames(new Foo(false)));
        }
    }

    @Nested
    @DisplayName("Java-only input shapes with no JavaScript counterpart")
    class JavaOnlyInputShapes {

        @Test
        @DisplayName("no arguments yields \"\" on every entry point")
        void noArguments() {
            assertEquals("", ClassNames.classNames());
            assertEquals("", ClassNamesBind.classNames());
            assertEquals("", ClassNamesDedupe.classNames());
            assertEquals("", ClassNamesBind.bind(Map.of("a", "#a")).apply());
        }

        @Test
        @DisplayName("a null varargs array yields \"\" rather than throwing")
        void nullVarargsArray() {
            // Java-only hazard: `classNames(null)` binds null to the varargs ARRAY, not
            // to a single argument. JavaScript cannot express this. Returning "" matches
            // what JavaScript produces for the closest equivalent, classNames().
            assertEquals("", ClassNames.classNames((Object[]) null));
            assertEquals("", ClassNamesBind.classNames((Object[]) null));
            assertEquals("", ClassNamesDedupe.classNames((Object[]) null));
            assertEquals("", ClassNamesBind.bind(Map.of("a", "#a")).apply((Object[]) null));
        }

        @Test
        @DisplayName("a Character argument behaves like a single-character string")
        void characterArgument() {
            // JavaScript has no char type; 'a' corresponds to the string "a".
            // Verified in Node: classNames("a") === "a"
            assertEquals("a", ClassNames.classNames('a'));
            assertEquals("a b", ClassNames.classNames('a', 'b'));
            assertEquals("a", ClassNamesBind.classNames('a'));
            assertEquals("a", ClassNamesDedupe.classNames('a'));
            assertEquals("a", ClassNamesDedupe.classNames('a', 'a'));
        }

        @Test
        @DisplayName("a bound Character argument is mapped like a string key")
        void boundCharacterArgument() {
            // Verified in Node: bind({a:'#a'})('a') === "#a"
            assertEquals("#a", ClassNamesBind.bind(Map.of("a", "#a")).apply('a'));
        }

        @Test
        @DisplayName("the bound variant enumerates POJO fields and maps them")
        void boundPojo() {
            // Verified in Node: bind({bar:'#bar'})({bar:true})  === "#bar"
            //                   bind({bar:'#bar'})({bar:false}) === ""
            ClassNamesBind cx = ClassNamesBind.bind(Map.of("bar", "#bar"));

            assertEquals("#bar", cx.apply(new Foo(true)));
            assertEquals("", cx.apply(new Foo(false)));
            assertEquals("bar", ClassNamesBind.classNames(new Foo(true)));
        }

        @Test
        @DisplayName("dedupe enumerates POJO fields")
        void dedupePojo() {
            assertEquals("bar", ClassNamesDedupe.classNames(new Foo(true), new Foo(true)));
        }
    }

    @Nested
    @DisplayName("rejected-at-compile-time shapes - runtime behavior only")
    class UnpreservableTypeErrors {

        @Test
        @DisplayName("a number argument yields \"\" at runtime, matching JavaScript")
        void numberArgument() {
            // tsd: expectError(classNames(42)) - a COMPILE error in TypeScript.
            // Java accepts it; the runtime result is identical to JavaScript's.
            assertEquals("", ClassNames.classNames(42));
            assertEquals("", ClassNamesBind.classNames(42));
            assertEquals("", ClassNamesDedupe.classNames(42));
        }

        @Test
        @DisplayName("number arguments follow git HEAD, NOT published npm 2.5.1")
        void numbersFollowMigrationSource() {
            // The migration source (commit abd6314) dropped number support that the
            // PUBLISHED npm 2.5.1 still has. Both were executed to confirm:
            //
            //   call                 npm 2.5.1   git HEAD (migrated)
            //   classNames(42)       "42"        ""
            //   classNames("a", 1)   "a 1"       "a"
            //   dedupe(130)          "130"       ""
            //
            // This test pins the git-HEAD behavior so the divergence can never drift
            // silently. See TRUTH.md "Source provenance".
            assertEquals("", ClassNames.classNames(42));
            assertEquals("a", ClassNames.classNames("a", 1));
            assertEquals("", ClassNamesDedupe.classNames(130));
            assertEquals("a", ClassNamesDedupe.classNames("a", 1));
        }

        @Test
        @DisplayName("numbers remain legal inside arrays and as object values")
        void numbersAreLegalElsewhere() {
            // Why the signature cannot simply exclude numbers.
            assertEquals("a b", ClassNames.classNames("a", 0, null, false, "b"));
            assertEquals("a b", ClassNames.classNames(arr("a", 0, null, false, true, "b")));
            assertEquals("f", ClassNames.classNames(obj("f", 1, "g", 0)));
        }
    }

    /** Stands in for the TypeScript `type Foo` / `interface IFoo` fixtures. */
    static class Foo {
        public final Object bar;

        Foo(Object bar) {
            this.bar = bar;
        }
    }
}

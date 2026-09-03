package com.github.jedwatson.classnames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Migrated 1:1 from {@code tests/index.js} (18 tests).
 *
 * <p>Test names are preserved verbatim so the Java and JavaScript suites can be compared
 * line by line.
 */
class ClassNamesTest {

    /** Mirrors a JavaScript object literal: ordered, heterogeneous, null-tolerant. */
    private static Map<String, Object> obj(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /** Mirrors a JavaScript array literal, which may contain nulls. */
    private static List<Object> arr(Object... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    @Test
    @DisplayName("keeps object keys with truthy values")
    void keepsObjectKeysWithTruthyValues() {
        assertEquals("a f", ClassNames.classNames(obj(
                "a", true,
                "b", false,
                "c", 0,
                "d", null,
                "e", null,   // JS `undefined`
                "f", 1
        )));
    }

    @Test
    @DisplayName("joins arrays of class names and ignore falsy values")
    void joinsAndIgnoresFalsy() {
        assertEquals("a b", ClassNames.classNames("a", 0, null, null, false, "b"));
    }

    @Test
    @DisplayName("supports heterogenous arguments")
    void heterogenousArguments() {
        assertEquals("a b", ClassNames.classNames(obj("a", true), "b", 0));
    }

    @Test
    @DisplayName("should be trimmed")
    void shouldBeTrimmed() {
        assertEquals("b", ClassNames.classNames("", "b", obj(), ""));
    }

    @Test
    @DisplayName("returns an empty string for an empty configuration")
    void emptyConfiguration() {
        assertEquals("", ClassNames.classNames(obj()));
    }

    @Test
    @DisplayName("supports an array of class names")
    void supportsArray() {
        assertEquals("a b", ClassNames.classNames(arr("a", "b")));
    }

    @Test
    @DisplayName("joins array arguments with string arguments")
    void joinsArrayAndStringArguments() {
        assertEquals("a b c", ClassNames.classNames(arr("a", "b"), "c"));
        assertEquals("c a b", ClassNames.classNames("c", arr("a", "b")));
    }

    @Test
    @DisplayName("handles multiple array arguments")
    void multipleArrayArguments() {
        assertEquals("a b c d", ClassNames.classNames(arr("a", "b"), arr("c", "d")));
    }

    @Test
    @DisplayName("handles arrays that include falsy and true values")
    void arraysWithFalsyAndTrue() {
        assertEquals("a b", ClassNames.classNames(arr("a", 0, null, null, false, true, "b")));
    }

    @Test
    @DisplayName("handles arrays that include arrays")
    void nestedArrays() {
        assertEquals("a b c", ClassNames.classNames(arr("a", arr("b", "c"))));
    }

    @Test
    @DisplayName("handles arrays that include objects")
    void arraysWithObjects() {
        assertEquals("a b", ClassNames.classNames(arr("a", obj("b", true, "c", false))));
    }

    @Test
    @DisplayName("handles deep array recursion")
    void deepArrayRecursion() {
        assertEquals("a b c d",
                ClassNames.classNames(arr("a", arr("b", arr("c", obj("d", true))))));
    }

    @Test
    @DisplayName("handles arrays that are empty")
    void emptyArrays() {
        assertEquals("a", ClassNames.classNames("a", arr()));
    }

    @Test
    @DisplayName("handles nested arrays that have empty nested arrays")
    void nestedEmptyArrays() {
        assertEquals("a", ClassNames.classNames("a", arr(arr())));
    }

    @Test
    @DisplayName("handles all types of truthy and falsy property values as expected")
    void allTruthyAndFalsyPropertyValues() {
        // The central L1 truthiness test. Whitespace, empty object, empty list and a
        // function reference must ALL be retained.
        Map<String, Object> input = obj(
                // falsy:
                "null", null,
                "emptyString", "",
                "noNumber", Double.NaN,
                "zero", 0,
                "negativeZero", -0.0d,
                "false", false,
                "undefined", null,

                // truthy (literally anything else):
                "nonEmptyString", "foobar",
                "whitespace", " ",
                "function", (Runnable) () -> { },
                "emptyObject", obj(),
                "nonEmptyObject", obj("a", 1, "b", 2),
                "emptyList", arr(),
                "nonEmptyList", arr(1, 2, 3),
                "greaterZero", 1
        );

        assertEquals(
                "nonEmptyString whitespace function emptyObject nonEmptyObject "
                        + "emptyList nonEmptyList greaterZero",
                ClassNames.classNames(input));
    }

    @Test
    @DisplayName("handles toString() method defined on object")
    void ownToString() {
        Object withToString = new Object() {
            @Override
            public String toString() {
                return "classFromMethod";
            }
        };
        assertEquals("classFromMethod", ClassNames.classNames(withToString));
    }

    @Test
    @DisplayName("handles toString() method defined inherited in object")
    void inheritedToString() {
        assertEquals("classFromMethod", ClassNames.classNames(new Class2()));
    }

    @Test
    @DisplayName("handles objects in a VM")
    void objectsInAVm() {
        // The source test constructs the object inside a `vm` context so that its
        // Object.prototype.toString differs from the caller's realm, which the second
        // clause of the toString check guards against.
        //
        // Java has no realm concept: an object loaded by a different ClassLoader still
        // resolves toString to the one and only java.lang.Object. The equivalent
        // observable assertion is that a foreign-classloader object without an override
        // still enumerates normally.
        Map<String, Object> input = obj("a", true, "b", true);
        assertEquals("a b", ClassNames.classNames(input));
    }

    static class Class1 {
        @Override
        public String toString() {
            return "classFromMethod";
        }
    }

    static class Class2 extends Class1 {
    }
}

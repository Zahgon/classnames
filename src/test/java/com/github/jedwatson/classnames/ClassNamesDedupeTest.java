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
 * Migrated 1:1 from {@code tests/dedupe.js} (17 tests).
 */
class ClassNamesDedupeTest {

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

    @Test
    @DisplayName("keeps object keys with truthy values")
    void keepsObjectKeysWithTruthyValues() {
        assertEquals("a f", ClassNamesDedupe.classNames(obj(
                "a", true, "b", false, "c", 0, "d", null, "e", null, "f", 1)));
    }

    @Test
    @DisplayName("should dedupe")
    void shouldDedupe() {
        assertEquals("foo bar", ClassNamesDedupe.classNames(
                "foo", "bar", "foo", "bar", obj("foo", true)));
    }

    @Test
    @DisplayName("should make sure subsequent objects can remove/add classes")
    void subsequentObjectsCanRemoveAndAdd() {
        // "foo" is switched off then back on, and keeps its ORIGINAL position.
        assertEquals("foo bar", ClassNamesDedupe.classNames(
                "foo", obj("foo", false), obj("foo", true, "bar", true)));
    }

    @Test
    @DisplayName("should make sure object with falsy value wipe out previous classes")
    void falsyValueWipesOutPreviousClasses() {
        assertEquals("b", ClassNamesDedupe.classNames(
                "foo foo", 0, null, null, false, "b", obj("foo", false)));
        assertEquals("foobar bar", ClassNamesDedupe.classNames(
                "foo", "foobar", "bar", obj("foo", false)));
        assertEquals("foo-bar bar", ClassNamesDedupe.classNames(
                "foo", "foo-bar", "bar", obj("foo", false)));
        assertEquals("-moz-foo-bar bar", ClassNamesDedupe.classNames(
                "foo", "-moz-foo-bar", "bar", obj("foo", false)));
    }

    @Test
    @DisplayName("joins arrays of class names and ignore falsy values")
    void joinsAndIgnoresFalsy() {
        assertEquals("a b", ClassNamesDedupe.classNames("a", 0, null, null, false, "b"));
    }

    @Test
    @DisplayName("supports heterogenous arguments")
    void heterogenousArguments() {
        assertEquals("a b", ClassNamesDedupe.classNames(obj("a", true), "b", 0));
    }

    @Test
    @DisplayName("should be trimmed")
    void shouldBeTrimmed() {
        assertEquals("b", ClassNamesDedupe.classNames("", "b", obj(), ""));
    }

    @Test
    @DisplayName("returns an empty string for an empty configuration")
    void emptyConfiguration() {
        assertEquals("", ClassNamesDedupe.classNames(obj()));
    }

    @Test
    @DisplayName("supports an array of class names")
    void supportsArray() {
        assertEquals("a b", ClassNamesDedupe.classNames(arr("a", "b")));
    }

    @Test
    @DisplayName("joins array arguments with string arguments")
    void joinsArrayAndStringArguments() {
        assertEquals("a b c", ClassNamesDedupe.classNames(arr("a", "b"), "c"));
        assertEquals("c a b", ClassNamesDedupe.classNames("c", arr("a", "b")));
    }

    @Test
    @DisplayName("handles multiple array arguments")
    void multipleArrayArguments() {
        assertEquals("a b c d", ClassNamesDedupe.classNames(arr("a", "b"), arr("c", "d")));
    }

    @Test
    @DisplayName("handles arrays that include falsy and true values")
    void arraysWithFalsyAndTrue() {
        assertEquals("a b",
                ClassNamesDedupe.classNames(arr("a", 0, null, null, false, true, "b")));
    }

    @Test
    @DisplayName("handles arrays that include arrays")
    void nestedArrays() {
        assertEquals("a b c", ClassNamesDedupe.classNames(arr("a", arr("b", "c"))));
    }

    @Test
    @DisplayName("handles arrays that include objects")
    void arraysWithObjects() {
        assertEquals("a b",
                ClassNamesDedupe.classNames(arr("a", obj("b", true, "c", false))));
    }

    @Test
    @DisplayName("handles deep array recursion")
    void deepArrayRecursion() {
        assertEquals("a b c d",
                ClassNamesDedupe.classNames(arr("a", arr("b", arr("c", obj("d", true))))));
    }

    @Test
    @DisplayName("handles own toString() method defined on object")
    void ownToString() {
        Object withToString = new Object() {
            @Override
            public String toString() {
                return "classFromMethod";
            }
        };
        assertEquals("classFromMethod", ClassNamesDedupe.classNames(withToString));
    }

    @Test
    @DisplayName("handles toString() method defined inherited in object")
    void inheritedToString() {
        assertEquals("classFromMethod", ClassNamesDedupe.classNames(new Class2()));
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

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

/**
 * Migrated 1:1 from {@code tests/bind.js} (28 tests: 13 unbound + 15 bound).
 *
 * <p>The source file's two {@code describe} blocks become two {@code @Nested} classes so
 * the structure stays comparable.
 */
class ClassNamesBindTest {

    /** Mirrors the source's `cssModulesMock` (tests/bind.js:5-12). */
    private static final Map<String, Object> CSS_MODULES_MOCK = Map.of(
            "a", "#a",
            "b", "#b",
            "c", "#c",
            "d", "#d",
            "e", "#e",
            "f", "#f"
    );

    private static final ClassNamesBind BOUND = ClassNamesBind.bind(CSS_MODULES_MOCK);

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
    @DisplayName("classNames")
    class Unbound {

        @Test
        @DisplayName("keeps object keys with truthy values")
        void keepsObjectKeysWithTruthyValues() {
            assertEquals("a f", ClassNamesBind.classNames(obj(
                    "a", true, "b", false, "c", 0, "d", null, "e", null, "f", 1)));
        }

        @Test
        @DisplayName("joins arrays of class names and ignore falsy values")
        void joinsAndIgnoresFalsy() {
            assertEquals("a b", ClassNamesBind.classNames("a", 0, null, null, false, "b"));
        }

        @Test
        @DisplayName("supports heterogenous arguments")
        void heterogenousArguments() {
            assertEquals("a b", ClassNamesBind.classNames(obj("a", true), "b", 0));
        }

        @Test
        @DisplayName("should be trimmed")
        void shouldBeTrimmed() {
            assertEquals("b", ClassNamesBind.classNames("", "b", obj(), ""));
        }

        @Test
        @DisplayName("returns an empty string for an empty configuration")
        void emptyConfiguration() {
            assertEquals("", ClassNamesBind.classNames(obj()));
        }

        @Test
        @DisplayName("supports an array of class names")
        void supportsArray() {
            assertEquals("a b", ClassNamesBind.classNames(arr("a", "b")));
        }

        @Test
        @DisplayName("joins array arguments with string arguments")
        void joinsArrayAndStringArguments() {
            assertEquals("a b c", ClassNamesBind.classNames(arr("a", "b"), "c"));
            assertEquals("c a b", ClassNamesBind.classNames("c", arr("a", "b")));
        }

        @Test
        @DisplayName("handles multiple array arguments")
        void multipleArrayArguments() {
            assertEquals("a b c d", ClassNamesBind.classNames(arr("a", "b"), arr("c", "d")));
        }

        @Test
        @DisplayName("handles arrays that include falsy and true values")
        void arraysWithFalsyAndTrue() {
            assertEquals("a b",
                    ClassNamesBind.classNames(arr("a", 0, null, null, false, true, "b")));
        }

        @Test
        @DisplayName("handles arrays that include arrays")
        void nestedArrays() {
            assertEquals("a b c", ClassNamesBind.classNames(arr("a", arr("b", "c"))));
        }

        @Test
        @DisplayName("handles arrays that include objects")
        void arraysWithObjects() {
            assertEquals("a b",
                    ClassNamesBind.classNames(arr("a", obj("b", true, "c", false))));
        }

        @Test
        @DisplayName("handles deep array recursion")
        void deepArrayRecursion() {
            assertEquals("a b c d",
                    ClassNamesBind.classNames(arr("a", arr("b", arr("c", obj("d", true))))));
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
            assertEquals("classFromMethod", ClassNamesBind.classNames(withToString));
        }
    }

    @Nested
    @DisplayName("classNamesBound")
    class Bound {

        @Test
        @DisplayName("keeps object keys with truthy values")
        void keepsObjectKeysWithTruthyValues() {
            assertEquals("#a #f", BOUND.apply(obj(
                    "a", true, "b", false, "c", 0, "d", null, "e", null, "f", 1)));
        }

        @Test
        @DisplayName("keeps class names undefined in bound hash")
        void keepsUnmappedClassNames() {
            // Keys absent from the mapping fall through to the raw key.
            assertEquals("#a #f x z", BOUND.apply(obj(
                    "a", true, "b", false, "c", 0, "d", null, "e", null, "f", 1,
                    "x", true, "y", null, "z", 1)));
        }

        @Test
        @DisplayName("joins arrays of class names and ignore falsy values")
        void joinsAndIgnoresFalsy() {
            assertEquals("#a #b", BOUND.apply("a", 0, null, null, false, 1, "b"));
        }

        @Test
        @DisplayName("supports heterogenous arguments")
        void heterogenousArguments() {
            assertEquals("#a #b", BOUND.apply(obj("a", true), "b", 0));
        }

        @Test
        @DisplayName("should be trimmed")
        void shouldBeTrimmed() {
            assertEquals("#b", BOUND.apply("", "b", obj(), ""));
        }

        @Test
        @DisplayName("returns an empty string for an empty configuration")
        void emptyConfiguration() {
            assertEquals("", BOUND.apply(obj()));
        }

        @Test
        @DisplayName("supports an array of class names")
        void supportsArray() {
            assertEquals("#a #b", BOUND.apply(arr("a", "b")));
        }

        @Test
        @DisplayName("joins array arguments with string arguments")
        void joinsArrayAndStringArguments() {
            assertEquals("#a #b #c", BOUND.apply(arr("a", "b"), "c"));
            assertEquals("#c #a #b", BOUND.apply("c", arr("a", "b")));
        }

        @Test
        @DisplayName("handles multiple array arguments")
        void multipleArrayArguments() {
            assertEquals("#a #b #c #d", BOUND.apply(arr("a", "b"), arr("c", "d")));
        }

        @Test
        @DisplayName("handles arrays that include falsy and true values")
        void arraysWithFalsyAndTrue() {
            assertEquals("#a #b", BOUND.apply(arr("a", 0, null, null, false, true, "b")));
        }

        @Test
        @DisplayName("handles arrays that include arrays")
        void nestedArrays() {
            assertEquals("#a #b #c", BOUND.apply(arr("a", arr("b", "c"))));
        }

        @Test
        @DisplayName("handles arrays that include objects")
        void arraysWithObjects() {
            assertEquals("#a #b", BOUND.apply(arr("a", obj("b", true, "c", false))));
        }

        @Test
        @DisplayName("handles deep array recursion")
        void deepArrayRecursion() {
            assertEquals("#a #b #c #d",
                    BOUND.apply(arr("a", arr("b", arr("c", obj("d", true))))));
        }

        @Test
        @DisplayName("handles own toString() method defined on object")
        void ownToString() {
            // The toString result is NOT passed through the mapping.
            Object withToString = new Object() {
                @Override
                public String toString() {
                    return "classFromMethod";
                }
            };
            assertEquals("classFromMethod", BOUND.apply(withToString));
        }

        @Test
        @DisplayName("handles toString() method defined inherited in object")
        void inheritedToString() {
            assertEquals("classFromMethod", BOUND.apply(new Class2()));
        }
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

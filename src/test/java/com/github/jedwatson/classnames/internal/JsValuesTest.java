package com.github.jedwatson.classnames.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins JavaScript truthiness (landmine L1) and custom-toString detection (L4).
 *
 * <p>These tests have no counterpart in the JavaScript suite because JavaScript gets this
 * behavior from the language itself. In Java it is hand-written, so it needs direct
 * coverage: a defect here would surface as a confusing failure in the business-logic
 * suites rather than as a clear semantics failure.
 *
 * <p>Expectations are taken from the source test
 * {@code tests/index.js:71-92} and from probes executed against the JavaScript runtime.
 */
class JsValuesTest {

    @Nested
    @DisplayName("isTruthy - the JavaScript falsy set")
    class Falsy {

        @Test
        @DisplayName("false is falsy")
        void booleanFalse() {
            assertFalse(JsValues.isTruthy(false));
        }

        @Test
        @DisplayName("null (JS null and undefined) is falsy")
        void nullValue() {
            assertFalse(JsValues.isTruthy(null));
        }

        @Test
        @DisplayName("empty string is falsy")
        void emptyString() {
            assertFalse(JsValues.isTruthy(""));
        }

        @Test
        @DisplayName("zero is falsy across numeric types")
        void zero() {
            assertFalse(JsValues.isTruthy(0));
            assertFalse(JsValues.isTruthy(0L));
            assertFalse(JsValues.isTruthy(0.0d));
            assertFalse(JsValues.isTruthy(0.0f));
            assertFalse(JsValues.isTruthy((short) 0));
            assertFalse(JsValues.isTruthy((byte) 0));
            assertFalse(JsValues.isTruthy(java.math.BigInteger.ZERO));
        }

        @Test
        @DisplayName("negative zero is falsy (source test pins 'negativeZero')")
        void negativeZero() {
            assertFalse(JsValues.isTruthy(-0.0d));
            assertFalse(JsValues.isTruthy(-0.0f));
        }

        @Test
        @DisplayName("NaN is falsy (source test pins 'noNumber')")
        void notANumber() {
            assertFalse(JsValues.isTruthy(Double.NaN));
            assertFalse(JsValues.isTruthy(Float.NaN));
        }
    }

    @Nested
    @DisplayName("isTruthy - everything else is truthy")
    class Truthy {

        @Test
        @DisplayName("whitespace string is truthy (source test pins 'whitespace')")
        void whitespace() {
            assertTrue(JsValues.isTruthy(" "));
        }

        @Test
        @DisplayName("empty map is truthy (source test pins 'emptyObject')")
        void emptyObject() {
            assertTrue(JsValues.isTruthy(new LinkedHashMap<String, Object>()));
        }

        @Test
        @DisplayName("empty list is truthy (source test pins 'emptyList')")
        void emptyList() {
            assertTrue(JsValues.isTruthy(new ArrayList<>()));
        }

        @Test
        @DisplayName("empty array is truthy")
        void emptyArray() {
            assertTrue(JsValues.isTruthy(new Object[0]));
            assertTrue(JsValues.isTruthy(new int[0]));
        }

        @Test
        @DisplayName("function reference is truthy (source test pins 'function')")
        void functionReference() {
            Supplier<String> fn = () -> "x";
            assertTrue(JsValues.isTruthy(fn));
        }

        @Test
        @DisplayName("non-zero numbers are truthy, including negatives")
        void nonZeroNumbers() {
            assertTrue(JsValues.isTruthy(1));
            assertTrue(JsValues.isTruthy(-1));
            assertTrue(JsValues.isTruthy(0.5d));
            assertTrue(JsValues.isTruthy(Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("non-zero floats are truthy, and infinities on both types")
        void nonZeroFloats() {
            assertTrue(JsValues.isTruthy(1.5f));
            assertTrue(JsValues.isTruthy(-1.5f));
            assertTrue(JsValues.isTruthy(Float.POSITIVE_INFINITY));
            assertTrue(JsValues.isTruthy(Float.NEGATIVE_INFINITY));
            assertTrue(JsValues.isTruthy(Double.NEGATIVE_INFINITY));
        }

        @Test
        @DisplayName("large integral types are truthy when non-zero")
        void integralTypes() {
            assertTrue(JsValues.isTruthy(java.math.BigInteger.ONE));
            assertTrue(JsValues.isTruthy(new java.math.BigDecimal("0.1")));
            assertTrue(JsValues.isTruthy((byte) 1));
            assertTrue(JsValues.isTruthy((short) 1));
            assertTrue(JsValues.isTruthy(1L));
        }

        @Test
        @DisplayName("true and non-empty strings are truthy")
        void obviousTruthy() {
            assertTrue(JsValues.isTruthy(true));
            assertTrue(JsValues.isTruthy("foobar"));
            assertTrue(JsValues.isTruthy("0"));
        }

        @Test
        @DisplayName("a Character is a non-empty string in JS terms, so always truthy")
        void characters() {
            // Java-only input shape: JavaScript has no char type, and a JS string is
            // falsy only when empty - which a Character can never be.
            assertTrue(JsValues.isTruthy('a'));
            assertTrue(JsValues.isTruthy('0'));
            assertTrue(JsValues.isTruthy('\0'));
        }

        @Test
        @DisplayName("isTruthy is not a null check")
        void notANullCheck() {
            // Guards the single most likely silent regression in this migration.
            assertFalse(JsValues.isTruthy(""));
            assertFalse(JsValues.isTruthy(0));
            assertFalse(JsValues.isTruthy(false));
        }
    }

    @Nested
    @DisplayName("isObjectLike - typeof arg === 'object'")
    class ObjectLike {

        @Test
        @DisplayName("maps, lists and arrays are object-like")
        void collections() {
            assertTrue(JsValues.isObjectLike(new LinkedHashMap<String, Object>()));
            assertTrue(JsValues.isObjectLike(List.of("a")));
            assertTrue(JsValues.isObjectLike(new String[]{"a"}));
        }

        @Test
        @DisplayName("numbers, booleans and strings are not object-like")
        void primitives() {
            assertFalse(JsValues.isObjectLike(42));
            assertFalse(JsValues.isObjectLike(true));
            assertFalse(JsValues.isObjectLike("a"));
        }

        @Test
        @DisplayName("a lambda is typeof 'function', not 'object'")
        void lambda() {
            Supplier<String> fn = () -> "x";
            assertFalse(JsValues.isObjectLike(fn));
        }

        @Test
        @DisplayName("a plain class instance is object-like")
        void pojo() {
            assertTrue(JsValues.isObjectLike(new Plain()));
        }

        @Test
        @DisplayName("a Character is a string in JS terms, not an object")
        void character() {
            assertFalse(JsValues.isObjectLike('a'));
        }

        @Test
        @DisplayName("primitive arrays are object-like")
        void primitiveArray() {
            assertTrue(JsValues.isObjectLike(new int[]{1}));
        }

        @Test
        @DisplayName("a non-functional synthetic instance is still object-like")
        void nonFunctionalInterfaceInstance() {
            // @FunctionalInterface is the discriminator, not syntheticness.
            NotFunctional instance = new NotFunctional() {
                @Override
                public void a() {
                }

                @Override
                public void b() {
                }
            };
            assertTrue(JsValues.isObjectLike(instance));
        }
    }

    @Nested
    @DisplayName("hasCustomToString - L4")
    class CustomToString {

        @Test
        @DisplayName("plain object without override is not custom")
        void noOverride() {
            assertFalse(JsValues.hasCustomToString(new Plain()));
        }

        @Test
        @DisplayName("own override is detected")
        void ownOverride() {
            assertTrue(JsValues.hasCustomToString(new Class1()));
        }

        @Test
        @DisplayName("inherited override is detected (Class2 extends Class1)")
        void inheritedOverride() {
            // Mirrors "handles toString() method defined inherited in object"
            // present in all three source suites.
            assertTrue(JsValues.hasCustomToString(new Class2()));
            assertEquals("classFromMethod", new Class2().toString());
        }

        @Test
        @DisplayName("a Map is data, not a toString override")
        void mapIsNotCustom() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", true);
            assertFalse(JsValues.hasCustomToString(map));
        }

        @Test
        @DisplayName("collections and arrays are never treated as toString overrides")
        void collectionsAreNotCustom() {
            // ArrayList and HashSet both override toString, but they model JS arrays
            // and must be flattened, not stringified.
            assertFalse(JsValues.hasCustomToString(new ArrayList<>(List.of("a"))));
            assertFalse(JsValues.hasCustomToString(new java.util.HashSet<>(List.of("a"))));
            assertFalse(JsValues.hasCustomToString(new String[]{"a"}));
            assertFalse(JsValues.hasCustomToString(new int[]{1}));
        }
    }

    interface NotFunctional {
        void a();

        void b();
    }

    @Nested
    @DisplayName("toJsString and toBoolean")
    class Coercion {

        @Test
        @DisplayName("null stringifies to \"null\", matching JavaScript String(null)")
        void nullStringifies() {
            assertEquals("null", JsValues.toJsString(null));
        }

        @Test
        @DisplayName("strings pass through and other values use String.valueOf")
        void stringifies() {
            assertEquals("abc", JsValues.toJsString("abc"));
            assertEquals("42", JsValues.toJsString(42));
            assertEquals("classFromMethod", JsValues.toJsString(new Class1()));
        }

        @Test
        @DisplayName("toBoolean mirrors the !! coercion used by dedupe")
        void doubleNegation() {
            assertTrue(JsValues.toBoolean("a"));
            assertFalse(JsValues.toBoolean(""));
            assertFalse(JsValues.toBoolean(0));
            assertFalse(JsValues.toBoolean(null));
        }
    }

    static class Plain {
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

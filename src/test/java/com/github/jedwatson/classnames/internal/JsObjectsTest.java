package com.github.jedwatson.classnames.internal;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins JavaScript {@code for...in} property ordering (landmine L2).
 *
 * <p>Enumeration order is observable in the produced class string, so it is a correctness
 * requirement. Expectations come from probes executed against the JavaScript runtime.
 */
class JsObjectsTest {

    @Test
    @DisplayName("integer-like keys come first in ascending numeric order, then insertion order")
    void jsEnumerationOrder() {
        // Verified in Node: Object.keys({b:1, 2:1, a:1, 1:1}) => ["1","2","b","a"]
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", true);
        map.put("2", true);
        map.put("a", true);
        map.put("1", true);

        assertEquals(List.of("1", "2", "b", "a"), JsObjects.ownKeys(map));
    }

    @Test
    @DisplayName("LinkedHashMap insertion order alone would be wrong")
    void insertionOrderIsNotEnough() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", true);
        map.put("2", true);
        map.put("a", true);
        map.put("1", true);

        // Documents precisely why LinkedHashMap cannot be used directly.
        assertEquals(List.of("b", "2", "a", "1"), List.copyOf(map.keySet()));
        assertEquals(List.of("1", "2", "b", "a"), JsObjects.ownKeys(map));
    }

    @Test
    @DisplayName("index keys sort numerically, not lexicographically")
    void numericNotLexicographic() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("10", true);
        map.put("9", true);
        map.put("100", true);

        // Lexicographic order would give ["10","100","9"].
        assertEquals(List.of("9", "10", "100"), JsObjects.ownKeys(map));
    }

    @Test
    @DisplayName("pure string keys keep insertion order")
    void stringKeysKeepInsertionOrder() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("zeta", true);
        map.put("alpha", true);
        map.put("mu", true);

        assertEquals(List.of("zeta", "alpha", "mu"), JsObjects.ownKeys(map));
    }

    @Test
    @DisplayName("non-canonical numeric-looking keys are ordinary string keys")
    void nonCanonicalKeys() {
        assertTrue(JsObjects.isArrayIndex("0"));
        assertTrue(JsObjects.isArrayIndex("42"));

        assertFalse(JsObjects.isArrayIndex("01"));   // leading zero
        assertFalse(JsObjects.isArrayIndex("-1"));   // negative
        assertFalse(JsObjects.isArrayIndex("1.5"));  // not an integer
        assertFalse(JsObjects.isArrayIndex(" 1"));   // whitespace
        assertFalse(JsObjects.isArrayIndex("+1"));   // sign
        assertFalse(JsObjects.isArrayIndex(""));     // empty
        assertFalse(JsObjects.isArrayIndex("4294967295")); // 2^32-1 is excluded
    }

    @Test
    @DisplayName("non-canonical keys enumerate in insertion order, after real indices")
    void nonCanonicalOrdering() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("01", true);
        map.put("2", true);
        map.put("-1", true);
        map.put("0", true);

        assertEquals(List.of("0", "2", "01", "-1"), JsObjects.ownKeys(map));
    }

    @Test
    @DisplayName("arrayElements handles lists, object arrays and primitive arrays")
    void arrayElements() {
        assertEquals(List.of("a", "b"), JsObjects.arrayElements(List.of("a", "b")));
        assertEquals(List.of("a", "b"), JsObjects.arrayElements(new String[]{"a", "b"}));
        assertEquals(List.of(1, 2, 3), JsObjects.arrayElements(new int[]{1, 2, 3}));
    }

    @Test
    @DisplayName("propertiesOf reads public fields, superclass first (L5 decision)")
    void propertiesOfPojo() {
        Map<String, Object> properties = JsObjects.propertiesOf(new Child());

        assertEquals(List.of("inherited", "own"), List.copyOf(properties.keySet()));
        assertEquals(true, properties.get("inherited"));
        assertEquals(false, properties.get("own"));
    }

    @Test
    @DisplayName("propertiesOf ignores private and static fields, and does not call getters")
    void propertiesOfIgnoresNonPublicData() {
        Map<String, Object> properties = JsObjects.propertiesOf(new WithHidden());

        assertEquals(List.of("visible"), List.copyOf(properties.keySet()));
    }

    @Test
    @DisplayName("non-String map keys are coerced, and order by their coerced form")
    void nonStringKeys() {
        // A Map<Integer, ?> is a plausible migration of {1: true}. Verified in Node:
        // classNames({2:true, 1:true, 10:true}) === "1 2 10"
        Map<Integer, Object> map = new LinkedHashMap<>();
        map.put(2, true);
        map.put(1, true);
        map.put(10, true);

        assertEquals(List.of("1", "2", "10"), JsObjects.ownKeys(map));
        assertEquals(Boolean.TRUE, JsObjects.get(map, "1"));
        assertEquals(Boolean.TRUE, JsObjects.get(map, "10"));
    }

    @Test
    @DisplayName("a null map key coerces to \"null\", matching JavaScript")
    void nullKey() {
        // Verified in Node: classNames({null: true, a: true}) === "null a"
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(null, true);
        map.put("a", true);

        assertEquals(List.of("null", "a"), JsObjects.ownKeys(map));
        assertEquals(Boolean.TRUE, JsObjects.get(map, "null"));
    }

    @Test
    @DisplayName("get returns null for an absent key")
    void absentKey() {
        assertNull(JsObjects.get(Map.of("a", true), "missing"));
    }

    @Test
    @DisplayName("a non-List Iterable is treated as an array")
    void nonListIterable() {
        Set<Object> set = new LinkedHashSet<>(List.of("a", "b"));

        assertTrue(JsObjects.isArrayLike(set));
        assertEquals(List.of("a", "b"), JsObjects.arrayElements(set));
    }

    @Test
    @DisplayName("a Map is object-like, not array-like")
    void mapIsNotArrayLike() {
        assertFalse(JsObjects.isArrayLike(Map.of("a", true)));
    }

    @Test
    @DisplayName("a type that is both Map and Iterable is treated as an object")
    void mapAndIterableIsObject() {
        // java.util.Map does not extend Iterable, so the Map guard in isArrayLike only
        // matters for a caller-supplied type implementing both. Such a value models a JS
        // object literal and must be enumerated, not flattened.
        assertFalse(JsObjects.isArrayLike(new MapAndIterable()));
        assertEquals(List.of("a"), JsObjects.ownKeys(new MapAndIterable()));
    }

    /** A caller-supplied shape that is simultaneously a Map and an Iterable. */
    static class MapAndIterable extends LinkedHashMap<String, Object>
            implements Iterable<Object> {

        private static final long serialVersionUID = 1L;

        MapAndIterable() {
            put("a", true);
        }

        @Override
        public java.util.Iterator<Object> iterator() {
            return values().iterator();
        }
    }

    @Test
    @DisplayName("arrayElements rejects a value that is not array-like")
    void arrayElementsRejectsNonArray() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> JsObjects.arrayElements("not an array"));
        assertTrue(error.getMessage().contains("String"));
    }

    static class Parent {
        public Object inherited = true;
    }

    static class Child extends Parent {
        public Object own = false;
    }

    @SuppressWarnings("unused")
    static class WithHidden {
        public Object visible = true;
        private Object hidden = true;
        public static Object shared = true;

        public Object getComputed() {
            throw new AssertionError("getters must never be invoked");
        }
    }
}

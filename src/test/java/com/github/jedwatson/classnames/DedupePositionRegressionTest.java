package com.github.jedwatson.classnames;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for {@code dedupe.js:65-67} — "set value to false instead of deleting
 * it to avoid changing object structure".
 *
 * <p>These are <strong>additional</strong> to the 17 tests migrated 1:1 from
 * {@code tests/dedupe.js}, which live in {@link ClassNamesDedupeTest}.
 *
 * <h2>Why this file exists</h2>
 *
 * <p>A mutation test replaced {@code classSet.put(key, false)} with
 * {@code classSet.remove(key)} — discarding the very behavior the source comments call
 * out — and <strong>the entire migrated suite still passed</strong>.
 *
 * <p>The upstream case
 * {@code dedupe('foo', {foo:false}, {foo:true, bar:true})} cannot detect the difference:
 * whether {@code foo} keeps its original slot or is removed and re-appended, it still
 * lands before {@code bar}, so both implementations return {@code "foo bar"}.
 *
 * <p>Distinguishing the two requires a class that is switched off and back on while
 * <em>another class sits between it and the revival</em>. Expected values below were
 * produced by executing the JavaScript source at commit {@code abd6314}.
 */
class DedupePositionRegressionTest {

    private static Map<String, Object> obj(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("a revived class keeps its ORIGINAL position, not a re-appended one")
    void revivedClassKeepsOriginalPosition() {
        // Node: dedupe('foo','bar',{foo:false},{foo:true}) === "foo bar"
        // A remove-then-reinsert implementation would yield "bar foo".
        assertEquals("foo bar",
                ClassNamesDedupe.classNames("foo", "bar", obj("foo", false), obj("foo", true)));
    }

    @Test
    @DisplayName("position is preserved across several intervening classes")
    void positionPreservedAcrossMultipleClasses() {
        // Node: dedupe('a','b','c',{a:false},{a:true}) === "a b c"
        // A remove-then-reinsert implementation would yield "b c a".
        assertEquals("a b c",
                ClassNamesDedupe.classNames("a", "b", "c", obj("a", false), obj("a", true)));
    }

    @Test
    @DisplayName("position is preserved when the class originated in an object")
    void positionPreservedForObjectSourcedClass() {
        // Node: dedupe({a:true,b:true},{a:false},{a:true}) === "a b"
        // A remove-then-reinsert implementation would yield "b a".
        assertEquals("a b",
                ClassNamesDedupe.classNames(obj("a", true, "b", true), obj("a", false), obj("a", true)));
    }

    @Test
    @DisplayName("a class switched off and left off is dropped, not reordered")
    void switchedOffClassIsDropped() {
        assertEquals("bar",
                ClassNamesDedupe.classNames("foo", "bar", obj("foo", false)));
    }

    @Test
    @DisplayName("repeated toggling settles on the final state, keeping position")
    void repeatedToggling() {
        assertEquals("foo bar", ClassNamesDedupe.classNames(
                "foo", "bar", obj("foo", false), obj("foo", true),
                obj("foo", false), obj("foo", true)));
    }
}

package com.github.jedwatson.classnames;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.jedwatson.classnames.internal.JsObjects;
import com.github.jedwatson.classnames.internal.JsValues;

/**
 * The deduplicating variant: each class name appears at most once in the output.
 *
 * <p>Java migration of {@code dedupe.js} from
 * <a href="https://github.com/JedWatson/classnames">JedWatson/classnames</a> v2.5.1.
 *
 * <pre>{@code
 * ClassNamesDedupe.classNames("foo", "bar", "foo");              // => "foo bar"
 * ClassNamesDedupe.classNames("foo", Map.of("foo", false));      // => ""
 * }</pre>
 *
 * <p>Later arguments can switch an earlier class off <em>and back on</em> without moving
 * it: the source records {@code false} rather than deleting the key, so its original
 * position survives. See {@link #classNames(Object...)}.
 */
public final class ClassNamesDedupe {

    /** Migrated from {@code dedupe.js:33} - {@code const SPACE = /\s+/;} */
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private ClassNamesDedupe() {
    }

    /**
     * Joins the truthy class names produced by the given arguments, deduplicated.
     *
     * <p>Migrated from {@code dedupe.js:5-18}.
     *
     * <p>The accumulator is a {@link LinkedHashMap}, which is the correct model for the
     * source's null-prototype {@code StorageObject}: re-putting an existing key updates
     * the value while <strong>keeping the original insertion position</strong>. That is
     * what makes the source's "set false, never delete" strategy
     * ({@code dedupe.js:65-67}) observable:
     * <pre>{@code
     * classNames("foo", Map.of("foo", false), Map.of("foo", true, "bar", true))
     * // => "foo bar"   (not "bar foo")
     * }</pre>
     *
     * <p>Key <em>enumeration</em> still goes through {@link JsObjects#ownKeys(Map)},
     * because the final loop is a {@code for...in} and integer-like class names must
     * enumerate before string ones.
     *
     * @param args any number of strings, maps, arrays/lists, or objects
     * @return the joined, deduplicated class names
     */
    public static String classNames(Object... args) {
        Map<String, Boolean> classSet = new LinkedHashMap<>();

        if (args != null) {
            appendArray(classSet, java.util.Arrays.asList(args));
        }

        String classes = "";

        for (String key : JsObjects.ownKeys(classSet)) {
            if (Boolean.TRUE.equals(classSet.get(key))) {
                classes += classes.isEmpty() ? key : " " + key;
            }
        }

        return classes;
    }

    /** Migrated from {@code dedupe.js:20-31}. */
    private static void appendValue(Map<String, Boolean> classSet, Object arg) {
        // dedupe.js:21 - `if (!arg) return;`
        if (!JsValues.isTruthy(arg)) {
            return;
        }

        if (arg instanceof CharSequence || arg instanceof Character) {
            appendString(classSet, arg.toString());
        } else if (JsObjects.isArrayLike(arg)) {
            appendArray(classSet, JsObjects.arrayElements(arg));
        } else if (JsValues.isObjectLike(arg)) {
            appendObject(classSet, arg);
        }
        // Numbers, booleans and functions fall through silently, matching the source's
        // if/else-if chain which has no terminal else.
    }

    /**
     * Migrated from {@code dedupe.js:35-42}.
     *
     * <p>LANDMINE L3: JavaScript's {@code String.prototype.split} keeps trailing empty
     * strings; Java's single-argument {@code String.split} discards them. The explicit
     * {@code -1} limit restores JavaScript behavior.
     *
     * <p>This matters because it is externally observable:
     * <pre>{@code
     * "a ".split(/\s+/)   // JS   => ["a", ""]
     * "a ".split("\\s+")  // Java => ["a"]        <- diverges
     * SPACE.split("a ",-1)         => ["a", ""]   <- matches
     * }</pre>
     *
     * <p>The empty token is then stored as a class name, and the output loop appends a
     * separator for it, so {@code classNames("a ")} returns {@code "a "} - with a trailing
     * space. That is a defect in the upstream package, reproduced here deliberately
     * rather than silently corrected. Correcting it would be an unauthorized behavior
     * change; it is recorded in the migration report instead.
     */
    private static void appendString(Map<String, Boolean> classSet, String str) {
        String[] array = SPACE.split(str, -1);
        for (String token : array) {
            classSet.put(token, Boolean.TRUE);
        }
    }

    /** Migrated from {@code dedupe.js:44-50}. */
    private static void appendArray(Map<String, Boolean> classSet, List<Object> array) {
        for (Object element : array) {
            appendValue(classSet, element);
        }
    }

    /** Migrated from {@code dedupe.js:54-70}. */
    private static void appendObject(Map<String, Boolean> classSet, Object object) {
        // dedupe.js:55-61 - a custom toString short-circuits enumeration entirely.
        if (JsValues.hasCustomToString(object)) {
            classSet.put(JsValues.toJsString(object), Boolean.TRUE);
            return;
        }

        Map<?, ?> properties = object instanceof Map<?, ?> map
                ? map
                : JsObjects.propertiesOf(object);

        for (String key : JsObjects.ownKeys(properties)) {
            // dedupe.js:67 - `classSet[k] = !!object[k];`
            // Set to false rather than removing, so the key keeps its original position
            // and a later argument can switch it back on in place.
            classSet.put(key, JsValues.toBoolean(JsObjects.get(properties, key)));
        }
    }
}

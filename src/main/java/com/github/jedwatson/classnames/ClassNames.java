package com.github.jedwatson.classnames;

import java.util.List;
import java.util.Map;

import com.github.jedwatson.classnames.internal.JsObjects;
import com.github.jedwatson.classnames.internal.JsValues;

/**
 * A simple utility for conditionally joining class names together.
 *
 * <p>Java migration of {@code index.js} from
 * <a href="https://github.com/JedWatson/classnames">JedWatson/classnames</a> v2.5.1.
 *
 * <p>Accepts any number of heterogeneous arguments and joins the resulting class names
 * with a single space:
 * <pre>{@code
 * ClassNames.classNames("foo", "bar");                 // => "foo bar"
 * ClassNames.classNames("foo", Map.of("bar", true));   // => "foo bar"
 * ClassNames.classNames(List.of("foo", "bar"));        // => "foo bar"
 * }</pre>
 *
 * <p>Falsy arguments are ignored, matching JavaScript truthiness exactly - see
 * {@link JsValues#isTruthy(Object)}. This method never throws and never mutates its
 * arguments, mirroring the source.
 */
public final class ClassNames {

    private ClassNames() {
    }

    /**
     * Joins the truthy class names produced by the given arguments.
     *
     * <p>Migrated from {@code index.js:3-14}.
     *
     * @param args any number of strings, maps, arrays/lists, or objects; may be empty,
     *             and individual elements may be {@code null}
     * @return the joined class names, or an empty string when nothing accumulates
     */
    public static String classNames(Object... args) {
        String classes = "";

        if (args == null) {
            // A bare `classNames(null)` in Java binds null to the varargs array itself
            // rather than to a single argument. JavaScript would treat that call as a
            // single falsy argument, which produces "" - the same result.
            return classes;
        }

        for (Object arg : args) {
            if (JsValues.isTruthy(arg)) {
                classes = appendClass(classes, parseValue(arg));
            }
        }

        return classes;
    }

    /**
     * Migrated from {@code index.js:16-42}.
     *
     * <p>Branch order is preserved exactly, because the branches overlap: an array is
     * also object-like, and an object with a custom {@code toString} is also enumerable.
     * Reordering them would change the output.
     */
    private static String parseValue(Object arg) {
        // index.js:17-19 - typeof arg === 'string'
        if (arg instanceof CharSequence || arg instanceof Character) {
            return arg.toString();
        }

        // index.js:21-23 - typeof arg !== 'object' returns ''.
        // Numbers, booleans and functions therefore contribute nothing.
        if (!JsValues.isObjectLike(arg)) {
            return "";
        }

        // index.js:25-27 - Array.isArray(arg) recurses through the same entry point.
        if (JsObjects.isArrayLike(arg)) {
            List<Object> elements = JsObjects.arrayElements(arg);
            return classNames(elements.toArray());
        }

        // index.js:29-31 - an own/inherited toString override wins over enumeration.
        if (JsValues.hasCustomToString(arg)) {
            return JsValues.toJsString(arg);
        }

        // index.js:33-41 - enumerate own keys, keeping those with truthy values.
        Map<?, ?> properties = arg instanceof Map<?, ?> map
                ? map
                : JsObjects.propertiesOf(arg);

        String classes = "";
        for (String key : JsObjects.ownKeys(properties)) {
            if (JsValues.isTruthy(JsObjects.get(properties, key))) {
                classes = appendClass(classes, key);
            }
        }

        return classes;
    }

    /**
     * Migrated from {@code index.js:44-50}.
     *
     * <p>Skipping falsy additions is what keeps the result free of leading, trailing and
     * doubled spaces without a trailing {@code trim()}.
     */
    private static String appendClass(String value, String newClass) {
        if (!JsValues.isTruthy(newClass)) {
            return value;
        }
        return JsValues.isTruthy(value) ? value + " " + newClass : newClass;
    }
}

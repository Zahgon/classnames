package com.github.jedwatson.classnames;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jedwatson.classnames.internal.JsObjects;
import com.github.jedwatson.classnames.internal.JsValues;

/**
 * The CSS-modules variant: class names are looked up through a bound mapping.
 *
 * <p>Java migration of {@code bind.js} from
 * <a href="https://github.com/JedWatson/classnames">JedWatson/classnames</a> v2.5.1.
 *
 * <p>In JavaScript this variant is used via {@code Function.prototype.bind}:
 * <pre>{@code
 * const cx = classNames.bind(styles);
 * cx('a', { b: true });   // => styles.a + ' ' + styles.b
 * }</pre>
 *
 * <p>Java has no {@code this}-rebinding for static methods, so binding produces an
 * <em>instance</em> instead:
 * <pre>{@code
 * ClassNamesBind cx = ClassNamesBind.bind(styles);
 * cx.apply("a", Map.of("b", true));
 * }</pre>
 *
 * <p>Classified as an accepted difference of type (A): the calling syntax differs because
 * Java requires it, but the observable result - one bound callable per mapping - is
 * identical. The unbound {@link #classNames(Object...)} static entry point is preserved
 * and behaves exactly like {@code bind.js} called without a receiver.
 */
public final class ClassNamesBind {

    /**
     * The bound mapping, corresponding to {@code this} in {@code bind.js}.
     * Empty for the unbound static entry point, matching {@code this === undefined}.
     */
    private final Map<String, Object> bindings;

    private ClassNamesBind(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    /**
     * Creates a bound instance, equivalent to {@code classNames.bind(mapping)}.
     *
     * @param mapping class-name mapping, typically a CSS-modules styles object
     * @return a bound callable
     */
    public static ClassNamesBind bind(Map<String, ?> mapping) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (mapping != null) {
            copy.putAll(mapping);
        }
        // Defensive copy: the source captures `this` by reference, but a JS caller cannot
        // observe mutation through the bound function either, so copying preserves
        // behavior while removing a Java-specific aliasing hazard.
        return new ClassNamesBind(Collections.unmodifiableMap(copy));
    }

    /**
     * Invokes the bound variant.
     *
     * <p>Migrated from {@code bind.js:3-14} with {@code this} set to the bound mapping.
     *
     * @param args any number of strings, maps, arrays/lists, or objects
     * @return the joined, mapped class names
     */
    public String apply(Object... args) {
        return join(this.bindings, args);
    }

    /**
     * Invokes the unbound variant, equivalent to calling {@code bind.js} with no receiver.
     *
     * <p>With {@code this} undefined, {@code this && this[arg] || arg} degrades to
     * {@code arg}, so this behaves identically to {@link ClassNames#classNames(Object...)}.
     *
     * @param args any number of strings, maps, arrays/lists, or objects
     * @return the joined class names
     */
    public static String classNames(Object... args) {
        return join(Collections.emptyMap(), args);
    }

    /** Migrated from {@code bind.js:3-14}. */
    private static String join(Map<String, Object> bindings, Object... args) {
        String classes = "";

        if (args == null) {
            return classes;
        }

        for (Object arg : args) {
            if (JsValues.isTruthy(arg)) {
                classes = appendClass(classes, parseValue(bindings, arg));
            }
        }

        return classes;
    }

    /** Migrated from {@code bind.js:16-42}. */
    private static String parseValue(Map<String, Object> bindings, Object arg) {
        // bind.js:17-19 - `return this && this[arg] || arg;`
        if (arg instanceof CharSequence || arg instanceof Character) {
            return lookup(bindings, arg.toString());
        }

        // bind.js:21-23
        if (!JsValues.isObjectLike(arg)) {
            return "";
        }

        // bind.js:25-27 - recursion propagates `this` via classNames.apply(this, arg).
        if (JsObjects.isArrayLike(arg)) {
            List<Object> elements = JsObjects.arrayElements(arg);
            return join(bindings, elements.toArray());
        }

        // bind.js:29-31 - a custom toString result is NOT passed through the mapping.
        // Pinned by tests/bind.js:152-163.
        if (JsValues.hasCustomToString(arg)) {
            return JsValues.toJsString(arg);
        }

        // bind.js:33-41 - each retained key IS passed through the mapping.
        Map<?, ?> properties = arg instanceof Map<?, ?> map
                ? map
                : JsObjects.propertiesOf(arg);

        String classes = "";
        for (String key : JsObjects.ownKeys(properties)) {
            if (JsValues.isTruthy(JsObjects.get(properties, key))) {
                classes = appendClass(classes, lookup(bindings, key));
            }
        }

        return classes;
    }

    /**
     * Reproduces {@code this && this[key] || key} from {@code bind.js:18,37}.
     *
     * <p>The {@code ||} fallback is load-bearing: when the mapping holds a <em>falsy</em>
     * value for the key (an empty string, {@code 0}, {@code false}), JavaScript falls back
     * to the raw key rather than emitting the falsy value. A plain
     * {@code map.getOrDefault(key, key)} would be wrong, because it only falls back on
     * absence, not on falsiness.
     */
    private static String lookup(Map<String, Object> bindings, String key) {
        if (bindings.isEmpty()) {
            return key;
        }
        Object mapped = bindings.get(key);
        return JsValues.isTruthy(mapped) ? JsValues.toJsString(mapped) : key;
    }

    /** Migrated from {@code bind.js:44-50}. */
    private static String appendClass(String value, String newClass) {
        if (!JsValues.isTruthy(newClass)) {
            return value;
        }
        return JsValues.isTruthy(value) ? value + " " + newClass : newClass;
    }
}

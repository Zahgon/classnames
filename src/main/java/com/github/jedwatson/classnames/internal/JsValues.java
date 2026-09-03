package com.github.jedwatson.classnames.internal;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * JavaScript value semantics that Java does not provide natively.
 *
 * <p>The source package relies on JavaScript truthiness and duck-typed {@code toString}
 * detection on nearly every line. Java has no equivalent for either, so both are
 * reproduced explicitly here rather than approximated at each call site.
 *
 * <p>This class exists because the semantics are <em>implicit</em> in JavaScript and must
 * become <em>explicit</em> in Java. It is not an added abstraction layer.
 */
public final class JsValues {

    private JsValues() {
    }

    /**
     * Reproduces JavaScript truthiness: {@code if (value)}.
     *
     * <p>The JavaScript falsy set is exactly:
     * {@code false}, {@code 0}, {@code -0}, {@code 0n}, {@code ""}, {@code null},
     * {@code undefined}, {@code NaN}. <strong>Every other value is truthy</strong>,
     * including {@code " "} (whitespace), {@code {}} (empty object), {@code []}
     * (empty array) and function references.
     *
     * <p>This behavior is pinned by the source test
     * {@code tests/index.js:71-92} ("handles all types of truthy and falsy property
     * values as expected"), which asserts that an empty object, an empty list, a
     * whitespace string and a function reference are all retained.
     *
     * <p>Note in particular that this is <strong>not</strong> a null check. Substituting
     * {@code value != null} would wrongly keep {@code ""}, {@code 0} and {@code false}.
     *
     * @param value any value, may be {@code null}
     * @return {@code true} when JavaScript would treat the value as truthy
     */
    public static boolean isTruthy(Object value) {
        // null covers both JS `null` and JS `undefined`. Both are falsy, and neither is
        // distinguishable from the other in any observable way in this library.
        if (value == null) {
            return false;
        }

        if (value instanceof Boolean b) {
            return b;
        }

        if (value instanceof CharSequence cs) {
            // "" is falsy; " " is truthy.
            return cs.length() != 0;
        }

        if (value instanceof Character c) {
            // A JS single-character string is falsy only when empty, which a Character
            // can never be. '\0' is a non-empty string in JS terms, therefore truthy.
            return true;
        }

        if (value instanceof Double d) {
            // Covers 0.0, -0.0 and NaN in one expression:
            // NaN != NaN is true, and -0.0 == 0.0 is true.
            return d != 0.0d && !d.isNaN();
        }

        if (value instanceof Float f) {
            return f != 0.0f && !f.isNaN();
        }

        if (value instanceof Number n) {
            // Integral types (Byte, Short, Integer, Long, BigInteger, BigDecimal...).
            // BigInteger.ZERO maps to JS `0n`, which is falsy.
            return n.doubleValue() != 0.0d;
        }

        // Everything else - Map, List, arrays, POJOs, functional interfaces - is an
        // object reference in JS terms and is therefore truthy regardless of contents.
        return true;
    }

    /**
     * Reproduces the JavaScript {@code !!value} double-negation coercion.
     *
     * <p>Used by {@code dedupe.js:67} where object values are stored as explicit
     * booleans rather than deleted, so a later argument can revive an earlier key
     * without changing its position.
     *
     * @param value any value, may be {@code null}
     * @return the truthiness of the value as a boxed Boolean
     */
    public static boolean toBoolean(Object value) {
        return isTruthy(value);
    }

    /**
     * Determines whether a value should be treated as a JavaScript "object" for the
     * purposes of {@code typeof arg === 'object'}.
     *
     * <p>In JavaScript, {@code typeof} returns {@code 'object'} for plain objects, arrays
     * and class instances, but {@code 'function'} for functions and {@code 'number'} /
     * {@code 'boolean'} for primitives. The source returns {@code ''} for anything that
     * is neither a string nor an object, so numbers, booleans and functions passed
     * directly as arguments produce no output.
     *
     * <p>Verified against the source runtime:
     * {@code classNames(42) === ''} and {@code classNames(function(){}) === ''}.
     *
     * @param value a non-null value
     * @return {@code true} when JavaScript {@code typeof} would yield {@code 'object'}
     */
    public static boolean isObjectLike(Object value) {
        if (value instanceof Map || value instanceof Iterable) {
            return true;
        }
        if (value.getClass().isArray()) {
            return true;
        }
        // Primitives and their boxes are `typeof 'number'` / `'boolean'` - not objects.
        if (value instanceof Number || value instanceof Boolean
                || value instanceof CharSequence || value instanceof Character) {
            return false;
        }
        // A lambda / method reference is `typeof 'function'` in JS, which is NOT
        // 'object', so it must produce '' when passed as a top-level argument.
        if (isFunctionLike(value)) {
            return false;
        }
        // Remaining reference types are class instances - `typeof 'object'`.
        return true;
    }

    /**
     * Detects values that correspond to a JavaScript {@code function}.
     *
     * <p>Java has no function type, so this recognises the two shapes that a migrated
     * caller can realistically pass: a lambda / method reference (synthetic class
     * implementing a single interface) and an explicit functional-interface instance.
     */
    private static boolean isFunctionLike(Object value) {
        Class<?> type = value.getClass();
        if (type.isSynthetic() && type.getInterfaces().length > 0) {
            for (Class<?> iface : type.getInterfaces()) {
                if (iface.isAnnotationPresent(FunctionalInterface.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reproduces the source's custom-{@code toString} detection.
     *
     * <p>The JavaScript predicate is:
     * <pre>{@code
     * arg.toString !== Object.prototype.toString &&
     *   !arg.toString.toString().includes('[native code]')
     * }</pre>
     *
     * <p>The first clause asks whether {@code toString} has been overridden. The second is
     * a cross-realm guard: an object created inside a {@code vm} context has a
     * <em>different</em> {@code Object.prototype.toString} function object that is
     * nonetheless still native, so the first clause alone would misfire. That case is
     * pinned by {@code tests/index.js:107} ("handles objects in a VM").
     *
     * <p>The Java equivalent satisfies both clauses at once by asking which class
     * <em>declares</em> the resolved {@code toString}. Java has no realms, so a
     * non-overridden {@code toString} always resolves to {@link Object} itself.
     * Inherited overrides resolve to the declaring superclass and are therefore honored,
     * which matches the {@code class Class2 extends Class1} tests in all three suites.
     *
     * <p>Classified as an accepted difference of type (A): required by Java mechanics,
     * externally observable behavior unchanged.
     *
     * @param value a non-null value
     * @return {@code true} when the value declares its own {@code toString}
     */
    public static boolean hasCustomToString(Object value) {
        // A Map models a JS object literal. A `toString` *key* in such a map is data,
        // not a method override, and is handled by the caller before reaching here.
        if (value instanceof Map || value instanceof Iterable || value.getClass().isArray()) {
            return false;
        }
        try {
            Method toString = value.getClass().getMethod("toString");
            return toString.getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            // Unreachable: every class inherits Object#toString. Treated as "no override"
            // rather than swallowed - there is no error path to propagate here, because
            // the JavaScript source likewise cannot fail at this point.
            return false;
        }
    }

    /**
     * Reproduces JavaScript string coercion for values that reach output.
     *
     * <p>Only ever applied to values the source itself stringifies: the result of a
     * custom {@code toString} and bound-mapping lookups.
     *
     * @param value the value to coerce, may be {@code null}
     * @return the JavaScript string form of the value
     */
    public static String toJsString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence cs) {
            return cs.toString();
        }
        return String.valueOf(value);
    }
}

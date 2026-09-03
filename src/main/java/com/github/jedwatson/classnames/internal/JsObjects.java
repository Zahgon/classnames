package com.github.jedwatson.classnames.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaScript object-property semantics: own-key enumeration and {@code for...in} ordering.
 *
 * <p>Property enumeration order is externally observable in this library, because the
 * output string is built by iterating object keys. Reproducing it is therefore a
 * correctness requirement, not a cosmetic one.
 */
public final class JsObjects {

    /** Exclusive upper bound for a JavaScript array index: 2^32 - 1. */
    private static final long MAX_ARRAY_INDEX = 4294967295L;

    private JsObjects() {
    }

    /**
     * Returns own enumerable keys in JavaScript {@code for...in} order.
     *
     * <p>The ECMAScript ordinary-own-property-keys order is:
     * <ol>
     *   <li>integer-like keys ("array indices") in <strong>ascending numeric order</strong>,</li>
     *   <li>then remaining string keys in <strong>insertion order</strong>.</li>
     * </ol>
     *
     * <p>Verified against the source runtime:
     * <pre>{@code
     * Object.keys({b:1, 2:1, a:1, 1:1})  // => ["1", "2", "b", "a"]
     * classNames({b:true, 2:true, a:true, 1:true})  // => "1 2 b a"
     * }</pre>
     *
     * <p>This is why neither {@link java.util.HashMap} (arbitrary order) nor
     * {@link LinkedHashMap} alone (pure insertion order, yielding {@code "b 2 a 1"}) is a
     * correct migration of a JavaScript object.
     *
     * @param map a map modelling a JavaScript object literal
     * @return keys ordered exactly as JavaScript would enumerate them
     */
    public static List<String> ownKeys(Map<?, ?> map) {
        List<String> indexKeys = new ArrayList<>();
        List<String> stringKeys = new ArrayList<>();

        for (Object rawKey : map.keySet()) {
            // JavaScript object keys are always strings (symbols are not enumerated by
            // for...in). A non-string Java key is coerced the same way JS would coerce it.
            String key = rawKey == null ? "null" : String.valueOf(rawKey);
            if (isArrayIndex(key)) {
                indexKeys.add(key);
            } else {
                stringKeys.add(key);
            }
        }

        // Ascending NUMERIC order - not lexicographic. "10" must follow "9".
        indexKeys.sort((a, b) -> Long.compare(Long.parseLong(a), Long.parseLong(b)));

        List<String> ordered = new ArrayList<>(indexKeys.size() + stringKeys.size());
        ordered.addAll(indexKeys);
        ordered.addAll(stringKeys);
        return ordered;
    }

    /**
     * Determines whether a key is a JavaScript "array index".
     *
     * <p>A key qualifies only when it is the <em>canonical</em> decimal string of an
     * integer in {@code [0, 2^32 - 1)}. This deliberately excludes {@code "01"},
     * {@code "-1"}, {@code "1.5"}, {@code " 1"} and {@code "+1"}: JavaScript treats those
     * as ordinary string keys and enumerates them in insertion order.
     */
    static boolean isArrayIndex(String key) {
        if (key.isEmpty() || key.length() > 10) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        // Reject non-canonical forms such as "01" (but keep a bare "0").
        if (key.length() > 1 && key.charAt(0) == '0') {
            return false;
        }
        long value = Long.parseLong(key);
        return value < MAX_ARRAY_INDEX;
    }

    /**
     * Reads a property value from a value modelling a JavaScript object.
     *
     * @param target the object-like value
     * @param key    the property name, as returned by {@link #ownKeys(Map)}
     * @return the property value, or {@code null} when absent
     */
    public static Object get(Map<?, ?> target, String key) {
        Object direct = target.get(key);
        if (direct != null || target.containsKey(key)) {
            return direct;
        }
        // The map may be keyed by a non-String type whose toString matches the key.
        for (Map.Entry<?, ?> entry : target.entrySet()) {
            Object rawKey = entry.getKey();
            String coerced = rawKey == null ? "null" : String.valueOf(rawKey);
            if (coerced.equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Converts a plain Java object into its JavaScript own-property view.
     *
     * <p>MIGRATION DECISION (documented extension point - see workflow §L5): the
     * JavaScript source only ever receives object literals and class instances, and gives
     * no guidance on how a Java POJO should be enumerated. This migration enumerates
     * <strong>public instance fields</strong>, declaring-class first, in declaration
     * order. That is the closest analogue of JavaScript "own enumerable data properties":
     * a JS class instance exposes the data assigned in its constructor, and Java's public
     * fields are the equivalent public data surface.
     *
     * <p>Getters are deliberately NOT invoked: they are a Java-specific convention with no
     * JavaScript counterpart, and calling them could trigger side effects the JavaScript
     * source would never cause.
     *
     * <p>A POJO that declares its own {@code toString} never reaches this method - the
     * caller routes it to the {@code toString} branch first, matching the source.
     *
     * @param target a non-null POJO
     * @return an insertion-ordered map of public field names to values
     */
    public static Map<String, Object> propertiesOf(Object target) {
        Map<String, Object> properties = new LinkedHashMap<>();
        collectFields(target, target.getClass(), properties);
        return properties;
    }

    private static void collectFields(Object target, Class<?> type, Map<String, Object> out) {
        if (type == null || type == Object.class) {
            return;
        }
        // Superclass fields first, matching prototype-chain construction order.
        collectFields(target, type.getSuperclass(), out);

        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || field.isSynthetic()) {
                continue;
            }
            // Java denies access to a public field when its DECLARING CLASS is not
            // public. JavaScript always enumerates own properties, so skipping here
            // would silently drop them - escalate rather than diverge.
            if (!field.canAccess(target) && !field.trySetAccessible()) {
                throw new IllegalStateException(
                        "Unable to read public field '" + field.getName() + "' on "
                                + type.getName() + ": the declaring class is not accessible"
                                + " to " + JsObjects.class.getName()
                                + ". Make the class public or open its module.");
            }

            try {
                out.put(field.getName(), field.get(target));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Unable to read public field '" + field.getName()
                                + "' on " + type.getName(), e);
            }
        }
    }

    /**
     * Returns the elements of an array-like value in index order.
     *
     * <p>Accepts Java arrays (including primitive arrays, whose elements are boxed) and
     * {@link Iterable}. Both are reasonable migrations of a JavaScript array literal, so
     * both are supported rather than forcing callers to pick one.
     *
     * @param value an array or iterable
     * @return its elements in order
     * @throws IllegalArgumentException if the value is not array-like
     */
    public static List<Object> arrayElements(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> elements = new ArrayList<>();
            for (Object element : iterable) {
                elements.add(element);
            }
            return elements;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                elements.add(Array.get(value, i));
            }
            return elements;
        }
        throw new IllegalArgumentException(
                "Not an array-like value: " + value.getClass().getName());
    }

    /**
     * Whether the value should be treated as a JavaScript array.
     *
     * <p>Mirrors {@code Array.isArray(arg)}. A {@link Map} is explicitly excluded even
     * though it is object-like, matching the source's ordering of checks.
     *
     * @param value a non-null value
     * @return {@code true} when the value should be flattened as an array
     */
    public static boolean isArrayLike(Object value) {
        return value.getClass().isArray()
                || (value instanceof Iterable && !(value instanceof Map));
    }
}

package com.botmaker.plugin.api.source;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The new values a {@link Conversion} answers, built link by link and immutable at each one (2026-10-11):
 *
 * <pre>{@code
 * return Converted.empty()
 *         .value(SdkValues.FLOW, flow)
 *         .members(SdkValues.ACTIVITIES, Map.of("Collect.NEXT", BotConstants.outcome("Collect", "NEXT")));
 * }</pre>
 *
 * <p>The host writes each value as it writes any other — a method's expression, an open set's members — and only
 * for the values the conversion's {@code to} names. A value left out is not written: the bot keeps what it has,
 * or the host creates it fresh as it would on bind.
 */
public final class Converted {

    private static final Converted EMPTY = new Converted(Map.of());

    private final Map<String, Object> values;

    private Converted(Map<String, Object> values) {
        this.values = values;
    }

    /** Nothing written yet. */
    public static Converted empty() {
        return EMPTY;
    }

    /**
     * This, writing {@code written} as the method-shaped {@code value}.
     *
     * @throws IllegalArgumentException when {@code value} is an open set, or {@code written} is not its type
     */
    public <T> Converted value(ManagedValue<T> value, T written) {
        if (value == null || value.isOpenSet()) {
            throw new IllegalArgumentException(value + " is an open set: write its members()");
        }
        if (value.cast(written) == null) {
            throw new IllegalArgumentException(value + " holds " + value.type().getName() + ", not " + written);
        }
        return with(value.id(), written);
    }

    /**
     * This, writing {@code members} as the open set {@code value}: each member's name — {@code "Collect.WON"} in a
     * grouped set — to its value, in the order the host writes them.
     *
     * @throws IllegalArgumentException when {@code value} is method-shaped, or a member is not its type
     */
    public <T> Converted members(ManagedValue<T> value, Map<String, ? extends T> members) {
        if (value == null || !value.isOpenSet()) {
            throw new IllegalArgumentException(value + " is one method's value: write its value()");
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        if (members != null) {
            members.forEach((name, member) -> {
                if (name == null || name.isBlank() || value.cast(member) == null) {
                    throw new IllegalArgumentException(value + " cannot hold " + name + " = " + member);
                }
                copy.put(name, member);
            });
        }
        return with(value.id(), Collections.unmodifiableMap(copy));
    }

    private Converted with(String id, Object written) {
        Map<String, Object> next = new LinkedHashMap<>(values);
        next.put(id, written);
        return new Converted(Collections.unmodifiableMap(next));
    }

    /**
     * What to write, by {@link ManagedValue#id()}: a method's value, or for an open set an unmodifiable map from
     * member name to value — what the host reads back.
     */
    public Map<String, Object> written() {
        return values;
    }

    @Override
    public String toString() {
        return "Converted" + values.keySet();
    }
}

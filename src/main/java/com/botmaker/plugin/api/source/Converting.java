package com.botmaker.plugin.api.source;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The old values a {@link Conversion} reads: what the host found in the bot for each {@link ManagedValue#retired()
 * retired} value the conversion names, read as any value is — through the plugin's own declarations — before
 * anything is written (2026-10-11).
 *
 * <p>Built by the host ({@link #of}); a plugin only reads it. A value the bot does not hold reads as null, a set
 * it does not hold as empty, so a conversion copes with a bot that kept only some of the old shapes.
 */
public final class Converting {

    private final Map<String, Object> values;

    private Converting(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * What the host read, by {@link ManagedValue#id()}: a method's value, or for an open set a map from each
     * member's name ({@code "WON"}, or {@code "Collect.WON"} in a grouped set) to its value, in file order.
     */
    public static Converting of(Map<String, ?> values) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (values != null) {
            // Deep: a set's members are copied too, so nothing the host does later reaches what a conversion read.
            values.forEach((id, value) -> copy.put(id, value instanceof Map<?, ?> members
                    ? Collections.unmodifiableMap(new LinkedHashMap<>(members)) : value));
        }
        return new Converting(Collections.unmodifiableMap(copy));
    }

    /** Whether the bot holds {@code value} at all. */
    public boolean holds(ManagedValue<?> value) {
        return value != null && values.containsKey(value.id());
    }

    /**
     * The value of the method-shaped {@code value} the bot holds, or null when it holds none or another type.
     *
     * @throws IllegalArgumentException when {@code value} is an open set: read its {@link #members}, so a mistake
     *                                  is not taken for a bot that holds nothing
     */
    public <T> T value(ManagedValue<T> value) {
        if (value == null || value.isOpenSet()) {
            throw new IllegalArgumentException(value + " is an open set: read its members()");
        }
        return value.cast(values.get(value.id()));
    }

    /**
     * The members of the open set {@code value} the bot holds, by name in file order — {@code "Collect.WON"} in a
     * grouped set — leaving out any member that did not read as the set's type. Empty when it holds none.
     *
     * @throws IllegalArgumentException when {@code value} is method-shaped: read its {@link #value}
     */
    public <T> Map<String, T> members(ManagedValue<T> value) {
        if (value == null || !value.isOpenSet()) {
            throw new IllegalArgumentException(value + " is one method's value: read its value()");
        }
        Map<String, T> members = new LinkedHashMap<>();
        if (!(values.get(value.id()) instanceof Map<?, ?> held)) return members;
        held.forEach((name, member) -> {
            T cast = value.cast(member);
            if (name instanceof String text && cast != null) members.put(text, cast);
        });
        return members;
    }

    @Override
    public String toString() {
        return "Converting" + values.keySet();
    }
}

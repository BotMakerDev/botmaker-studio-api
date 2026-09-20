package com.botmaker.plugin.api.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The three containers the contract registers itself, so a project with no plugins installed still has a
 * list and a map.
 *
 * <p>They are ordinary {@link ValueContainer}s with no privilege whatsoever — a plugin's own container is
 * built the same way, registered the same way and written by the same host code. That is the whole claim the
 * open mechanism makes, and keeping the built-ins on this side of the same interface is what checks it.
 */
final class HostContainers {

    private HostContainers() {
    }

    /** {@code List.of(e₁, …)}. */
    static final class ListContainer implements ValueContainer<List<?>> {

        @Override
        public Class<?> type() {
            return List.class;
        }

        @Override
        public int arity() {
            return 1;
        }

        @Override
        public String factory() {
            return "of";
        }

        @Override
        public String label() {
            return "List of…";
        }

        @Override
        public List<Object> parts(List<?> value) {
            return value == null ? List.of() : List.copyOf(value);
        }

        @Override
        public List<?> build(List<Object> parts) {
            return parts == null ? List.of() : List.copyOf(parts);
        }

        @Override
        public List<ValueForm> partForms(List<ValueForm> arguments, int parts) {
            return Collections.nCopies(parts, arguments.getFirst());
        }
    }

    /**
     * {@code Map.ofEntries(Map.entry(k, v), …)}.
     *
     * <p>Built back as a {@link LinkedHashMap} rather than through {@code Map.ofEntries}, because
     * {@link ValueContainer#parts} promises a stable order and {@code Map.of*} specifies none — an
     * unspecified order means the generated file is rewritten with its entries shuffled on some later run,
     * for no change the user made.
     */
    static final class MapContainer implements ValueContainer<Map<?, ?>> {

        @Override
        public Class<?> type() {
            return Map.class;
        }

        @Override
        public int arity() {
            return 2;
        }

        @Override
        public String factory() {
            return "ofEntries";
        }

        @Override
        public String label() {
            return "Map from… to…";
        }

        @Override
        public List<Object> parts(Map<?, ?> value) {
            return value == null ? List.of() : List.copyOf(value.entrySet());
        }

        @Override
        public Map<?, ?> build(List<Object> parts) {
            Map<Object, Object> out = new LinkedHashMap<>();
            if (parts != null) {
                for (Object part : parts) {
                    if (part instanceof Map.Entry<?, ?> entry) out.put(entry.getKey(), entry.getValue());
                }
            }
            return Collections.unmodifiableMap(out);
        }

        @Override
        public List<ValueForm> partForms(List<ValueForm> arguments, int parts) {
            return Collections.nCopies(parts, new ValueForm.Of(ValueContainer.ENTRY, arguments));
        }
    }

    /** {@code Map.entry(k, v)} — declared on {@code Map}, not on {@code Map.Entry}. */
    static final class EntryContainer implements ValueContainer<Map.Entry<?, ?>> {

        @Override
        public Class<?> type() {
            return Map.Entry.class;
        }

        @Override
        public int arity() {
            return 2;
        }

        @Override
        public Class<?> factoryOwner() {
            return Map.class;
        }

        @Override
        public String factory() {
            return "entry";
        }

        @Override
        public String label() {
            return "Entry";
        }

        @Override
        public List<Object> parts(Map.Entry<?, ?> value) {
            List<Object> out = new ArrayList<>(2);
            out.add(value == null ? null : value.getKey());
            out.add(value == null ? null : value.getValue());
            return Collections.unmodifiableList(out);
        }

        @Override
        public Map.Entry<?, ?> build(List<Object> parts) {
            Object key = parts != null && !parts.isEmpty() ? parts.getFirst() : null;
            Object value = parts != null && parts.size() > 1 ? parts.get(1) : null;
            return Map.entry(key, value);
        }

        @Override
        public List<ValueForm> partForms(List<ValueForm> arguments, int parts) {
            return List.copyOf(arguments);
        }
    }
}

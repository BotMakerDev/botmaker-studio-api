package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.assist.ToolSchemas.Actions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What an assistant sent built into a tool's records, against the fields {@link ToolSchemas} read off them. Every
 * refusal is an {@link IllegalArgumentException} naming the key, which {@link AssistantTool#invoke} answers as the
 * tool's refusal.
 */
final class ToolArguments {

    /**
     * Each sealed interface a list component holds, read into its actions once: its declaration already checked
     * it, so a call only builds from it.
     */
    private static final ClassValue<Actions<?>> LISTED_ACTIONS = new ClassValue<>() {
        @Override
        protected Actions<?> computeValue(Class<?> sealed) {
            return Actions.of(sealed.getName(), sealed);
        }
    };

    private ToolArguments() {
    }

    /** The action {@code sent} chose; {@code at} prefixes each key in a refusal ({@code "edits[2]."}). */
    static <S> S action(Actions<S> actions, Map<String, ?> sent, String at) {
        String action = AssistantTool.ACTION;
        Object chosen = sent.get(action);
        if (chosen == null) {
            throw new IllegalArgumentException("'" + at + action + "' is required: one of " + actions.byId().keySet());
        }
        Actions.Act act = actions.byId().get(chosen.toString().trim().toLowerCase(Locale.ROOT));
        if (act == null) {
            throw new IllegalArgumentException("'" + at + action + "' is one of " + actions.byId().keySet()
                    + ", not " + chosen);
        }
        Map<String, Object> rest = new LinkedHashMap<>(sent);
        rest.remove(action);
        rest.values().removeIf(Objects::isNull);
        Set<String> unknown = new LinkedHashSet<>(rest.keySet());
        unknown.removeAll(actions.union().keySet());
        if (!unknown.isEmpty()) {
            List<String> takes = new ArrayList<>(List.of(action));
            takes.addAll(actions.union().keySet());
            throw unknown(at, unknown, takes);
        }
        List<String> own = act.fields().stream().map(ToolSchema.Field::name).toList();
        List<String> foreign = rest.keySet().stream().filter(k -> !own.contains(k)).toList();
        if (!foreign.isEmpty()) {
            throw new IllegalArgumentException((at.isEmpty() ? "" : "'" + parent(at) + "': ")
                    + act.id() + " does not take " + foreign + "; it takes " + own);
        }
        @SuppressWarnings("unchecked")
        S built = (S) record(act.type(), act.fields(), rest, at, " for " + act.id());
        return built;
    }

    /**
     * {@code type} built from {@code sent}; {@code at} prefixes each key in a refusal ({@code "boxes[2]."}), and
     * {@code needed} follows "is required" ({@code " for rename"}).
     */
    static <R extends Record> R record(Class<R> type, List<ToolSchema.Field> fields, Map<String, ?> sent,
                                       String at, String needed) {
        RecordComponent[] components = type.getRecordComponents();
        // A key sent as null is one left out, known or not.
        Set<String> unknown = new LinkedHashSet<>();
        sent.forEach((k, v) -> {
            if (v != null) unknown.add(k);
        });
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            ToolSchema.Field field = fields.get(i);
            unknown.remove(field.name());
            Object value = sent.get(field.name());
            // An optional choice sent empty is one left out: "" was how an assistant said "the default".
            if (field.optional() && field.kind() == ToolSchema.Field.Kind.CHOICE && value instanceof CharSequence text
                    && text.toString().isBlank()) {
                value = null;
            }
            if (value == null) {
                if (!field.optional()) {
                    throw new IllegalArgumentException("'" + at + field.name() + "' is required" + needed);
                }
            } else {
                values[i] = convert(field, components[i], value, at + field.name());
            }
        }
        if (!unknown.isEmpty()) {
            throw unknown(at, unknown, fields.stream().map(ToolSchema.Field::name).toList());
        }
        try {
            Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            Constructor<R> constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            String why = cause.getMessage() == null ? cause.toString() : cause.getMessage();
            throw new IllegalArgumentException(at.isEmpty() ? why : "'" + parent(at) + "': " + why, cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build " + type.getName(), e);
        }
    }

    /** The refusal of keys no field takes, at the top or inside {@code at}. */
    private static IllegalArgumentException unknown(String at, Set<String> unknown, List<String> takes) {
        return new IllegalArgumentException(at.isEmpty()
                ? "no parameter " + unknown + "; it takes " + takes
                : "'" + parent(at) + "' has no field " + unknown + "; it takes " + takes);
    }

    /** The key {@code at} prefixes, without its trailing dot: {@code "edits[2]"}. */
    private static String parent(String at) {
        return at.substring(0, at.length() - 1);
    }

    private static Object convert(ToolSchema.Field field, RecordComponent component, Object sent, String path) {
        String key = "'" + path + "'";
        Class<?> target = component.getType();
        return switch (field.kind()) {
            case TEXT -> {
                // A number or a yes sent bare is its text: assistants send a slot's 5 or true unquoted.
                if (sent instanceof CharSequence || sent instanceof Number || sent instanceof Boolean) {
                    yield sent.toString();
                }
                throw new IllegalArgumentException(key + " is text, not " + sent);
            }
            case YES_NO -> {
                if (sent instanceof Boolean yes) yield yes;
                if (sent instanceof String text && (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))) {
                    yield Boolean.parseBoolean(text);
                }
                throw new IllegalArgumentException(key + " is true or false, not " + sent);
            }
            case WHOLE_NUMBER -> {
                long whole = whole(key, sent);
                if (target == int.class || target == Integer.class) {
                    if (whole < Integer.MIN_VALUE || whole > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException(key + " is out of range: " + whole);
                    }
                    yield (int) whole;
                }
                yield whole;
            }
            case NUMBER -> {
                if (sent instanceof Number number) yield number.doubleValue();
                try {
                    yield Double.parseDouble(sent.toString().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(key + " is a number, not " + sent);
                }
            }
            case CHOICE -> constant(target, field.choices(), sent, key);
            case TEXT_LIST -> {
                List<String> texts = new ArrayList<>();
                for (Object element : list(key, sent, "text")) {
                    if (!(element instanceof CharSequence text)) {
                        throw new IllegalArgumentException(key + " is a list of text, not " + element);
                    }
                    texts.add(text.toString());
                }
                yield List.copyOf(texts);
            }
            case CHOICE_LIST -> {
                Class<?> element = ToolSchemas.element(component.getGenericType(), 0);
                List<Object> constants = new ArrayList<>();
                for (Object each : list(key, sent, "one of " + field.choices())) {
                    constants.add(constant(element, field.choices(), each, key));
                }
                yield List.copyOf(constants);
            }
            case RECORD -> record(target.asSubclass(Record.class), field.fields(), object(key, sent, field), path + ".",
                    "");
            case RECORD_LIST -> {
                Class<?> element = ToolSchemas.element(component.getGenericType(), 0);
                Actions<?> actions = element.isRecord() ? null : LISTED_ACTIONS.get(element);
                List<Object> records = new ArrayList<>();
                int i = 0;
                for (Object each : list(key, sent, "objects")) {
                    String at = path + "[" + i++ + "]";
                    Map<String, Object> object = object("'" + at + "'", each, field);
                    records.add(actions != null ? action(actions, object, at + ".")
                            : record(element.asSubclass(Record.class), field.fields(), object, at + ".", ""));
                }
                yield List.copyOf(records);
            }
            case TEXT_MAP -> {
                if (!(sent instanceof Map<?, ?> map)) throw new IllegalArgumentException(key + " is an object of text");
                Map<String, String> texts = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object value = entry.getValue() == null ? "" : entry.getValue();
                    if (!(value instanceof CharSequence || value instanceof Number || value instanceof Boolean)) {
                        throw new IllegalArgumentException(key + "'s " + entry.getKey() + " is text, not " + value);
                    }
                    texts.put(String.valueOf(entry.getKey()), value.toString());
                }
                yield Collections.unmodifiableMap(texts);
            }
        };
    }

    private static Object constant(Class<?> type, List<String> choices, Object sent, String key) {
        if (sent == null) throw new IllegalArgumentException(key + " is one of " + choices + ", not null");
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(sent.toString().trim())) return constant;
        }
        throw new IllegalArgumentException(key + " is one of " + choices + ", not " + sent);
    }

    private static Collection<?> list(String key, Object sent, String of) {
        if (sent instanceof Collection<?> list) return list;
        throw new IllegalArgumentException(key + " is a list of " + of);
    }

    private static Map<String, Object> object(String key, Object sent, ToolSchema.Field field) {
        if (!(sent instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(key + " is an object of "
                    + field.fields().stream().map(ToolSchema.Field::name).toList() + ", not " + sent);
        }
        Map<String, Object> keyed = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (v != null) keyed.put(String.valueOf(k), v);
        });
        return keyed;
    }

    private static long whole(String key, Object sent) {
        if (sent instanceof Integer || sent instanceof Long || sent instanceof Short || sent instanceof Byte) {
            return ((Number) sent).longValue();
        }
        if (sent instanceof BigInteger || sent instanceof BigDecimal) {
            try {
                return new BigDecimal(sent.toString()).longValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(key + " is a whole number in range, not " + sent);
            }
        }
        if (sent instanceof Number number) {
            double value = number.doubleValue();
            // 2^63 is exactly representable; anything at or past it does not fit a long.
            if (value == Math.rint(value) && value >= -0x1p63 && value < 0x1p63) return (long) value;
        } else {
            try {
                return Long.parseLong(sent.toString().trim());
            } catch (NumberFormatException ignored) {
                // falls through to the refusal
            }
        }
        throw new IllegalArgumentException(key + " is a whole number, not " + sent);
    }
}

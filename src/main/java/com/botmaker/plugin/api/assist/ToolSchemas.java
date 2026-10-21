package com.botmaker.plugin.api.assist;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A tool's parameter records read into its {@link ToolSchema}, once, when the tool is declared: every check that
 * refuses a record the schema cannot say is here, so a bad tool fails its plugin's class initialisation.
 * {@link ToolArguments} builds the records back from what an assistant sent.
 */
final class ToolSchemas {

    /** A tool's name, an action's id and a {@link Key}: {@code snake_case}, at most 48 characters. */
    static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,47}");

    private ToolSchemas() {
    }

    /** A sealed interface's records as the actions of one tool, and the flat union of their fields. */
    record Actions<S>(Map<String, Act> byId, Map<String, ToolSchema.Field> union, ToolSchema schema) {

        /** One permitted record. */
        record Act(String id, Class<? extends Record> type, List<ToolSchema.Field> fields) {
        }

        static <S> Actions<S> of(String tool, Class<S> sealed) {
            return of(tool, sealed, new HashSet<>());
        }

        /** {@code inside} holds the records around a list of these actions, which none may hold. */
        static <S> Actions<S> of(String tool, Class<S> sealed, Set<Class<?>> inside) {
            if (sealed == null || !sealed.isInterface() || !sealed.isSealed()) {
                throw new IllegalArgumentException(tool + ": an action tool takes a sealed interface of records");
            }
            Map<String, Act> byId = new LinkedHashMap<>();
            Map<String, ToolSchema.Field> union = new LinkedHashMap<>();
            Map<String, List<String>> takers = new LinkedHashMap<>();
            List<ToolSchema.Action> described = new ArrayList<>();
            for (Class<?> permitted : sealed.getPermittedSubclasses()) {
                if (!permitted.isRecord()) {
                    throw new IllegalArgumentException(permitted.getName() + " is an action of " + tool
                            + ", so it must be a record");
                }
                @SuppressWarnings("unchecked")
                Class<? extends Record> type = (Class<? extends Record>) permitted;
                String id = snakeCase(type.getSimpleName());
                if (!NAME.matcher(id).matches()) {
                    throw new IllegalArgumentException(type.getName() + " is an action of " + tool
                            + ", so it needs a name that reads as snake_case, not '" + id + "'");
                }
                if (byId.containsKey(id)) {
                    throw new IllegalArgumentException(tool + ": two actions are named " + id);
                }
                List<ToolSchema.Field> fields = fieldsOf(type, inside);
                for (ToolSchema.Field field : fields) {
                    if (field.name().equals(AssistantTool.ACTION)) {
                        throw new IllegalArgumentException(type.getName() + "." + AssistantTool.ACTION + ": '"
                                + AssistantTool.ACTION + "' is the action's own key");
                    }
                    ToolSchema.Field seen = union.get(field.name());
                    if (seen == null) {
                        union.put(field.name(), field);
                    } else if (!sameShape(seen, field)) {
                        throw new IllegalArgumentException(type.getName() + "." + field.name()
                                + " holds what another action's '" + field.name() + "' does not");
                    } else if (seen.description().isEmpty() && !field.description().isEmpty()) {
                        union.put(field.name(), field);
                    }
                    takers.computeIfAbsent(field.name(), k -> new ArrayList<>()).add(id);
                }
                byId.put(id, new Act(id, type, fields));
                Describe says = type.getAnnotation(Describe.class);
                described.add(new ToolSchema.Action(id, says == null ? "" : says.value(),
                        fields.stream().map(ToolSchema.Field::name).toList(),
                        fields.stream().filter(f -> !f.optional()).map(ToolSchema.Field::name).toList()));
            }
            if (byId.isEmpty()) throw new IllegalArgumentException(tool + ": an action tool needs an action");

            List<ToolSchema.Field> wire = new ArrayList<>();
            wire.add(new ToolSchema.Field(AssistantTool.ACTION, ToolSchema.Field.Kind.CHOICE, "", false,
                    List.copyOf(byId.keySet()), List.of(), List.of(), List.of()));
            for (ToolSchema.Field field : union.values()) {
                List<String> by = takers.get(field.name());
                wire.add(new ToolSchema.Field(field.name(), field.kind(), field.description(), true, field.choices(),
                        field.fields(), by.size() == byId.size() ? List.of() : by, field.oneOf()));
            }
            return new Actions<>(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(union),
                    new ToolSchema(wire, described));
        }

        /** Whether two actions' fields of one name hold the same, whatever each says of it. */
        private static boolean sameShape(ToolSchema.Field a, ToolSchema.Field b) {
            if (a.kind() != b.kind() || !a.choices().equals(b.choices()) || a.fields().size() != b.fields().size()
                    || !a.oneOf().stream().map(ToolSchema.Action::id).toList()
                    .equals(b.oneOf().stream().map(ToolSchema.Action::id).toList())) {
                return false;
            }
            for (int i = 0; i < a.fields().size(); i++) {
                ToolSchema.Field x = a.fields().get(i);
                ToolSchema.Field y = b.fields().get(i);
                if (!x.name().equals(y.name()) || x.optional() != y.optional() || !sameShape(x, y)) return false;
            }
            return true;
        }

        private static String snakeCase(String simpleName) {
            return simpleName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
                    .toLowerCase(Locale.ROOT);
        }
    }

    /** {@code type}'s components as fields; {@code inside} holds the records around it, which it may not hold. */
    static List<ToolSchema.Field> fieldsOf(Class<? extends Record> type, Set<Class<?>> inside) {
        if (!inside.add(type)) {
            throw new IllegalArgumentException(type.getName() + " holds itself; a tool's parameters are a tree");
        }
        List<ToolSchema.Field> fields = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            Describe describe = component.getAnnotation(Describe.class);
            boolean optional = describe != null && describe.optional();
            Class<?> raw = component.getType();
            Type generic = component.getGenericType();
            String where = type.getName() + "." + component.getName();
            if (optional && raw.isPrimitive()) {
                throw new IllegalArgumentException(where + " is optional, so it must be a box, not " + raw);
            }
            ToolSchema.Field.Kind kind = kind(raw, generic, where);
            List<String> choices = switch (kind) {
                case CHOICE -> names(raw);
                case CHOICE_LIST -> names(element(generic, 0));
                default -> List.of();
            };
            List<ToolSchema.Field> nested = List.of();
            List<ToolSchema.Action> oneOf = List.of();
            if (kind == ToolSchema.Field.Kind.RECORD) {
                nested = fieldsOf(raw.asSubclass(Record.class), inside);
            } else if (kind == ToolSchema.Field.Kind.RECORD_LIST && element(generic, 0).isRecord()) {
                nested = fieldsOf(element(generic, 0).asSubclass(Record.class), inside);
            } else if (kind == ToolSchema.Field.Kind.RECORD_LIST) {
                ToolSchema each = Actions.of(where, element(generic, 0), inside).schema();
                nested = each.fields();
                oneOf = each.actions();
            }
            Key sentAs = component.getAnnotation(Key.class);
            if (sentAs != null && !NAME.matcher(sentAs.value()).matches()) {
                throw new IllegalArgumentException(where + ": @Key is a snake_case key, not '" + sentAs.value() + "'");
            }
            String key = sentAs == null ? component.getName() : sentAs.value();
            if (fields.stream().anyMatch(f -> f.name().equals(key))) {
                throw new IllegalArgumentException(where + ": two components are sent as '" + key + "'");
            }
            fields.add(new ToolSchema.Field(key, kind, describe == null ? "" : describe.value(),
                    optional, choices, nested, List.of(), oneOf));
        }
        inside.remove(type);
        return List.copyOf(fields);
    }

    /** The {@code index}th type argument of {@code generic} when it is a plain class; {@code null} otherwise. */
    static Class<?> element(Type generic, int index) {
        if (generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[index] instanceof Class<?> argument) {
            return argument;
        }
        return null;
    }

    private static List<String> names(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(c -> ((Enum<?>) c).name().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static ToolSchema.Field.Kind kind(Class<?> raw, Type generic, String where) {
        if (raw == String.class) return ToolSchema.Field.Kind.TEXT;
        if (raw == boolean.class || raw == Boolean.class) return ToolSchema.Field.Kind.YES_NO;
        if (raw == int.class || raw == Integer.class || raw == long.class || raw == Long.class) {
            return ToolSchema.Field.Kind.WHOLE_NUMBER;
        }
        if (raw == double.class || raw == Double.class) return ToolSchema.Field.Kind.NUMBER;
        if (raw.isEnum()) return ToolSchema.Field.Kind.CHOICE;
        if (raw.isRecord()) return ToolSchema.Field.Kind.RECORD;
        if (raw == List.class) {
            Class<?> element = element(generic, 0);
            if (element == String.class) return ToolSchema.Field.Kind.TEXT_LIST;
            if (element != null && element.isEnum()) return ToolSchema.Field.Kind.CHOICE_LIST;
            if (element != null && (element.isRecord() || element.isInterface() && element.isSealed())) {
                return ToolSchema.Field.Kind.RECORD_LIST;
            }
        }
        if (raw == Map.class && element(generic, 0) == String.class && element(generic, 1) == String.class) {
            return ToolSchema.Field.Kind.TEXT_MAP;
        }
        throw new IllegalArgumentException(where + " is a " + generic.getTypeName() + "; a tool parameter is"
                + " String, boolean, int, long, double, an enum, a record, a List of String, of an enum, of a"
                + " record or of a sealed interface of records, or Map<String, String>");
    }
}

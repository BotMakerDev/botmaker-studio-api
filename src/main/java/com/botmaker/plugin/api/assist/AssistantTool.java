package com.botmaker.plugin.api.assist;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A tool a plugin offers the AI assistant — see the package comment for the declaration.
 *
 * <p>Steps: {@link #named} → {@link Describing#describedAs} → {@link Taking#takes} (a record),
 * {@link Taking#takesOneOf} (a sealed interface of records, one per action) or {@link Taking#takesNothing} →
 * {@code handledBy} (a method reference). The records' components are read once, when the tool is declared, into
 * {@link #schema()}; a component the schema cannot say is refused then, naming the record and the component, so a
 * bad tool fails the plugin's own class initialisation and tests rather than an assistant's call.
 *
 * <p>A component is {@code String}, {@code boolean}, {@code int}, {@code long}, {@code double} (or their boxes),
 * an enum, a nested record, a {@code List} of {@code String}, of an enum, of a record or of a sealed interface of
 * records (each element one of its actions, flat as {@link Taking#takesOneOf}'s are), or a
 * {@code Map<String, String>}. A record may not hold itself, however deep.
 *
 * <p>The host serves the tool under a name prefixed with the plugin's, so two plugins' {@code screenshot}s do
 * not collide; the name here is unique within the plugin.
 *
 * @param <P> the parameter record; {@link OneOf} for a tool declared by {@link Taking#takesOneOf}
 */
public final class AssistantTool<P extends Record> {

    /** Answers one call: the parameters the assistant sent, built into the record. A method reference. */
    @FunctionalInterface
    public interface Handler<P extends Record> {
        AgentReply handle(P params, AgentContext context);
    }

    /**
     * Answers one call of a tool declared by {@link Taking#takesOneOf}: the action the assistant chose, built into
     * its record, for the handler to {@code switch} over. A method reference.
     */
    @FunctionalInterface
    public interface ActionHandler<S> {
        /** The reply to {@code action}, one of the sealed interface's records; a throw is the tool's refusal. */
        AgentReply handle(S action, AgentContext context);
    }

    /** The parameters of a tool that takes none. */
    public record None() {
    }

    /** The parameters of a tool declared by {@link Taking#takesOneOf}: the action the assistant chose. */
    public record OneOf<S>(S action) {
    }

    /** The key an action tool's choice is sent under; no action record may have a component of that name. */
    public static final String ACTION = "action";

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,47}");

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

    private final String name;
    private final String description;
    private final Class<P> type;
    private final ToolSchema schema;
    private final List<ToolParam> params;
    private final Function<Map<String, ?>, P> builder;
    private final Handler<P> handler;

    private AssistantTool(String name, String description, Class<P> type, ToolSchema schema,
                          Function<Map<String, ?>, P> builder, Handler<P> handler) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.schema = schema;
        this.params = schema.fields().stream().map(f -> new ToolParam(f.name(), f.kind(), f.description(),
                f.optional(), f.choices())).toList();
        this.builder = builder;
        this.handler = handler;
    }

    /** The first step: the tool's name, {@code snake_case}, unique within its plugin: {@code "crop_picture"}. */
    public static Describing named(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("A tool name is snake_case, at most 48 characters: " + name);
        }
        return new Describing(name);
    }

    /** After {@link #named}: what the tool does, as the assistant reads it. */
    public static final class Describing {

        private final String name;

        private Describing(String name) {
            this.name = name;
        }

        public Taking describedAs(String description) {
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException(name + ": a tool needs a description");
            }
            return new Taking(name, description.trim());
        }
    }

    /** After {@link Describing#describedAs}: the parameter record, or the actions. */
    public static final class Taking {

        private final String name;
        private final String description;

        private Taking(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /** The record whose components are the tool's parameters. */
        public <P extends Record> Handling<P> takes(Class<P> params) {
            if (params == null || !params.isRecord()) {
                throw new IllegalArgumentException(name + ": a tool's parameters are a record class");
            }
            return new Handling<>(name, description, params);
        }

        /** No parameters. */
        public Handling<None> takesNothing() {
            return new Handling<>(name, description, None.class);
        }

        /**
         * One of several actions: {@code actions} is a sealed interface whose permitted classes are records, one per
         * action, its id the record's simple name in {@code snake_case} ({@code SetStart} → {@code set_start}) and
         * its {@link Describe} what it does. The assistant sends {@code action} and that record's components, flat
         * beside it ({@link ToolSchema}); a component of the same name in two actions must hold the same, and the
         * first sentence said of it is the one the assistant reads. The actions are one level: a permitted class that
         * is itself sealed is refused, so flatten it into records.
         */
        public <S> Choosing<S> takesOneOf(Class<S> actions) {
            return new Choosing<>(name, description, Actions.of(name, actions));
        }
    }

    /** After {@link Taking#takes}: the handler. */
    public static final class Handling<P extends Record> {

        private final String name;
        private final String description;
        private final Class<P> type;

        private Handling(String name, String description, Class<P> type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }

        public AssistantTool<P> handledBy(Handler<P> handler) {
            List<ToolSchema.Field> fields = fieldsOf(type, new HashSet<>());
            return new AssistantTool<>(name, description, type, new ToolSchema(fields, List.of()),
                    sent -> record(type, fields, sent, "", ""), Objects.requireNonNull(handler, "handler"));
        }
    }

    /** After {@link Taking#takesOneOf}: the handler, which gets the chosen action's record. */
    public static final class Choosing<S> {

        private final String name;
        private final String description;
        private final Actions<S> actions;

        private Choosing(String name, String description, Actions<S> actions) {
            this.name = name;
            this.description = description;
            this.actions = actions;
        }

        /** The last step: the tool, whose calls {@code handler} answers. */
        public AssistantTool<OneOf<S>> handledBy(ActionHandler<S> handler) {
            Objects.requireNonNull(handler, "handler");
            @SuppressWarnings("unchecked")
            Class<OneOf<S>> type = (Class<OneOf<S>>) (Class<?>) OneOf.class;
            return new AssistantTool<>(name, description, type, actions.schema(), actions::build,
                    (chosen, context) -> handler.handle(chosen.action(), context));
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    /** The parameter record; {@link OneOf} for a tool declared by {@link Taking#takesOneOf}. */
    public Class<P> paramsType() {
        return type;
    }

    /** The top level of the input schema, one entry per record component (or per field on the wire), in order. */
    public List<ToolParam> params() {
        return params;
    }

    /** The whole input schema: nested records' fields, and an action tool's actions. */
    public ToolSchema schema() {
        return schema;
    }

    /**
     * Runs the tool on what the assistant sent — a JSON object already read into {@code String},
     * {@code Boolean}, {@code Number}, {@code List} and {@code Map} values. Never throws: an argument that does
     * not fit, a missing or unknown one, and a handler that throws all answer a {@link AgentReply#refused} saying
     * so.
     */
    public AgentReply invoke(Map<String, ?> arguments, AgentContext context) {
        P built;
        try {
            built = builder.apply(arguments == null ? Map.of() : arguments);
        } catch (IllegalArgumentException e) {
            return AgentReply.refused(name + ": " + e.getMessage());
        }
        try {
            AgentReply reply = handler.handle(built, context);
            return reply == null ? AgentReply.text("") : reply;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (RuntimeException | Error e) {
            // An Error too: a handler that links a class the host lacks (JavaFX, headless) throws
            // NoClassDefFoundError, and that is this tool's failure, not the MCP server's.
            return AgentReply.refused(name + " failed: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // ---- an action tool ---------------------------------------------------------------------------------------

    /** A sealed interface's records as the actions of one tool, and the flat union of their fields. */
    private record Actions<S>(Map<String, Act> byId, Map<String, ToolSchema.Field> union, ToolSchema schema) {

        /** One permitted record. */
        private record Act(String id, Class<? extends Record> type, List<ToolSchema.Field> fields) {
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
                    if (field.name().equals(ACTION)) {
                        throw new IllegalArgumentException(type.getName() + "." + ACTION + ": '" + ACTION
                                + "' is the action's own key");
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
            wire.add(new ToolSchema.Field(ACTION, ToolParam.Kind.CHOICE, "", false, List.copyOf(byId.keySet()),
                    List.of(), List.of(), List.of()));
            for (ToolSchema.Field field : union.values()) {
                List<String> by = takers.get(field.name());
                wire.add(new ToolSchema.Field(field.name(), field.kind(), field.description(), true, field.choices(),
                        field.fields(), by.size() == byId.size() ? List.of() : by, field.oneOf()));
            }
            return new Actions<>(Collections.unmodifiableMap(byId), Collections.unmodifiableMap(union),
                    new ToolSchema(wire, described));
        }

        OneOf<S> build(Map<String, ?> sent) {
            return new OneOf<>(build(sent, ""));
        }

        /** The action {@code sent} chose; {@code at} prefixes each key in a refusal ({@code "edits[2]."}). */
        S build(Map<String, ?> sent, String at) {
            Object chosen = sent.get(ACTION);
            if (chosen == null) {
                throw new IllegalArgumentException("'" + at + ACTION + "' is required: one of " + byId.keySet());
            }
            Act act = byId.get(chosen.toString().trim().toLowerCase(Locale.ROOT));
            if (act == null) {
                throw new IllegalArgumentException("'" + at + ACTION + "' is one of " + byId.keySet() + ", not "
                        + chosen);
            }
            Map<String, Object> rest = new LinkedHashMap<>(sent);
            rest.remove(ACTION);
            rest.values().removeIf(Objects::isNull);
            Set<String> unknown = new LinkedHashSet<>(rest.keySet());
            unknown.removeAll(union.keySet());
            if (!unknown.isEmpty()) {
                List<String> takes = new ArrayList<>(List.of(ACTION));
                takes.addAll(union.keySet());
                throw new IllegalArgumentException(at.isEmpty()
                        ? "no parameter " + unknown + "; it takes " + takes
                        : "'" + at.substring(0, at.length() - 1) + "' has no field " + unknown + "; it takes " + takes);
            }
            List<String> own = act.fields().stream().map(ToolSchema.Field::name).toList();
            List<String> foreign = rest.keySet().stream().filter(k -> !own.contains(k)).toList();
            if (!foreign.isEmpty()) {
                throw new IllegalArgumentException((at.isEmpty() ? "" : "'" + at.substring(0, at.length() - 1) + "': ")
                        + act.id() + " does not take " + foreign + "; it takes " + own);
            }
            @SuppressWarnings("unchecked")
            S action = (S) record(act.type(), act.fields(), rest, at, " for " + act.id());
            return action;
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

    // ---- building a record from what was sent -----------------------------------------------------------------

    /**
     * {@code type} built from {@code sent}; {@code at} prefixes each key in a refusal ({@code "boxes[2]."}), and
     * {@code needed} follows "is required" ({@code " for rename"}).
     */
    private static <R extends Record> R record(Class<R> type, List<ToolSchema.Field> fields, Map<String, ?> sent,
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
            if (field.optional() && field.kind() == ToolParam.Kind.CHOICE && value instanceof CharSequence text
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
            List<String> takes = fields.stream().map(ToolSchema.Field::name).toList();
            throw new IllegalArgumentException(at.isEmpty()
                    ? "no parameter " + unknown + "; it takes " + takes
                    : "'" + at.substring(0, at.length() - 1) + "' has no field " + unknown + "; it takes " + takes);
        }
        try {
            Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            Constructor<R> constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            String why = cause.getMessage() == null ? cause.toString() : cause.getMessage();
            throw new IllegalArgumentException(at.isEmpty() ? why : "'" + at.substring(0, at.length() - 1) + "': " + why,
                    cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build " + type.getName(), e);
        }
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
                Class<?> element = element(component.getGenericType(), 0);
                List<Object> constants = new ArrayList<>();
                for (Object each : list(key, sent, "one of " + field.choices())) {
                    constants.add(constant(element, field.choices(), each, key));
                }
                yield List.copyOf(constants);
            }
            case RECORD -> record(target.asSubclass(Record.class), field.fields(), object(key, sent, field), path + ".",
                    "");
            case RECORD_LIST -> {
                Class<?> element = element(component.getGenericType(), 0);
                Actions<?> actions = element.isRecord() ? null : LISTED_ACTIONS.get(element);
                List<Object> records = new ArrayList<>();
                int i = 0;
                for (Object each : list(key, sent, "objects")) {
                    String at = path + "[" + i++ + "]";
                    Map<String, Object> object = object("'" + at + "'", each, field);
                    records.add(actions != null ? actions.build(object, at + ".")
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

    // ---- reading a record into the schema ---------------------------------------------------------------------

    /** {@code type}'s components as fields; {@code inside} holds the records around it, which it may not hold. */
    private static List<ToolSchema.Field> fieldsOf(Class<? extends Record> type, Set<Class<?>> inside) {
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
            ToolParam.Kind kind = kind(raw, generic, where);
            List<String> choices = switch (kind) {
                case CHOICE -> names(raw);
                case CHOICE_LIST -> names(element(generic, 0));
                default -> List.of();
            };
            List<ToolSchema.Field> nested = List.of();
            List<ToolSchema.Action> oneOf = List.of();
            if (kind == ToolParam.Kind.RECORD) {
                nested = fieldsOf(raw.asSubclass(Record.class), inside);
            } else if (kind == ToolParam.Kind.RECORD_LIST && element(generic, 0).isRecord()) {
                nested = fieldsOf(element(generic, 0).asSubclass(Record.class), inside);
            } else if (kind == ToolParam.Kind.RECORD_LIST) {
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

    private static List<String> names(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(c -> ((Enum<?>) c).name().toLowerCase(Locale.ROOT))
                .toList();
    }

    private static ToolParam.Kind kind(Class<?> raw, Type generic, String where) {
        if (raw == String.class) return ToolParam.Kind.TEXT;
        if (raw == boolean.class || raw == Boolean.class) return ToolParam.Kind.YES_NO;
        if (raw == int.class || raw == Integer.class || raw == long.class || raw == Long.class) {
            return ToolParam.Kind.WHOLE_NUMBER;
        }
        if (raw == double.class || raw == Double.class) return ToolParam.Kind.NUMBER;
        if (raw.isEnum()) return ToolParam.Kind.CHOICE;
        if (raw.isRecord()) return ToolParam.Kind.RECORD;
        if (raw == List.class) {
            Class<?> element = element(generic, 0);
            if (element == String.class) return ToolParam.Kind.TEXT_LIST;
            if (element != null && element.isEnum()) return ToolParam.Kind.CHOICE_LIST;
            if (element != null && (element.isRecord() || element.isInterface() && element.isSealed())) {
                return ToolParam.Kind.RECORD_LIST;
            }
        }
        if (raw == Map.class && element(generic, 0) == String.class && element(generic, 1) == String.class) {
            return ToolParam.Kind.TEXT_MAP;
        }
        throw new IllegalArgumentException(where + " is a " + generic.getTypeName() + "; a tool parameter is"
                + " String, boolean, int, long, double, an enum, a record, a List of String, of an enum, of a"
                + " record or of a sealed interface of records, or Map<String, String>");
    }

    /** The {@code index}th type argument of {@code generic} when it is a plain class; {@code null} otherwise. */
    private static Class<?> element(Type generic, int index) {
        if (generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments()[index] instanceof Class<?> argument) {
            return argument;
        }
        return null;
    }
}

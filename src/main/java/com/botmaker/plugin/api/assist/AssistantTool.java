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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A tool a plugin offers the AI assistant — see the package comment for the declaration.
 *
 * <p>Steps: {@link #named} → {@link Describing#describedAs} → {@link Taking#takes} (a record) or
 * {@link Taking#takesNothing} → {@link Handling#handledBy} (a method reference). The record's components are
 * read once, when the tool is declared, into {@link #params()}; a component the schema cannot say is refused
 * then, naming the record and the component, so a bad tool fails the plugin's own class initialisation and
 * tests rather than an assistant's call.
 *
 * <p>The host serves the tool under a name prefixed with the plugin's, so two plugins' {@code screenshot}s do
 * not collide; the name here is unique within the plugin.
 *
 * @param <P> the parameter record
 */
public final class AssistantTool<P extends Record> {

    /** Answers one call: the parameters the assistant sent, built into the record. A method reference. */
    @FunctionalInterface
    public interface Handler<P extends Record> {
        AgentReply handle(P params, AgentContext context);
    }

    /** The parameters of a tool that takes none. */
    public record None() {
    }

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_]{0,47}");

    private final String name;
    private final String description;
    private final Class<P> type;
    private final List<ToolParam> params;
    private final Handler<P> handler;

    private AssistantTool(String name, String description, Class<P> type, Handler<P> handler) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.params = schema(type);
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

    /** After {@link Describing#describedAs}: the parameter record. */
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
            return new AssistantTool<>(name, description, type, Objects.requireNonNull(handler, "handler"));
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    /** The parameter record. */
    public Class<P> paramsType() {
        return type;
    }

    /** The input schema, one entry per record component, in declared order. */
    public List<ToolParam> params() {
        return params;
    }

    /**
     * Runs the tool on what the assistant sent — a JSON object already read into {@code String},
     * {@code Boolean}, {@code Number} and {@code List} values. Never throws: an argument that does not fit, a
     * missing or unknown one, and a handler that throws all answer a {@link AgentReply#refused} saying so.
     */
    public AgentReply invoke(Map<String, ?> arguments, AgentContext context) {
        P built;
        try {
            built = build(arguments == null ? Map.of() : arguments);
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

    private P build(Map<String, ?> arguments) {
        RecordComponent[] components = type.getRecordComponents();
        Set<String> unknown = new LinkedHashSet<>(arguments.keySet());
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            ToolParam param = params.get(i);
            unknown.remove(param.name());
            Object sent = arguments.get(param.name());
            if (sent == null) {
                if (!param.optional()) throw new IllegalArgumentException("'" + param.name() + "' is required");
                values[i] = null;
            } else {
                values[i] = convert(param, component.getType(), sent);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("no parameter " + unknown + "; it takes "
                    + params.stream().map(ToolParam::name).toList());
        }
        try {
            Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
            Constructor<P> constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new IllegalArgumentException(cause.getMessage() == null ? cause.toString() : cause.getMessage(), cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build " + type.getName(), e);
        }
    }

    private static Object convert(ToolParam param, Class<?> target, Object sent) {
        String key = "'" + param.name() + "'";
        return switch (param.kind()) {
            case TEXT -> {
                if (sent instanceof CharSequence text) yield text.toString();
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
            case CHOICE -> {
                for (Object constant : target.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(sent.toString().trim())) yield constant;
                }
                throw new IllegalArgumentException(key + " is one of " + param.choices() + ", not " + sent);
            }
            case TEXT_LIST -> {
                if (!(sent instanceof Collection<?> list)) throw new IllegalArgumentException(key + " is a list of text");
                List<String> texts = new ArrayList<>();
                for (Object element : list) {
                    if (!(element instanceof CharSequence text)) {
                        throw new IllegalArgumentException(key + " is a list of text, not " + element);
                    }
                    texts.add(text.toString());
                }
                yield List.copyOf(texts);
            }
        };
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

    private static List<ToolParam> schema(Class<? extends Record> type) {
        List<ToolParam> params = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            Describe describe = component.getAnnotation(Describe.class);
            boolean optional = describe != null && describe.optional();
            Class<?> raw = component.getType();
            String where = type.getName() + "." + component.getName();
            if (optional && raw.isPrimitive()) {
                throw new IllegalArgumentException(where + " is optional, so it must be a box, not " + raw);
            }
            ToolParam.Kind kind = kind(raw, component.getGenericType(), where);
            List<String> choices = kind == ToolParam.Kind.CHOICE
                    ? Arrays.stream(raw.getEnumConstants()).map(c -> ((Enum<?>) c).name().toLowerCase(Locale.ROOT)).toList()
                    : List.of();
            params.add(new ToolParam(component.getName(), kind, describe == null ? "" : describe.value(), optional,
                    choices));
        }
        return List.copyOf(params);
    }

    private static ToolParam.Kind kind(Class<?> raw, Type generic, String where) {
        if (raw == String.class) return ToolParam.Kind.TEXT;
        if (raw == boolean.class || raw == Boolean.class) return ToolParam.Kind.YES_NO;
        if (raw == int.class || raw == Integer.class || raw == long.class || raw == Long.class) {
            return ToolParam.Kind.WHOLE_NUMBER;
        }
        if (raw == double.class || raw == Double.class) return ToolParam.Kind.NUMBER;
        if (raw.isEnum()) return ToolParam.Kind.CHOICE;
        if (raw == List.class && generic instanceof ParameterizedType list
                && list.getActualTypeArguments()[0] == String.class) {
            return ToolParam.Kind.TEXT_LIST;
        }
        throw new IllegalArgumentException(where + " is a " + generic.getTypeName() + "; a tool parameter is"
                + " String, boolean, int, long, double, an enum or List<String>");
    }
}

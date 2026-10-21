package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.assist.ToolSchemas.Actions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

    private final String name;
    private final String description;
    private final Class<P> type;
    private final ToolSchema schema;
    private final Function<Map<String, ?>, P> builder;
    private final Handler<P> handler;

    private AssistantTool(String name, String description, Class<P> type, ToolSchema schema,
                          Function<Map<String, ?>, P> builder, Handler<P> handler) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.schema = schema;
        this.builder = builder;
        this.handler = handler;
    }

    /** The first step: the tool's name, {@code snake_case}, unique within its plugin: {@code "cut_picture"}. */
    public static Describing named(String name) {
        if (name == null || !ToolSchemas.NAME.matcher(name).matches()) {
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
            List<ToolSchema.Field> fields = ToolSchemas.fieldsOf(type, new HashSet<>());
            return new AssistantTool<>(name, description, type, new ToolSchema(fields, List.of()),
                    sent -> ToolArguments.record(type, fields, sent, "", ""),
                    Objects.requireNonNull(handler, "handler"));
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
            return new AssistantTool<>(name, description, type, actions.schema(),
                    sent -> new OneOf<>(ToolArguments.action(actions, sent, "")),
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
}

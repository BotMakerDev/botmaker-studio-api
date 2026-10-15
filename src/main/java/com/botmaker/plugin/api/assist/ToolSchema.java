package com.botmaker.plugin.api.assist;

import java.util.List;

/**
 * An {@link AssistantTool}'s whole input, as a tree the host writes into the schema the assistant sees: the
 * parameter record's components, each nested record's own components under it, and, for a tool declared by
 * {@link AssistantTool.Taking#takesOneOf}, its actions. Read by the host only; a plugin declares the record and
 * {@link AssistantTool#schema()} derives this from it.
 *
 * <p>A tool that takes one of several actions is <b>flat on the wire</b>: its first field is {@code action}, a
 * {@link ToolParam.Kind#CHOICE} of the action ids, and the others are the union of the actions' fields, each
 * optional, with {@link Field#actions()} naming the actions that take it. Which of them an action needs is
 * {@link Action#required()}; the tool refuses a call that leaves one out or sends a field its action does not
 * take, so the host need not say it in the schema's own terms (no {@code oneOf}, which some assistants read
 * poorly).
 */
public final class ToolSchema {

    private final List<Field> fields;
    private final List<Action> actions;

    ToolSchema(List<Field> fields, List<Action> actions) {
        this.fields = List.copyOf(fields);
        this.actions = List.copyOf(actions);
    }

    /** The top-level fields, in declared order; for an action tool, {@code action} first. */
    public List<Field> fields() {
        return fields;
    }

    /** The actions a tool declared by {@link AssistantTool.Taking#takesOneOf} takes, in permitted order; empty otherwise. */
    public List<Action> actions() {
        return actions;
    }

    /** One field: a record component, or the action tool's {@code action}. */
    public static final class Field {

        private final String name;
        private final ToolParam.Kind kind;
        private final String description;
        private final boolean optional;
        private final List<String> choices;
        private final List<Field> fields;
        private final List<String> actions;

        Field(String name, ToolParam.Kind kind, String description, boolean optional, List<String> choices,
              List<Field> fields, List<String> actions) {
            this.name = name;
            this.kind = kind;
            this.description = description == null ? "" : description;
            this.optional = optional;
            this.choices = List.copyOf(choices);
            this.fields = List.copyOf(fields);
            this.actions = List.copyOf(actions);
        }

        /** The key the assistant sends. */
        public String name() {
            return name;
        }

        /** What it holds. */
        public ToolParam.Kind kind() {
            return kind;
        }

        /** Its {@link Describe} sentence, {@code ""} without one. */
        public String description() {
            return description;
        }

        /** Whether the assistant may leave it out; every field of an action tool but {@code action} may, on the wire. */
        public boolean optional() {
            return optional;
        }

        /**
         * For {@link ToolParam.Kind#CHOICE} and {@link ToolParam.Kind#CHOICE_LIST}, the values it accepts (an enum's
         * constants, or the action ids), lower case; empty otherwise.
         */
        public List<String> choices() {
            return choices;
        }

        /** For {@link ToolParam.Kind#RECORD} and {@link ToolParam.Kind#RECORD_LIST}, the nested record's fields; empty otherwise. */
        public List<Field> fields() {
            return fields;
        }

        /** For an action tool's field, the ids of the actions that take it; empty when every action does, or for any other tool. */
        public List<String> actions() {
            return actions;
        }
    }

    /** One action of a tool declared by {@link AssistantTool.Taking#takesOneOf}: one permitted record. */
    public static final class Action {

        private final String id;
        private final String description;
        private final List<String> fields;
        private final List<String> required;

        Action(String id, String description, List<String> fields, List<String> required) {
            this.id = id;
            this.description = description == null ? "" : description;
            this.fields = List.copyOf(fields);
            this.required = List.copyOf(required);
        }

        /** What the assistant sends as {@code action}: the record's simple name in {@code snake_case}. */
        public String id() {
            return id;
        }

        /** The record's own {@link Describe} sentence, {@code ""} without one. */
        public String description() {
            return description;
        }

        /** The names of the fields it takes, in its record's order. */
        public List<String> fields() {
            return fields;
        }

        /** The names of the fields it needs: its record's components that are not optional. */
        public List<String> required() {
            return required;
        }
    }
}

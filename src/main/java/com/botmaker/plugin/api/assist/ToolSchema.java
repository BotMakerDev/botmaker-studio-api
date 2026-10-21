package com.botmaker.plugin.api.assist;

import java.util.List;

/**
 * An {@link AssistantTool}'s whole input, as a tree the host writes into the schema the assistant sees: the
 * parameter record's components, each nested record's own components under it, and, for a tool declared by
 * {@link AssistantTool.Taking#takesOneOf}, its actions. Read by the host only; a plugin declares the record and
 * {@link AssistantTool#schema()} derives this from it.
 *
 * <p>A tool that takes one of several actions is <b>flat on the wire</b>: its first field is {@code action}, a
 * {@link Field.Kind#CHOICE} of the action ids, and the others are the union of the actions' fields, each
 * optional, with {@link Field#actions()} naming the actions that take it. Which of them an action needs is
 * {@link Action#required()}; the tool refuses a call that leaves one out or sends a field its action does not
 * take, so the host need not say it in the schema's own terms (no {@code oneOf}, which some assistants read
 * poorly). A list of a sealed interface's records is the same, element by element ({@link Field#oneOf()}).
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

        /** What a field holds — the closed set a record component may be. */
        public enum Kind {
            /** {@code String}. */
            TEXT("string"),
            /** {@code boolean}/{@code Boolean}. */
            YES_NO("boolean"),
            /** {@code int}, {@code long} and their boxes. */
            WHOLE_NUMBER("integer"),
            /** {@code double}/{@code Double}. */
            NUMBER("number"),
            /** An enum: one of {@link Field#choices()}, its constants' names. */
            CHOICE("string"),
            /** {@code List<String>}. */
            TEXT_LIST("array"),
            /** A nested record: an object of its own components, {@link Field#fields()}. */
            RECORD("object"),
            /**
             * {@code List} of a record, or of a sealed interface of records: an array of objects. In a list of a
             * sealed interface, each object is one of its actions ({@link Field#oneOf()}).
             */
            RECORD_LIST("array"),
            /** {@code List} of an enum: an array of its constants' names, {@link Field#choices()}. */
            CHOICE_LIST("array"),
            /** {@code Map<String, String>}: an object of text, its keys the assistant's. */
            TEXT_MAP("object");

            private final String jsonType;

            Kind(String jsonType) {
                this.jsonType = jsonType;
            }

            /** The JSON Schema {@code type} it is written as. */
            public String jsonType() {
                return jsonType;
            }
        }

        private final String name;
        private final Kind kind;
        private final String description;
        private final boolean optional;
        private final List<String> choices;
        private final List<Field> fields;
        private final List<String> actions;
        private final List<Action> oneOf;

        Field(String name, Kind kind, String description, boolean optional, List<String> choices,
              List<Field> fields, List<String> actions, List<Action> oneOf) {
            this.name = name;
            this.kind = kind;
            this.description = description == null ? "" : description;
            this.optional = optional;
            this.choices = List.copyOf(choices);
            this.fields = List.copyOf(fields);
            this.actions = List.copyOf(actions);
            this.oneOf = List.copyOf(oneOf);
        }

        /** The key the assistant sends. */
        public String name() {
            return name;
        }

        /** What it holds. */
        public Kind kind() {
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
         * For {@link Kind#CHOICE} and {@link Kind#CHOICE_LIST}, the values it accepts (an enum's
         * constants, or the action ids), lower case; empty otherwise.
         */
        public List<String> choices() {
            return choices;
        }

        /** For {@link Kind#RECORD} and {@link Kind#RECORD_LIST}, the nested record's fields; empty otherwise. */
        public List<Field> fields() {
            return fields;
        }

        /** For an action tool's field, the ids of the actions that take it; empty when every action does, or for any other tool. */
        public List<String> actions() {
            return actions;
        }

        /**
         * For a {@link Kind#RECORD_LIST} of a sealed interface, its records as actions: each element is
         * flat, its {@code action} first and that action's fields beside it, as an action tool's top level is, and
         * {@link #fields()} is their union. Empty for any other field.
         */
        public List<Action> oneOf() {
            return oneOf;
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

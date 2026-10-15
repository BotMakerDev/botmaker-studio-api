package com.botmaker.plugin.api.assist;

import java.util.List;

/**
 * One top-level parameter of an {@link AssistantTool}, as its input schema says it: derived from a component of
 * the tool's parameter record. A nested record's fields and an action tool's actions are in the whole tree,
 * {@link AssistantTool#schema()}, which is what a host writes the assistant's schema from.
 *
 * @param name        the key the assistant sends: the component's name, or its {@link Key}
 * @param kind        what it holds
 * @param description the component's {@link Describe} sentence, {@code ""} without one
 * @param optional    whether the assistant may leave it out
 * @param choices     for {@link Kind#CHOICE} and {@link Kind#CHOICE_LIST}, the values it accepts; empty otherwise
 */
public record ToolParam(String name, Kind kind, String description, boolean optional, List<String> choices) {

    public ToolParam {
        choices = choices == null ? List.of() : List.copyOf(choices);
        if (description == null) description = "";
    }

    /** What a parameter holds — the closed set a record component may be. */
    public enum Kind {
        /** {@code String}. */
        TEXT("string"),
        /** {@code boolean}/{@code Boolean}. */
        YES_NO("boolean"),
        /** {@code int}, {@code long} and their boxes. */
        WHOLE_NUMBER("integer"),
        /** {@code double}/{@code Double}. */
        NUMBER("number"),
        /** An enum: one of {@link ToolParam#choices()}, its constants' names. */
        CHOICE("string"),
        /** {@code List<String>}. */
        TEXT_LIST("array"),
        /** A nested record: an object of its own components, {@link ToolSchema.Field#fields()}. */
        RECORD("object"),
        /**
         * {@code List} of a record, or of a sealed interface of records: an array of objects. In a list of a sealed
         * interface, each object is one of its actions ({@link ToolSchema.Field#oneOf()}).
         */
        RECORD_LIST("array"),
        /** {@code List} of an enum: an array of its constants' names, {@link ToolParam#choices()}. */
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
}

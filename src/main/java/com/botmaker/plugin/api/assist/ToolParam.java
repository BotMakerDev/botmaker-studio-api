package com.botmaker.plugin.api.assist;

import java.util.List;

/**
 * One parameter of an {@link AssistantTool}, as its input schema says it: derived from a component of the
 * tool's parameter record, read by the host to write the schema the assistant sees.
 *
 * @param name        the component's name, which is the key the assistant sends
 * @param kind        what it holds
 * @param description the component's {@link Describe} sentence, {@code ""} without one
 * @param optional    whether the assistant may leave it out
 * @param choices     for {@link Kind#CHOICE}, the values it accepts; empty otherwise
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
        TEXT_LIST("array");

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

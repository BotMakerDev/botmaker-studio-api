package com.botmaker.plugin.api.run;

import java.util.Locale;

/**
 * What a question {@link RunChannel#ask asked} of the person running the bot takes as its answer, which decides how
 * the host asks it: a text field, a number field, two buttons, a list. A closed set with stable ids, read totally.
 */
public enum AskKind {
    /** Any line of text. */
    TEXT("text", "Text"),
    /** A number, fractions allowed. */
    NUMBER("number", "Number"),
    /** A whole number. */
    WHOLE("whole", "Whole number"),
    /** Yes or no, answered {@code "true"} or {@code "false"}. */
    YES_NO("yes-no", "Yes or no"),
    /** One of the question's choices, answered as the choice's text. */
    CHOICE("choice", "Choice"),
    /** A kind this host does not know, from a newer plugin: asked as {@link #TEXT}. */
    UNKNOWN("", "Other");

    private final String id;
    private final String displayName;

    AskKind(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** The stable id, what a wire carries. */
    public String id() {
        return id;
    }

    /** The kind's name, as a person reads it. */
    public String displayName() {
        return displayName;
    }

    /** The kind with this id, ignoring case, or {@link #UNKNOWN}; never throws. */
    public static AskKind fromId(String id) {
        if (id == null || id.isBlank()) return UNKNOWN;
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (AskKind kind : values()) {
            if (kind != UNKNOWN && kind.id.equals(wanted)) return kind;
        }
        return UNKNOWN;
    }
}

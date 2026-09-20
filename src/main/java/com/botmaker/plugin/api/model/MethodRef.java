package com.botmaker.plugin.api.model;

/**
 * A reference to a method in the bot's own Java, written {@code Owner::method}.
 *
 * <p><b>Not a value, and that is why it is its own thing.</b> Every other argument of a model statement is a
 * value with a {@link com.botmaker.plugin.api.value.ValueType} and a codec that can write it and read it
 * back. A method reference has no type of its own — its type is whatever functional interface the parameter
 * declares, which differs per call — and no codec could honestly claim that {@code Collect::body} is a
 * literal of some type. So it crosses as what it is: two names.
 *
 * <p><b>Why a model names the bot's code at all.</b> A plugin's model refers to work the user wrote — the
 * body of an activity, a handler, a check. Naming it by string means a rename in Java leaves a model
 * pointing at nothing, discovered three screens into a run. Naming it by method reference means the same
 * rename is a <em>compile error</em> in the generated file, which is the whole reason the model is Java.
 *
 * <p>Both names are as they are written, never resolved: the host reads a bot's source without bindings, and
 * the file that most needs to be read is the one the user has half-edited.
 *
 * @param owner  the class the method is declared on, qualified — {@code com.example.bot.Collect}
 * @param method the method's name, with no arguments and no brackets — {@code body}
 */
public record MethodRef(String owner, String method) {

    public MethodRef {
        owner = owner == null ? "" : owner.strip();
        method = method == null ? "" : method.strip();
    }

    /** The class's own name, as a file importing {@link #owner()} would write it. */
    public String simpleOwner() {
        int lastDot = owner.lastIndexOf('.');
        return lastDot < 0 ? owner : owner.substring(lastDot + 1);
    }

    /** Whether this names anything at all. A half-written reference is not one. */
    public boolean isPresent() {
        return !owner.isEmpty() && !method.isEmpty();
    }
}

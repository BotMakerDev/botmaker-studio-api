package com.botmaker.plugin.api.model;

import java.util.List;

/**
 * One line of a plugin's model: which call, and what its arguments are.
 *
 * <p>This is what crosses the contract, in both directions, and **no text crosses with it**. The plugin says
 * *call {@code Activities.declare} with these six things*; the host writes the Java and reads it back. A
 * plugin writes no emitter, no parser and no template.
 *
 * <p><b>Both directions from one declaration.</b> The alternative considered was a write callback, and it
 * fails for a measured reason: the half nobody is forced to write is the half that rots.
 * {@code ValueCodec.literal} had seventeen implementations and {@code wireOfLiteral} — a {@code default}
 * returning empty — had nine, because the reader had exactly one caller. Here there is one description, and
 * the host derives both halves from it.
 *
 * <p><b>Order is meaning.</b> A model is a <em>list</em> of these, written in order and read back in order,
 * because that is what a sequence of statements is. A plugin that needs one call to come before another gets
 * that for free and does not have to say so anywhere.
 *
 * @param call      the {@link ModelCall#id()} of the call to write
 * @param arguments one value per {@link ModelCall#arguments()}, in order; a {@link ModelCall.Argument.Body}
 *                  takes a {@link MethodRef} and every other takes a value of its declared form
 */
public record ModelStatement(String call, List<Object> arguments) {

    public ModelStatement {
        call = call == null ? "" : call.strip();
        // Unmodifiable rather than List.copyOf, which rejects a null element with a NullPointerException
        // from inside a record constructor. A null argument is not writable, but the useful place to say so
        // is the writer, which can name which argument it was.
        arguments = arguments == null ? List.of()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(arguments));
    }

    /** A statement of {@code call} with the arguments given in order. */
    public static ModelStatement of(ModelCall call, Object... arguments) {
        return new ModelStatement(call == null ? "" : call.id(),
                arguments == null ? List.of() : java.util.Arrays.asList(arguments));
    }
}

package com.botmaker.plugin.api.slot;


import java.util.List;
import java.util.Optional;

/**
 * Several sibling slots that are edited as <b>one</b> thing.
 *
 * <p>A {@link SlotContext} is one argument of one call, and that is the right shape for almost everything.
 * It is the wrong shape for a value the author writes as a <em>run</em> of arguments — three pictures to
 * match any of, four keys to try in order — where an editor confined to a single argument can change one
 * element and can never add or remove one. Such an editor has to hand back the whole run at once, which is
 * what {@link #replace} is for.
 *
 * <p><b>Elements cross as values, like every other value.</b> The host reads each argument through the
 * grammar it reads a slot with — a constant of a {@code @Managed} type included — and writes each value
 * back the same way. What the host contributes is what only the host has: that these arguments are one
 * list, and what the surrounding code will still accept. Until 2026-09-23 every element was Java text the
 * plugin split and wrote itself.
 *
 * <p>Reached through {@link SlotContext#siblingRun()}, which is empty for a slot that stands alone. An editor
 * that does not care simply never asks.
 */
public interface SlotRun {

    /**
     * One argument of the run: its value, or {@code null} when the host cannot read it, and what the file
     * writes, for showing an element that has no value.
     *
     * <p>Handing one back to {@link #replace} keeps that argument exactly as written. That is how an editor
     * rewrites the run around a variable or a call it cannot read without deleting it.
     *
     * @param value  the element as a value, or {@code null} when the grammar cannot read it
     * @param source the element as the file writes it, for display only
     */
    record Element(Object value, String source) {

        public Element {
            source = source == null ? "" : source;
        }

        /** The value as a {@code T}, or empty when it is unreadable or another type. */
        public <T> Optional<T> value(Class<T> type) {
            return type != null && type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }
    }

    /** Each argument, in the order they appear. Never null; empty is a legal state. */
    List<Element> elements();

    /**
     * How few elements the surrounding code will still compile with, or {@code 0} when the run may be
     * emptied.
     *
     * <p>The host knows this and the plugin cannot: a guarded branch may need at least one element to stay
     * a guarded branch. An editor uses it to <em>disable</em> removal at the floor rather than to hide it,
     * so the reader still sees that removal exists.
     */
    default int minimum() {
        return 0;
    }

    /**
     * The only values the surrounding code can still use, or empty when anything goes.
     *
     * <p>The host computes this from the code around the run (the elements of the list a branch narrows
     * against, say). Empty means <em>anything goes</em> and {@code List.of()} means <em>nothing is
     * allowed</em>; an {@link Optional} cannot be iterated by accident, which a nullable list could.
     */
    default Optional<List<Object>> allowed() {
        return Optional.empty();
    }

    /**
     * Replaces the whole run.
     *
     * <p>Each item is a value the host writes, as {@link ValueContext#set} would, or an {@link Element} from
     * {@link #elements()}, kept exactly as written. The host refuses the whole list, leaving the source
     * alone, when it has fewer items than {@link #minimum()} or an item it cannot write. Call it on the
     * JavaFX application thread.
     */
    void replace(List<?> elements);
}

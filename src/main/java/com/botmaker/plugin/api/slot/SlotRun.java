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
 * <p><b>Everything here is opaque source text, and that is the point.</b> The elements are Java
 * expressions exactly as they stand in the file; nothing in this interface names a type, a picture, a key
 * or any other concept belonging to one plugin. What the host contributes is the thing only the host has —
 * knowing that these arguments are one list, and what the surrounding code will still accept — and what
 * the plugin contributes is knowing what the strings mean. That is the same division
 * {@link SlotContext#replaceEnclosingCall} is built on.
 *
 * <p>Reached through {@link SlotContext#run()}, which is {@code null} for a slot that stands alone. An
 * editor that does not care simply never asks, and keeps working exactly as before.
 */
public interface SlotRun {

    /**
     * Each element's Java source, in the order they appear — {@code ["new ImageTemplate(\"gold.png\")",
     * "new ImageTemplate(\"ore.png\")"]}. Never null; empty is a legal state (a call written with no
     * arguments yet).
     *
     * <p>Parsing an element is the editor's own job, and failing to is normal: any of them may be a
     * variable, a field or a call rather than the literal shape the editor writes. An element it cannot
     * read must be left alone rather than overwritten — {@link #replace} takes the whole run, so an editor
     * that cannot represent an element cannot safely rewrite the run at all.
     */
    List<String> elements();

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
     * The only element sources the surrounding code can still use, or empty when anything goes.
     *
     * <p>Java source again, never decoded values — the host computes this by looking at the code around the
     * run (the elements of the list a branch is narrowing against, say), which it can do without knowing
     * what any of them mean. An editor offers exactly these and nothing else; an empty {@code Optional} is
     * the ordinary case and means "the whole library".
     *
     * <p><b>It is an {@link Optional} because the two absences here are opposite and the dangerous one is
     * the common case.</b> Empty means <em>anything goes</em> and {@code List.of()} means <em>nothing is
     * allowed</em>, so a plain {@code List} return would have had {@code null} for the first: then
     * {@code for (String s : run.allowed())}, which is what an author writes without thinking, throws
     * {@link NullPointerException} on nearly every slot, and the one shape that does not throw is the shape
     * that must offer nothing. An {@code Optional} cannot be iterated by accident.
     */
    default Optional<List<String>> allowedSources() {
        return Optional.empty();
    }

    /**
     * Replaces the whole run with {@code javaExpressions}, adding any imports they need.
     *
     * <p>Same rules as {@link ValueContext#set}: source text the host re-parses, fully-qualified
     * names are always safe, on the JavaFX application thread, and repeatable. Passing fewer expressions
     * than {@link #minimum()} is refused by the host and leaves the source alone, so an editor gets the
     * honest outcome rather than code that will not compile.
     *
     * @param javaExpressions one Java expression per element, never statements and never blank
     * @param importsNeeded   fully-qualified type names the expressions refer to by simple name
     */
    void replace(List<String> javaExpressions, String... importsNeeded);
}

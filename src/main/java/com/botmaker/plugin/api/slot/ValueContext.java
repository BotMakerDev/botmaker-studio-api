package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;

import java.util.Optional;

/**
 * A value being edited, with no syntax tree and no source text anywhere an editor has to look.
 *
 * <p>This is the half of {@link SlotContext} that does not need a call site: a type, the value, a way to
 * write one back, and the host services. It exists because the host edits values in more than one place and
 * only some of them are a call — a slot in a bot's Java, a row in the Parameters window, and the expression
 * a {@code @Managed} method returns — and until this interface there was no way for one editor to serve
 * them all.
 *
 * <h2>The value is a value, and that is the whole design</h2>
 *
 * <p>{@link #value(Class)} answers the {@code Duration}, the {@code Point}, the {@code java.awt.Color} that
 * stands in the user's file, and {@link #set(Object)} writes one back. <b>The host owns the grammar in both
 * directions</b>: it reads the expression through the {@link com.botmaker.plugin.api.value.ComponentType}
 * the owning plugin registered and writes it back the same way.
 *
 * <p><b>A value, not Java text.</b> Text would have every plugin that wants a typed value parse it, each
 * parser disagreeing with the next, and the host round-tripping a value through a decode it cannot verify.
 * There is one reader and it is the host's, and it reads any shape a bot declares —
 * {@code Map<String, List<Point>>} included.
 *
 * <h2>The type crosses as a name, never as a {@code Class}</h2>
 *
 * <p>{@link #type()} names the Java type as a {@link TypeRef} — see the module's rule 2. That is
 * deliberately the <em>same</em> discriminator a slot in source uses, which is what makes "one editor,
 * everywhere" true rather than aspirational.
 */
public interface ValueContext {

    /**
     * The type of the value being edited.
     *
     * <p>May be {@linkplain TypeRef#isResolved() unresolved} — a slot the host could not type, or a value
     * whose type no loaded plugin declares. An editor must treat that as "not mine" rather than as an
     * error.
     */
    TypeRef type();

    /**
     * The current value, as the plugin's own type, or empty.
     *
     * <p><b>Empty is normal and is not an error.</b> It means the expression in the user's file is not one
     * the grammar can read — a hand-written initializer, a variable, {@code target.center()}, a call from
     * somewhere else — and the honest thing for an editor to do is render read-only and leave what the
     * author wrote alone. It is also empty for a value that has never been set.
     *
     * <p>The value comes back through the {@link com.botmaker.plugin.api.value.ComponentType} the owning
     * plugin registered, so {@code T} never crosses a classloader: the host hands back only what that
     * plugin's own {@code build} produced. Asking for a type no plugin owns answers empty.
     */
    <T> Optional<T> value(Class<T> type);

    /**
     * Replaces the value with {@code value}, writing whatever Java spells it and adding the imports it
     * needs.
     *
     * <p>Call it on the JavaFX application thread. Calling it repeatedly is fine — each call replaces what
     * the previous one wrote — which is what lets a live editor track a drag.
     *
     * <p>The host writes it through the {@link com.botmaker.plugin.api.value.ComponentType} that owns
     * {@code value}'s class, or as a JDK literal, or as an enum constant. A value of a type no plugin owns
     * is ignored rather than written half-way: there is no expression for it.
     *
     * <p><b>The host owns every character of syntax around this.</b> It writes the expression into the node
     * it came from and reformats nothing else, so a file that had comments and helpers beside the value
     * still has them afterwards.
     *
     * <p>A value the host cannot write is told to the user on the status line. An editor that wants to know
     * itself — to keep the old value shown, or to say why — calls {@link #write} instead.
     */
    void set(Object value);

    /**
     * {@link #set}, answering whether it happened: empty when {@code value} was written, else the sentence
     * saying why not — a value of a type no loaded plugin writes, or none at all. A refused value leaves what
     * the file held untouched.
     *
     * <p>{@code default} for a host older than this method, which answers empty after an ordinary
     * {@link #set}: it cannot say more, and an editor written against this one must still run there.
     */
    default Optional<String> write(Object value) {
        set(value);
        return Optional.empty();
    }

    /**
     * The value exactly as the bot's Java writes it — never {@code null}, {@code ""} when there is none.
     *
     * <p>The escape hatch, and only that. Read it to <em>show</em> an expression {@link #value(Class)}
     * could not decode, so the user sees what is actually in their file; do not parse it. Everything that
     * used to parse it is the host's job now.
     */
    String source();

    /** The host services an editor may use: theming, dialogs, the open project and its values. */
    StudioServices services();

    /**
     * This context's source-code half, or empty when there is none.
     *
     * <p>The one place an editor is entitled to ask <em>where</em> it is being shown. A few editors are
     * chosen by the call they sit in rather than by their type (a Steam app id and a window title are both
     * {@code String}), and those need a call site; everything else should not know the difference.
     *
     * <p>A row in the Parameters window and a {@code @Managed} value both have no call site, so this is
     * empty there — which is the case an editor most often forgets, because it is the one that never
     * happens while the editor is being written against a slot in source.
     */
    default Optional<SlotContext> slot() {
        return Optional.empty();
    }

    /**
     * The range the value is declared to stay within, {@link Bounds#NONE} when nothing is declared.
     *
     * <p>A capability only the host has: the limit is a rule about the <em>field</em>, written as
     * {@code @Param(min = …, max = …)} on the bot's own declaration, and the value alone does not carry it. A
     * number editor stops its stepper at the ends and clamps what is typed; the host also clamps what
     * {@link #set(Object)} writes, so an editor that ignores this cannot write a value outside it.
     */
    default Bounds bounds() {
        return Bounds.NONE;
    }
}

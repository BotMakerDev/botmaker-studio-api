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
 * <p><b>It was a {@code String} of Java source until 2026-09-22</b>, and every plugin that wanted a typed
 * value parsed it. That produced three numeric-literal strippers, two argument splitters and one string
 * unescaper across two modules, none agreeing; and the host, holding only text, round-tripped a value
 * through a decode it could not verify — a {@code java.awt.Color} parameter opened and closed with no edit
 * came back rewritten. There is one reader now and it is the host's.
 *
 * <p><b>It was a {@code List<String>} before that</b> (until 2026-09-20), the wire form a JSON file held.
 * That form could say a leaf and a flat list of leaves and nothing else, so an editor could never be handed
 * {@code Map<String, List<Point>>}, even though a bot may perfectly well declare such a field.
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
     */
    void set(Object value);

    /**
     * The value exactly as the bot's Java writes it — never {@code null}, {@code ""} when there is none.
     *
     * <p>The escape hatch, and only that. Read it to <em>show</em> an expression {@link #value(Class)}
     * could not decode, so the user sees what is actually in their file; do not parse it. Everything that
     * used to parse it is the host's job now.
     */
    String source();

    /**
     * Replaces the value with a raw Java expression, adding {@code imports}.
     *
     * <p>For the editor that does not write a value of its own type but rewrites the shape of the call —
     * the SDK's duration picker turning {@code Wait.time(x)} into {@code Wait.between(min, max)}. Prefer
     * {@link #set(Object)} everywhere else: this one is text the compiler never looked at.
     *
     * <p>Imports are named as {@code Class} objects rather than as strings, because a nested type is
     * {@code Outer.Inner} in both an expression and an import and {@code Outer$Inner} in neither, and every
     * caller was deriving that by hand. The host reads their canonical names and never loads them.
     *
     * <p><b>Its own name rather than a second {@code set}.</b> An overload pair would put
     * {@code set("Source.current()")} on the value path — a {@code String} argument matches
     * {@link #set(Object)} before varargs are ever considered — so the expression would be written back as
     * a quoted string literal, silently, with nothing to catch it. Two verbs for two meanings.
     */
    void setSource(String javaExpression, Class<?>... imports);

    /** The host services an editor may use: theming, screen capture, dialogs, and the project's location. */
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
}

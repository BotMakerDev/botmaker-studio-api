package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

/**
 * A type this plugin owns: what it is, what a fresh one is, and how a person edits one.
 *
 * <p><b>One declaration per type, and every method abstract.</b> That is the whole reason this interface
 * exists. A type used to need four separate declarations that nothing checked against each other — a
 * {@code ValueType} with an id, a {@code ValueCodec} with its four string methods, a {@code SourceSeed} with
 * the fresh expression as text, and a {@code SlotEditor} with a predicate naming the type a third time. For
 * {@code Point} those lived in three files, and the id, the class literal and the seed expression agreed
 * only because somebody kept them agreeing. Here the compiler asks for all four at once, in one class, and
 * {@link #type()} is the only place the type is named.
 *
 * <h2>{@link #fresh()} returns a {@code T}, not a string</h2>
 *
 * <p>A default value used to be a Java expression the plugin wrote as text ({@code "new Point(0, 0)"}),
 * which javac never looked at: a renamed class, a removed constructor or a typo produced a seed the host
 * wrote into somebody's file and then could not compile. A real {@code T} cannot be any of those. The host
 * writes it out through this plugin's own {@link ComponentType}, or as a JDK literal, or as the enum
 * constant it is — see {@link ComponentType} for the grammar.
 *
 * <h2>Nothing here parses</h2>
 *
 * <p>There is no {@code parse(String)} and no {@code valueOfLiteral(String)}. <b>A plugin never reads Java
 * source</b>; the host owns every character of syntax in both directions, which is what lets one grammar
 * serve every plugin at once and what stopped a colour parameter being rewritten on open. An editor is
 * handed the value, through {@link ValueContext#value(Class)}, and writes one back through
 * {@link ValueContext#set(Object)}.
 *
 * @param <T> the plugin's own type. It never crosses a classloader: the host matches the {@link #type()}
 *            this plugin handed it and gives back only what this plugin's own {@code build} produced.
 */
public interface PluginType<T> {

    /**
     * The class this type is. <b>Read, never loaded</b> — the host takes its canonical name off the object
     * the plugin already holds, and compares names, exactly as the palette catalog does.
     */
    Class<T> type();

    /**
     * A freshly created value of this type — what a new parameter, a new slot and a new list item start as.
     *
     * <p>Asked every time a value is seeded and never cached, so it may read the plugin's live state. It
     * must not throw and must not answer {@code null}: a type that cannot say what a fresh one is cannot be
     * offered in a type picker, and the host would have nothing to write.
     */
    T fresh();

    /**
     * The control a person edits one of these with. Called on the JavaFX application thread, and only after
     * the host decided this type is the one being edited.
     *
     * <p>Read the current value with {@link ValueContext#value(Class)} — which answers empty when the
     * expression in the file is one the grammar cannot read, the case that must render read-only rather
     * than overwrite — and write one back with {@link ValueContext#set(Object)}.
     */
    Node editor(ValueContext ctx);

    /**
     * A small, non-interactive picture of the value — a swatch, a thumbnail, a label — or {@code null}.
     *
     * <p>Shown where a value appears but is not edited: beside a declared choice in the list an author picks
     * from. {@link ValueContext#set} does nothing there.
     */
    default Node preview(ValueContext ctx) {
        return null;
    }
}

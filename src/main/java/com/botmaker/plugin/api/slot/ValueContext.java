package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.value.ValueForm;

import java.util.Optional;

/**
 * A value being edited, with no syntax tree anywhere in sight.
 *
 * <p>This is the half of {@link SlotContext} that does not need a call site: a type, the value as the bot's
 * Java writes it, a way to write it back, and the host services. It exists because the host edits values in
 * more than one place and only some of them are a call — a slot in a bot's Java, a row in the Parameters
 * window, and the expression a {@code @Managed} method returns ({@link PluginValues}) — and until this
 * interface there was no way for one editor to serve them all.
 *
 * <h2>The value is Java source, and that is the whole design</h2>
 *
 * <p>{@link #source()} is the expression as it stands in the user's file — {@code Duration.ofSeconds(3)},
 * {@code new Rect(12, 40, 300, 80)}, {@code java.util.List.of("WON", "LOST")} — and {@link #set} writes one
 * back. A value's canonical form is its Java initialiser, which is what {@code docs/refactor/32-generic-values.md}
 * settled: {@code ValueCatalog.initializer(form, value)} produces it and {@code valueOf(form, source)} reads
 * it, so an editor that wants a typed value asks the catalog rather than parsing anything itself.
 *
 * <p><b>It was a {@code List<String>} until 2026-09-20</b>, the wire form a JSON file held. That form could
 * say a leaf and a flat list of leaves and nothing else, so an editor could never be handed
 * {@code Map<String, List<Point>>}, or anything else with two arguments or two levels, even though a bot may
 * perfectly well declare such a field. Worse, it was a second encoding of a value beside the Java one, kept
 * in step by hand — and the half nobody is forced to write is the half that rots, which
 * {@code ValueCodec.literal} had already proved once.
 *
 * <h2>The form, not just the type</h2>
 *
 * <p>{@link #type()} names the Java type as a {@link TypeRef} and is what an editor for a leaf matches on.
 * {@link #form()} is the whole {@link ValueForm} tree, so an editor may claim a composite —
 * {@code List<Activity>}, or a plugin's own container — which a type name alone cannot describe. An editor
 * that only ever handles one leaf can keep reading {@link #type()} and ignore this.
 *
 * <p><b>The type crosses as a {@link TypeRef}, never as a {@code Class}</b> — see the module's rule 2. A
 * value-vocabulary type reaches an editor as the Java type it is written as
 * ({@code com.botmaker.sdk.api.vision.Precision}, {@code java.time.Duration}), which is deliberately the
 * <em>same</em> discriminator a slot in source uses. That is what makes "one editor, everywhere" true rather
 * than aspirational: an editor matching on the type matches in all of them.
 */
public interface ValueContext {

    /**
     * The type of the value being edited.
     *
     * <p>May be {@linkplain TypeRef#isResolved() unresolved} — a slot the host could not type, or a value
     * whose type no loaded plugin registers. An editor must treat that as "not mine" rather than as an
     * error.
     */
    TypeRef type();

    /**
     * The declared type as a whole tree, for an editor that claims a composite rather than a leaf.
     *
     * <p>Never {@code null}: a value the host could not type at all answers a leaf of
     * {@code ValueType.unknown}, which is the state that already means <em>nothing registered this</em>.
     * {@link ValueForm#known()} is the question worth asking before offering to write one.
     */
    ValueForm form();

    /**
     * The value as the bot's Java writes it — never {@code null}, {@code ""} when there is none.
     *
     * <p>An expression, not a statement and not a declaration: what stands to the right of the {@code =}, or
     * inside the brackets of a call. Parsing it is the editor's own job and failing to parse it is normal —
     * the user may have typed anything, and a value may have been written by a newer version of the plugin
     * than the one reading it. Degrade to a default; never throw. {@code ValueCatalog.valueOf(form(),
     * source())} is the parse most editors want, and it answers empty rather than throwing.
     */
    String source();

    /**
     * Replaces the value with the Java expression {@code javaExpression}, adding {@code importsNeeded}.
     *
     * <p>Call it on the JavaFX application thread. Calling it repeatedly is fine — each call replaces what
     * the previous one wrote — which is what lets a live editor track a drag. Passing {@code ""} clears the
     * value where clearing is legal and is ignored where it is not.
     *
     * <p>Imports are fully-qualified type names the expression uses; the host adds the ones the file does not
     * already have and never removes any. An expression that is already fully qualified needs none.
     * {@code ValueCatalog.imports(form)} answers what a catalog-written initialiser needs.
     *
     * <p><b>The host owns every character of syntax around this.</b> It writes the expression into the node
     * it came from and reformats nothing else, so a file that had comments and helpers beside the value
     * still has them afterwards.
     */
    void set(String javaExpression, String... importsNeeded);

    /** The host services an editor may use: theming, screen capture, dialogs, and the project's location. */
    StudioServices services();

    /**
     * This context's source-code half, or empty when there is none.
     *
     * <p>The one place an editor is entitled to ask <em>where</em> it is being shown. A few editors are
     * chosen by the call they sit in rather than by their type (a Steam app id and a window title are both
     * {@code String}), and those need a call site; everything else should not know the difference. It is a
     * method rather than an {@code instanceof} so that the question reads as a question in plugin code, and
     * so the host may one day answer it with something other than "am I an instance of".
     *
     * <p>A row in the Parameters window and a {@code @Managed} value both have no call site, so this is empty
     * there — which is the case an editor most often forgets, because it is the one that never happens while
     * the editor is being written against a slot in source.
     */
    default Optional<SlotContext> slot() {
        return Optional.empty();
    }
}

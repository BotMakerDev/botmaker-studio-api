package com.botmaker.plugin.api.value;

/**
 * What one {@link ValueType}'s stored text means, and how to write it back — both as storage and as Java
 * source. One codec per type, supplied by whichever plugin registers the type.
 *
 * <h2>Four methods, because there were three answers and two owners</h2>
 *
 * <p>Before the registry, the SDK held the readers and the source writers while the editor held its own
 * parallel copy of the readers plus the normalising rules — two answers to the question "what does
 * {@code "1h30m"} mean?", in two repositories, kept in step by hand. There is one now, and it is the
 * plugin's, which is the only party that can possibly know what a {@code Channel} is.
 *
 * <h2>Every method is total</h2>
 *
 * <p>Nothing here may throw and nothing may return {@code null}. A number that will not parse, a choice that
 * is no longer offered, a duration in a unit nobody knows: each answers the type's own default. That is not
 * defensiveness — <b>a project must still open, and still generate, when its file says something
 * impossible</b>, or it is a project nobody can repair through the editor. A codec that throws takes the
 * whole project down with it.
 *
 * <h2>One item at a time</h2>
 *
 * <p>A value is stored as a list of strings whatever its shape — one entry for an ordinary variable, one per
 * item for a list-shaped one — but a codec sees <b>one item</b>. The shape is composed above it, by
 * {@link ValueCatalog#initializer}, so that a codec is written once and works in both shapes without knowing
 * either. {@code List<Key>} needs nothing from a {@code Key} codec that a single {@code Key} did not.
 *
 * <h2>{@code T} never crosses to the host</h2>
 *
 * <p>The editor holds codecs behind a wildcard, so it never names {@code T} and never loads the plugin's
 * class. A value read by {@link #valueOfLiteral} comes back as an {@code Object} it only ever hands to
 * {@link #literal} or {@link #store} on the same codec, which is the same capture {@code literal(parse(wire))}
 * always was. That is what lets a plugin type exist in a project the host cannot itself model.
 *
 * @param <T> the parsed value; a plugin's own type, and invisible outside the plugin
 */
public interface ValueCodec<T> {

    /** What one stored item means. Total: unreadable text answers this type's default, never an exception. */
    T parse(String wire);

    /**
     * The stored form a freshly created value of this type starts with.
     *
     * <p>The default is {@code store(parse(""))} — "what does empty text mean, written back canonically" —
     * which is the right answer for every type whose default <em>is</em> its empty reading: a number that
     * starts at zero, a flag that starts false, an enum that starts at its first constant. Override it only
     * when the seed is a choice rather than a fallback: the SDK's image template starts at the placeholder
     * every project ships, because an empty chip is a value the bot cannot run on, and no amount of parsing
     * {@code ""} discovers that.
     *
     * <p>Total like everything else here, and a fixed point: {@code store(parse(defaultWire()))} must equal
     * {@code defaultWire()}, or a freshly created value changes the moment it is read back.
     */
    default String defaultWire() {
        return store(parse(""));
    }

    /** The stored form of one item — the input {@link #parse} reads back. Canonical, so a diff is stable. */
    String store(T value);

    /**
     * One item as Java source, ready to initialise a field.
     *
     * <p>Write the <em>parsed</em> value, never the text: {@code new java.awt.Color(255, 0, 0)} rather than
     * {@code Color.decode("#FF0000")}. A generated file must contain no expression that can throw at class
     * initialisation, which is what it means for a bot never to fail to start because of its own
     * configuration.
     *
     * <p>Anything outside {@code java.lang} is written fully qualified <em>unless</em> the type declares an
     * {@link ValueType#importName()}, in which case the simple name is used and the import is arranged.
     */
    String literal(T value);

    /**
     * The value a piece of Java source was written from — {@link #literal} read backwards.
     *
     * <p><b>Why this exists</b> (2026-09-17): a user parameter is a field in the bot's own Java now
     * ({@code @Param}), so its value <em>is</em> that field's initialiser, and an editor that can write one
     * but not read one can only ever offer to overwrite. {@code literal} writes the value structurally —
     * {@code java.time.Duration.ofMillis(3000L)}, {@code new java.awt.Color(255, 0, 0)} — precisely so a bot
     * cannot throw while starting, and nothing about those spellings can be undone without knowing the type.
     * Only the codec knows.
     *
     * <p><b>Empty means "I do not recognise this"</b>, and the host shows the source text read-only rather
     * than guessing. That is the honest answer for a hand-written initialiser the plugin never emits — a
     * method call, an expression over another field, a constant from elsewhere — and it keeps the value the
     * author wrote instead of replacing it with a default.
     *
     * <p><b>Total, and the round trip is the contract</b>: {@code valueOfLiteral(literal(v))} answers
     * {@code v} for every value this type can hold. Whitespace is the host's to normalise, not this
     * method's: the source arrives as written, and an implementation that cares should be tolerant rather
     * than exact. Never throws — an unparseable argument is not a recognised literal, which is the empty
     * answer.
     *
     * <p><b>It has no default, and that is the lesson of the one it replaced.</b> {@code wireOfLiteral}
     * declined by default, so a type could be registered without a reader and nothing said so: nine of the
     * seventeen shipped types implemented it and eight silently did not, which is a value the editor writes
     * and then refuses to edit. Abstract makes the inverse a thing the compiler asks for, at the one moment
     * the author has the literal in front of them.
     */
    java.util.Optional<T> valueOfLiteral(String javaSource);
}

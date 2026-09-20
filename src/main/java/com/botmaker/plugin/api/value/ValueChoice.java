package com.botmaker.plugin.api.value;

/**
 * A type as a variable declares it: one {@link ValueType}, in one of the {@link ValueShape}s.
 *
 * <p>The shape is an axis rather than four times as many types because it composes with all of them and
 * carries nothing of its own — {@code List<Point>} needs nothing from the catalogue that {@code Point} did
 * not already supply, beyond the box a primitive takes inside the angle brackets.
 *
 * <p><b>A record, and frozen as one</b> — see {@link Range} for why a component may never be added. A pair is
 * what this is.
 *
 * @param type  what kind of value
 * @param shape how many, and out of what set
 */
public record ValueChoice(ValueType type, ValueShape shape) {

    public ValueChoice {
        if (type == null) type = ValueType.unknown("");
        if (shape == null) shape = ValueShape.ONE;
        // Two shapes that a type cannot express are corrected rather than stored: a closed set has nothing
        // for an author-written subset to add, and a shape is never allowed to outlive the type it was
        // chosen for. Correcting here means every reader gets it — a file, a fixture, a caller's literal.
        if (shape == ValueShape.ONE_OF && !type.shapeable()) shape = ValueShape.ONE;
    }

    /** One free value of {@code type}. */
    public static ValueChoice of(ValueType type) {
        return new ValueChoice(type, ValueShape.ONE);
    }

    /** A list of {@code type}, filled in by whoever runs the bot. */
    public static ValueChoice listOf(ValueType type) {
        return new ValueChoice(type, ValueShape.OPEN_LIST);
    }

    /** Whether this is emitted as {@code List<T>}. */
    public boolean isList() {
        return shape.isList();
    }

    /** Whether the author writes down the set of values this may take. */
    public boolean hasOptions() {
        return shape.hasOptions();
    }

    /**
     * This pair as a {@link ValueForm}.
     *
     * <p>The other half of the bridge, and the lossless half: a {@code ValueChoice} is exactly a
     * {@link ValueForm.Leaf} or a {@code List} of one, so nothing it can say is dropped. What does not
     * survive is {@link #hasOptions()}, which is not a property of a type and is asked of the declaration
     * instead — {@code @Param(options = …)}, {@code ParameterRow.options()}, {@code ValueType.options()}.
     *
     * <p>Both directions exist so forms can arrive one caller at a time. Both are deleted with this record.
     */
    public ValueForm form() {
        ValueForm leaf = new ValueForm.Leaf(type);
        return isList() ? ValueForm.listOf(leaf) : leaf;
    }

    /** What the user is shown — "Point", "One of Point", "Many of Point", or "List of Point". */
    public String label() {
        return shape.prefix() + type.label();
    }

    /** How a generator writes this: {@code java.time.Duration}, or {@code List<Key>}. */
    public String sourceName() {
        return isList() ? "java.util.List<" + type.boxedName() + ">" : type.sourceName();
    }

    /**
     * Reads the persisted form, including the two spellings this replaced.
     *
     * <p>A variable's type is the one part of the file whose <em>vocabulary</em> changed. Files written
     * before the shape axis existed say {@code {"type":"CHOICE","list":false}} — {@code CHOICE} being a
     * pseudo-type meaning "text out of a written-down set", and {@code list} a boolean where the shape now
     * is. Migrating here rather than in an open-time pass means every reader gets it: the project loader, a
     * hand-copied file, a test fixture.
     *
     * <p>The type arrives as a {@code String} and is resolved through {@code catalog}, so an id nothing
     * registers becomes an {@linkplain ValueType#unknown unknown type} rather than a failed open. The shape
     * arrives as a {@code String} too, so that parse stays total as well.
     *
     * <p>What this cannot decide is {@link ValueShape#ANY_OF} versus {@link ValueShape#OPEN_LIST} for a file
     * written before they were split: the answer is whether the variable declares choices, and the choices
     * are a sibling field this factory never sees. The variable's own reader settles it, where both are in
     * hand.
     *
     * @param catalog what resolves an id; the legacy {@code CHOICE} pseudo-type never reaches it
     */
    public static ValueChoice fromWire(ValueCatalog catalog, String type, String shape, Boolean list) {
        boolean wasChoice = "CHOICE".equals(type);
        ValueType base = wasChoice ? catalog.text() : catalog.type(type);
        ValueShape resolved = shape != null ? ValueShape.fromWire(shape)
                : Boolean.TRUE.equals(list) ? ValueShape.ANY_OF
                : wasChoice ? ValueShape.ONE_OF
                : ValueShape.ONE;
        return new ValueChoice(base, resolved);
    }
}

package com.botmaker.plugin.api;

/**
 * A row the host wants a plugin's section to hold — <b>the row as wanted, never the transition</b>.
 *
 * <p>Adding a parameter, deleting one, renaming one, retyping it, changing its declared choices, its range,
 * its category, its note or who it is offered to are nine things a window does and <b>one</b> thing the
 * contract says: <i>here is the row I want under this name</i>. The owner compares it with the row it holds
 * and reconciles the difference by its own rules — what a retype resets, whether options survive a change of
 * shape, how a value is clamped into a new range — and answers
 * {@link StudioPlugin#parameterDeclared(ParameterDeclaration)} with the row as it actually stored it.
 *
 * <p><b>That is why this is not nine methods, and not a {@code kind} enum either.</b> A verb would make the
 * contract learn what retyping means, which is the plugin's rule and differs between plugins; an enum would
 * freeze today's list of verbs into a surface only a major release may extend. Stating the desired end value
 * is the general shape to reach for whenever a surface looks like it needs a verb.
 *
 * <h2>A record the host constructs, and it may grow</h2>
 *
 * <p>Same call as {@link ParameterEdit} and {@code Sources.Use}: the host builds it and a plugin only reads
 * it, so the canonical constructor's descriptor is a signature no compiled plugin calls and a component may
 * be added later. The ban in {@code docs/refactor/25-compatibility.md} trap #2 is on records a
 * <em>plugin</em> constructs — which is why {@link ParameterRow}, on the other side of this same call, is a
 * class with a builder.
 *
 * @param groupId the {@link ParameterGroup#id()} the row is filed under. A plugin may own several sections,
 *                and a name is unique only within one of them.
 * @param name    the name of the row as it stands <em>today</em> — the handle. Blank when there is no such
 *                row yet, which is how a row is added; {@code wanted.name()} is what it should be called
 *                afterwards, so a rename is a declaration whose two names differ.
 * @param wanted  the row as the host wants it, or {@code null} to remove the row {@code name} names.
 */
public record ParameterDeclaration(String groupId, String name, ParameterRow wanted) {

    public ParameterDeclaration {
        groupId = groupId == null ? ParameterGroup.DEFAULT_ID : groupId.trim();
        name = name == null ? "" : name.trim();
        if (name.isEmpty() && wanted == null) {
            throw new IllegalArgumentException("a declaration that names no row and wants none says nothing");
        }
    }

    /** A row that does not exist yet. */
    public static ParameterDeclaration added(String groupId, ParameterRow row) {
        if (row == null) throw new IllegalArgumentException("a declaration adds a row or names one");
        return new ParameterDeclaration(groupId, "", row);
    }

    /** The row {@code name} names, gone. */
    public static ParameterDeclaration removed(String groupId, String name) {
        return new ParameterDeclaration(groupId, name, null);
    }

    /** The row {@code row.name()} names, as {@code row} describes it — the ordinary declaration. */
    public static ParameterDeclaration of(String groupId, ParameterRow row) {
        if (row == null) throw new IllegalArgumentException("a declaration adds a row or names one");
        return new ParameterDeclaration(groupId, row.name(), row);
    }

    /** Whether this asks for the row to go. */
    public boolean isRemoval() {
        return wanted == null;
    }

    /** Whether this asks for a row that does not exist yet. */
    public boolean isNew() {
        return name.isEmpty();
    }
}

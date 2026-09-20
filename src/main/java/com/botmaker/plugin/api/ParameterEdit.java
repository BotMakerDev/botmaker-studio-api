package com.botmaker.plugin.api;

/**
 * A value the user changed in the Parameters window, on its way back to the plugin that owns it.
 *
 * <p>The window is the host's and the data is not. So the host renders a row, watches a control, and hands
 * the new value to whoever supplied the row — it does not write anybody's project file, and it does not
 * know what the text means. What comes back is {@link StudioPlugin#parameterEdited(ParameterEdit)}'s answer:
 * the row as the plugin actually stored it, which is how a clamp, a normalisation or a refusal reaches the
 * screen without the host having to model any of them.
 *
 * <h2>A record, and it may grow — which {@link ParameterRow} may not</h2>
 *
 * <p>The <b>host</b> constructs this and a plugin only reads it, so the canonical constructor's descriptor is
 * a signature no compiled plugin calls. Adding a component here therefore breaks nobody, and the accessors an
 * older plugin already calls keep answering. That is the whole of why the pair is shaped this way round: the
 * ban in compatibility trap #2 is on records a <em>plugin</em> constructs.
 *
 * <p>Which is also why this carries only the three things an edit is, rather than a {@code kind} enum over
 * every edit the window might one day make. Adding a parameter, deleting one, renaming one and retyping one
 * are separate questions — they need the declaration, not just the value, and retyping needs the coercion
 * rules that live in the editor. They arrive as their own methods when the window that performs them does,
 * and nothing here has to be guessed now to make that possible.
 *
 * @param groupId the {@link ParameterGroup#id()} the row was rendered under. The plugin may own several
 *                sections, and a name is unique only within one of them.
 * @param name    the {@link ParameterRow#name()} that changed. Never blank.
 * @param value   the new value, as the Java initialiser a field of the row's
 *                {@link ParameterRow#form() form} takes — never a parsed object, and never a syntax tree.
 *                It crosses as source for the reason {@link ParameterRow#value()} does: a composite has no
 *                wire encoding, and inventing one for this direction alone would put back exactly what
 *                {@code 33-plugin-java.md} removes.
 */
public record ParameterEdit(String groupId, String name, String value) {

    public ParameterEdit {
        groupId = groupId == null ? ParameterGroup.DEFAULT_ID : groupId.trim();
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("a parameter edit names no row");
        value = value == null ? "" : value;
    }

    /** An edit named against a group's default id. */
    public static ParameterEdit of(String groupId, String name, String value) {
        return new ParameterEdit(groupId, name, value);
    }

    /** Whether this edit is against {@code row} — same name, and a value that differs from what it holds. */
    public boolean changes(ParameterRow row) {
        return row != null && row.name().equals(name) && !row.value().equals(value);
    }
}

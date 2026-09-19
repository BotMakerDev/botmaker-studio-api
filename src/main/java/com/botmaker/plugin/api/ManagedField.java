package com.botmaker.plugin.api;

/**
 * A kind of field a plugin keeps up to date through its own window, so the host draws it but does not let
 * its code canvas edit it.
 *
 * <p><b>What matches</b>: a {@code static final} field whose declared type resolves to {@link #typeName()}.
 * Constants only, on purpose. A constant is a <i>name</i> the rest of the bot refers to, and the plugin's own
 * window is what keeps that name and every use of it in step. Editing the constant on the canvas changes one
 * of those places and not the others. A local variable or a mutable field of the same type is ordinary code,
 * and stays editable.
 *
 * <p><b>A class holding nothing else</b> — every field matches, and there is at least one — is refused as a
 * whole, so no member can be added to it, moved in it or deleted from it on the canvas either. This is the
 * shape of a class that exists only to name a plugin's values. A class that mixes such constants with
 * ordinary code keeps the ordinary code editable.
 *
 * <p>The host still draws every matching field, and gives its value the plugin's own
 * {@link SlotEditor#preview} rather than a control that edits it. So a managed field is shown, not hidden.
 *
 * <p>Plain data, like every other contribution: the host reads the type name as text and does the matching
 * itself, so no plugin code runs while a file is drawn.
 *
 * @param typeName the field type, fully qualified — {@code com.botmaker.sdk.api.vision.ImageTemplate}
 * @param reason   the sentence the host shows when it refuses an edit: what owns this field and where to go
 *                 instead
 */
public record ManagedField(String typeName, String reason) {
}

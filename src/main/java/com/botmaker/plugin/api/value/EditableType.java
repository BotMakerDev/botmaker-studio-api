package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

/**
 * A declared type its owner draws. The only place {@code editor} lives: a plain
 * {@link PluginType} says, in its class, that its owner does not draw it — another plugin's
 * {@code SlotEditor.forType} does, or the host's fallback — and {@code botmaker plugin validate} reads that
 * without starting JavaFX.
 *
 * @param <T> the declared class
 */
public interface EditableType<T> extends PluginType<T> {

    /**
     * The control a person edits one of these with. <b>Never {@code null}</b>: a type with nothing to draw is a
     * plain {@link PluginType}. The host still defends — a {@code null} or a throw costs that widget, and the
     * next claimant or the host's fallback draws the value.
     *
     * <p>Called on the JavaFX application thread, and only after the host decided this type is the one being
     * edited. Read the current value with {@link ValueContext#value(Class)} — empty when the expression in the
     * file is one the grammar cannot read, which must render read-only rather than overwrite — and write one back
     * with {@link ValueContext#set(Object)}.
     */
    Node editor(ValueContext ctx);
}

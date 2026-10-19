package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * A type declared through {@link PluginType#value}, whose Java the host writes without a call of the plugin's:
 * a literal, an enum constant, or one of the parts declared beside it. Immutable.
 *
 * @param <T> the declared class
 */
public final class DeclaredType<T> implements EditableType<T> {

    private final Class<T> type;
    private final Supplier<? extends T> fresh;
    private final Drawn editor;
    private final Drawn preview;

    DeclaredType(Class<T> type, Supplier<? extends T> fresh, Drawn editor, Drawn preview) {
        this.type = type;
        this.fresh = fresh;
        this.editor = editor;
        this.preview = preview;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public T fresh() {
        return fresh.get();
    }

    @Override
    public Node editor(ValueContext ctx) {
        return editor.draw(ctx);
    }

    @Override
    public Node preview(ValueContext ctx) {
        return preview == null ? null : preview.draw(ctx);
    }

    @Override
    public String toString() {
        return "PluginType[" + type.getName() + "]";
    }
}

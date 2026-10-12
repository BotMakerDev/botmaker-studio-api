package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * A type declared through {@link PluginType#value}, whose Java the host writes without a call of the plugin's:
 * a literal, an enum constant, a call to a result method, or one of the parts declared beside it. Immutable.
 *
 * @param <T> the declared class
 */
public final class DeclaredType<T> implements EditableType<T> {

    private final Class<T> type;
    private final Supplier<? extends T> fresh;
    private final Method freshCall;
    private final Drawn editor;
    private final Drawn preview;
    private final Class<?> replacement;

    DeclaredType(Class<T> type, Supplier<? extends T> fresh, Method freshCall, Drawn editor, Drawn preview) {
        this(type, fresh, freshCall, editor, preview, null);
    }

    private DeclaredType(Class<T> type, Supplier<? extends T> fresh, Method freshCall, Drawn editor, Drawn preview,
                         Class<?> replacement) {
        this.type = type;
        this.fresh = fresh;
        this.freshCall = freshCall;
        this.editor = editor;
        this.preview = preview;
        this.replacement = replacement;
    }

    /**
     * This type, deprecated in favour of {@code replacement}: its class must carry {@link Deprecated}, and the
     * replacement's must not. See
     * {@link PluginType#replacedBy()}.
     */
    public DeclaredType<T> replacedBy(Class<?> replacement) {
        return new DeclaredType<>(type, fresh, freshCall, editor, preview, checkedReplacement(type, replacement));
    }

    static Class<?> checkedReplacement(Class<?> type, Class<?> replacement) {
        if (replacement == null) throw new IllegalArgumentException(type.getName() + ": no replacement given");
        if (replacement == type) throw new IllegalArgumentException(type.getName() + " cannot replace itself");
        if (!type.isAnnotationPresent(Deprecated.class)) {
            throw new IllegalArgumentException(type.getName() + " is replaced but not marked @Deprecated");
        }
        if (replacement.isAnnotationPresent(Deprecated.class)) {
            throw new IllegalArgumentException(type.getName() + " is replaced by " + replacement.getName()
                    + ", which is deprecated too; point at the type that stays");
        }
        return replacement;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public T fresh() {
        return fresh == null ? null : fresh.get();
    }

    @Override
    public Method freshCall() {
        return freshCall;
    }

    @Override
    public Class<?> replacedBy() {
        return replacement;
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

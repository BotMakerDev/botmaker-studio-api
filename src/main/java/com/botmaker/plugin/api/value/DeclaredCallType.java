package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

/**
 * A type declared through {@link PluginType#value} whose Java is a call: one object that is both the
 * {@link EditableType} and the {@link ComponentType}, which is how the host tells that the type it lists is
 * also the call it reads. It takes the steps a {@link DeclaredCall} takes, {@code when} included. Immutable; each
 * step answers a copy.
 *
 * @param <T> the declared class
 */
public final class DeclaredCallType<T> extends CallDeclaration<T, DeclaredCallType<T>> implements EditableType<T> {

    private final DeclaredType<T> declared;

    DeclaredCallType(DeclaredType<T> declared, CallShape<T> shape) {
        super(shape);
        this.declared = declared;
    }

    @Override
    DeclaredCallType<T> next(CallShape<T> shape) {
        return new DeclaredCallType<>(declared, shape);
    }

    @Override
    public Class<T> type() {
        return declared.type();
    }

    @Override
    public T fresh() {
        return declared.fresh();
    }

    @Override
    public Node editor(ValueContext ctx) {
        return declared.editor(ctx);
    }

    @Override
    public Node preview(ValueContext ctx) {
        return declared.preview(ctx);
    }

    @Override
    public String toString() {
        return "PluginType[" + type().getName() + " = " + shape.factory() + "]";
    }
}

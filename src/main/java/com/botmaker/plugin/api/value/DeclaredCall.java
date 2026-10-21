package com.botmaker.plugin.api.value;

/**
 * A call declared through {@link ComponentType#part}: a part of a value that is never picked on its own, or a
 * chain the host reads. Immutable; each step answers a copy, so a declaration stays a {@code static final} field.
 *
 * @param <T> the class the call builds
 */
public final class DeclaredCall<T> extends CallDeclaration<T, DeclaredCall<T>> {

    DeclaredCall(CallShape<T> shape) {
        super(shape);
    }

    @Override
    DeclaredCall<T> next(CallShape<T> shape) {
        return new DeclaredCall<>(shape);
    }

    @Override
    public String toString() {
        return "ComponentType[" + shape.type().getName() + " = " + shape.factory() + "]";
    }
}

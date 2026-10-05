package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

/**
 * A call declared through {@link ComponentType#part}: a part of a value that is never picked on its own, or a
 * chain the host reads. Immutable; each {@code with…} answers a copy, so a declaration stays a
 * {@code static final} field.
 *
 * @param <T> the class the call builds
 */
public final class DeclaredCall<T> implements ComponentType<T> {

    private final CallShape<T> shape;

    DeclaredCall(CallShape<T> shape) {
        this.shape = shape;
    }

    /**
     * This call, plus the {@code public static final} constants of its type that a value equal to one is
     * written as: {@code .constants(FlowLayout.NONE)}. Named by value and typed, so a renamed constant or one of
     * another type is a compile error.
     */
    @SafeVarargs
    public final DeclaredCall<T> constants(T... values) {
        return new DeclaredCall<>(shape.withConstants(values));
    }

    /**
     * This call, taken apart by hand rather than by its accessors. <b>Only for a part the host cannot read
     * as a value</b> — the SDK's activity body, which crosses as the source it is written as.
     */
    public DeclaredCall<T> components(Function<T, List<Object>> components) {
        return new DeclaredCall<>(shape.withComponents(components));
    }

    /**
     * This call, built back by hand rather than by invoking its factory — for parts the factory would refuse
     * but the value can still be made from, such as a map with a stray entry. Answer {@code null} for parts
     * this call does not build.
     */
    public DeclaredCall<T> build(Function<List<Object>, T> build) {
        return new DeclaredCall<>(shape.withBuild(build));
    }

    @Override
    public Class<T> type() {
        return shape.type();
    }

    @Override
    public Executable factory() {
        return shape.factory();
    }

    @Override
    public List<Class<?>> componentTypes() {
        return shape.parts();
    }

    @Override
    public List<Object> components(T value) {
        return shape.components().apply(value);
    }

    @Override
    public T build(List<Object> parts) {
        return shape.build().apply(parts);
    }

    @Override
    public List<Field> constants() {
        return shape.constants();
    }

    @Override
    public String toString() {
        return "ComponentType[" + shape.type().getName() + " = " + shape.factory() + "]";
    }
}

package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * This call, then {@code .wither(part)} wherever a value's part differs from what the factory makes:
     * {@code .with(Flow.Step::described, Flow.Step::description)} writes {@code .described("Picks up ore")}.
     * See {@link Wither}. Each wither adds one part after the factory's, in the order they are declared.
     *
     * @param wither an instance method of the type taking the part and answering the changed value
     * @param part   the part a value holds, as the wither takes it
     * @throws IllegalArgumentException for a method that is no wither of the type, a varargs factory, or a
     *                                  wither declared twice
     */
    public <A> DeclaredCall<T> with(Ref.Of2<T, A, T> wither, Function<? super T, ? extends A> part) {
        return new DeclaredCall<>(shape.withWither(Wither.with(shape.type(), wither, part)));
    }

    /**
     * This call, then {@code .wither()} wherever a value has {@code on} and the factory's value has not:
     * {@code .flag(Flow.Step::goesHome, Flow.Step::goHome)} writes {@code .goesHome()}. A flag only turns a
     * thing on, so a setting that is on by default is declared by its negative ({@code .off()}). See
     * {@link Wither}.
     *
     * @throws IllegalArgumentException as for {@link #with}
     */
    public DeclaredCall<T> flag(Ref.Of1<T, T> wither, Predicate<? super T> on) {
        return new DeclaredCall<>(shape.withWither(Wither.flag(shape.type(), wither, on)));
    }

    /**
     * This call, taken apart by hand rather than by its accessors. <b>Only for a part the host cannot read
     * as a value</b> — the SDK's activity body, which crosses as the source it is written as. The factory's
     * parts only: each wither's part still follows them.
     */
    public DeclaredCall<T> components(Function<T, List<Object>> components) {
        return new DeclaredCall<>(shape.withComponents(components));
    }

    /**
     * This call, built back by hand rather than by invoking its factory — for parts the factory would refuse
     * but the value can still be made from, such as a map with a stray entry. Answer {@code null} for parts
     * this call does not build. Given the factory's parts only; each wither is applied after it.
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
        return shape.allParts();
    }

    @Override
    public List<Object> components(T value) {
        return shape.taken(value);
    }

    @Override
    public T build(List<Object> parts) {
        return shape.built(parts);
    }

    @Override
    public List<Field> constants() {
        return shape.constants();
    }

    @Override
    public List<Wither<T>> withers() {
        return shape.withers();
    }

    @Override
    public String toString() {
        return "ComponentType[" + shape.type().getName() + " = " + shape.factory() + "]";
    }
}

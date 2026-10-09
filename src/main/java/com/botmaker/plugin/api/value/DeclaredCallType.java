package com.botmaker.plugin.api.value;

import com.botmaker.plugin.api.slot.ValueContext;
import javafx.scene.Node;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A type declared through {@link PluginType#value} whose Java is a call: one object that is both the
 * {@link EditableType} and the {@link ComponentType}, which is how the host tells that the type it lists is
 * also the call it reads. Immutable; each {@code with…} answers a copy.
 *
 * @param <T> the declared class
 */
public final class DeclaredCallType<T> implements EditableType<T>, ComponentType<T> {

    private final DeclaredType<T> declared;
    private final DeclaredCall<T> call;

    DeclaredCallType(DeclaredType<T> declared, DeclaredCall<T> call) {
        this.declared = declared;
        this.call = call;
    }

    /** This type, plus the constants a value equal to one is written as. See {@link DeclaredCall#constants}. */
    @SafeVarargs
    public final DeclaredCallType<T> constants(T... values) {
        return new DeclaredCallType<>(declared, call.constants(values));
    }

    /** This type, then a wither setting one part. See {@link DeclaredCall#with}. */
    public <A> DeclaredCallType<T> with(Ref.Of2<T, A, T> wither, Function<? super T, ? extends A> part) {
        return new DeclaredCallType<>(declared, call.with(wither, part));
    }

    /** This type, then a flag turning one part on. See {@link DeclaredCall#flag}. */
    public DeclaredCallType<T> flag(Ref.Of1<T, T> wither, Predicate<? super T> on) {
        return new DeclaredCallType<>(declared, call.flag(wither, on));
    }

    /** This type, built back by hand. See {@link DeclaredCall#build(Function)}. */
    public DeclaredCallType<T> build(Function<List<Object>, T> build) {
        return new DeclaredCallType<>(declared, call.build(build));
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
    public Method freshCall() {
        return declared.freshCall();
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
    public Executable factory() {
        return call.factory();
    }

    @Override
    public List<Class<?>> componentTypes() {
        return call.componentTypes();
    }

    @Override
    public List<Object> components(T value) {
        return call.components(value);
    }

    @Override
    public T build(List<Object> parts) {
        return call.build(parts);
    }

    @Override
    public List<Field> constants() {
        return call.constants();
    }

    @Override
    public List<Wither<T>> withers() {
        return call.withers();
    }

    @Override
    public String toString() {
        return "PluginType[" + type().getName() + " = " + call.factory() + "]";
    }
}

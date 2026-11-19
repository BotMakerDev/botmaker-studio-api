// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.value.Ref;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * One value a window of your plugin edits, kept as a method in the bot: {@code @SdkValue(SdkValue.Id.SETTINGS)
 * public static BotSettings settings() { return …; }}. The host rewrites only the expression the method returns, and never adds or
 * removes a method but to write it into a project that lacks it ({@link PluginValues#create}). Declared with
 * {@link ManagedValue#method}.
 *
 * <p>The bot's runtime hands the method's value to whoever claimed it
 * ({@link com.botmaker.plugin.api.managed.ManagedValues#claim}); the plugin's window reads and writes it through
 * {@link PluginValues}.
 *
 * @param <T> the class the method returns
 */
public final class ManagedMethod<T> extends ManagedValue<T> {

    private final Type form;
    private final T initial;

    ManagedMethod(Key key, String holder, Class<T> type, Type form, T initial, String reason) {
        super(key, holder, type, reason);
        this.form = form;
        this.initial = initial;
    }

    /**
     * The method's whole return type, which {@link #type()} cannot say: {@code List<Launcher>} where {@code type()} is
     * {@code List}. The host writes a created method with it, and repairs a method whose return type lacks its
     * arguments. {@link #type()} itself for a value declared by {@link TypeStep#holds}.
     */
    public Type form() {
        return form;
    }

    /**
     * What a created method first returns, or null for the type's own fresh value — which a type declared only by
     * its parts ({@code ComponentType}) has not got. A method with no {@link #holder()} is never created.
     */
    public T initial() {
        return initial;
    }

    /**
     * After {@link ManagedValue#method}: where the value lives.
     *
     * @param <H> the holder enum the id enum names
     */
    public static final class Steps<H extends Enum<H>> {

        private final Key key;

        Steps(Key key) {
            this.key = key;
        }

        /**
         * In the class {@code holder} stands for — {@code SdkValue.Holder.SDK}, the class {@code Sdk} — which the host
         * writes when a project has none.
         *
         * @throws IllegalArgumentException when a constant of {@code holder}'s enum is not UPPER_SNAKE, or two name
         *                                  classes that differ only by case
         */
        public TypeStep in(H holder) {
            return new TypeStep(key, holderName(key, holder));
        }

        /**
         * Somewhere the host may open and never create: {@code .openedOnly().holds(T.class)}.
         *
         * @throws IllegalArgumentException for a value of this PC ({@link com.botmaker.plugin.api.managed.HostHolder}):
         *                                  each lives in its holder's one class, which the host creates
         */
        public OpenedStep openedOnly() {
            if (key.host()) {
                throw new IllegalArgumentException(key.id() + ": a value of this PC lives in its holder's class; "
                        + "name it with in(…)");
            }
            return new OpenedStep(key);
        }
    }

    /** After {@link Steps#in}: what the method returns. */
    public static final class TypeStep {

        private final Key key;
        private final String holder;

        private TypeStep(Key key, String holder) {
            this.key = key;
            this.holder = holder;
        }

        /**
         * A method returning a {@code type}, first returning {@code initial} when the host creates it — or,
         * with {@code null}, the type's own fresh value. A generic type is written without its arguments: declare
         * one by {@link #startsAs} instead.
         */
        public <T> Reason<ManagedMethod<T>> holds(Class<T> type, T initial) {
            requireType(key, type, "type");
            return new Reason<>(reason -> new ManagedMethod<>(key, holder, type, type, initial, reason));
        }

        /**
         * A method returning the type {@code initial}'s method returns — generic arguments included, which a class
         * literal cannot say: {@code List<Launcher>} — first returning what it returns. The method is called once,
         * here.
         *
         * @param initial a method reference to a {@code static} method of no argument — {@code SdkValues::noTargets},
         *                returning {@code List<Launcher>}
         * @throws IllegalArgumentException when {@code initial} is no reference to a static method, or it returns
         *                                  nothing: a lambda's own method says no generic type
         */
        public <T> Reason<ManagedMethod<T>> startsAs(Ref.Of0<T> initial) {
            if (!(Ref.resolve(initial) instanceof Method method) || method.getReturnType() == void.class
                    || method.isSynthetic() || !Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException(key.id() + ": startsAs names a static method returning the value");
            }
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) MethodType.methodType(method.getReturnType()).wrap().returnType();
            T first = initial.call();
            Type form = method.getGenericReturnType();
            return new Reason<>(reason -> new ManagedMethod<>(key, holder, type, form, first, reason));
        }
    }

    /** After {@link Steps#openedOnly}: what the method returns. */
    public static final class OpenedStep {

        private final Key key;

        private OpenedStep(Key key) {
            this.key = key;
        }

        /** A method returning a {@code type}, which the host opens and never writes. */
        public <T> Reason<ManagedMethod<T>> holds(Class<T> type) {
            requireType(key, type, "type");
            return new Reason<>(reason -> new ManagedMethod<>(key, null, type, type, null, reason));
        }
    }
}

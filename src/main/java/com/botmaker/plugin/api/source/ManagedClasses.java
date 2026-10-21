package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.value.Ref;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An open set of classes nested in the holder, each a subclass of the element class with one constant holding its
 * one instance — {@code public static final class Market extends Screen { … } public static final Market MARKET =
 * new Market();}. A member is the constant; its parts are its {@code public final} instance fields, which other values
 * point at one by one ({@code Screens.MARKET.beerBuy}), and one method it overrides says the rest of it. Declared
 * with {@link ManagedValue#openSet}{@code .ofClasses(…)}.
 *
 * @param <T> the class every nested one extends
 */
public final class ManagedClasses<T> extends ManagedOpenSet<T> {

    private final Class<?> fieldType;
    private final Ref.Of2<String, String, ?> byField;
    private final Method setup;

    private ManagedClasses(Key key, String holder, Class<T> type, Class<?> fieldType,
                           Ref.Of2<String, String, ?> byField, Method setup, Ref.Of1<String, ? extends T> referred,
                           String reason) {
        super(key, holder, type, referred, reason);
        this.fieldType = fieldType;
        this.byField = byField;
        this.setup = setup;
    }

    /**
     * The class of a nested class's managed fields ({@link FieldsStep#fields}): each instance field whose declared
     * type is it or a subtype.
     */
    public Class<?> fieldType() {
        return fieldType;
    }

    /**
     * The value a reference to the field {@code field} of the class whose constant is {@code member} stands for —
     * {@code Screens.MARKET.beerBuy} read by a host that cannot load the bot's class is what this makes of
     * {@code ("MARKET", "beerBuy")}. Null when the plugin refused, or it answered something other than
     * {@link #fieldType()}.
     */
    public Object byField(String member, String field) {
        if (member == null || field == null) return null;
        try {
            Object made = byField.call(member, field);
            return fieldType.isInstance(made) ? made : null;
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /**
     * The method of {@link #type()} each nested class overrides, whose one {@code return} is the rest of the class
     * ({@link SetupStep#setup}): what the host reads, writes, and declares in a class it adds.
     */
    public Method setup() {
        return setup;
    }

    /** After {@link ManagedOpenSet.Steps#ofClasses}: what a nested class's fields are. */
    public static final class FieldsStep<E> {

        private final Key key;
        private final Class<E> element;

        FieldsStep(Key key, Class<E> element) {
            this.key = key;
            this.element = element;
        }

        /**
         * The class's managed fields: its {@code public final} instance fields of {@code field} or a subtype, in the
         * order they are written. A reference to one — {@code Screens.MARKET.beerBuy}, or {@code MARKET.close} inside
         * the holder — reads as what {@code byReference} makes of the constant's and the field's names, wherever it
         * is read, and a value equal to that is written back as the reference. Its initialiser is read only when the
         * field itself is opened ({@link PluginValues#open(ManagedValue, PluginValues.Member)}).
         *
         * @param field       the plugin's class for one field — {@code Input}
         * @param byReference a method reference from a constant's name and a field's name to the value standing for
         *                    that field — {@code BotConstants::input}; it must equal itself by those names alone
         * @throws IllegalArgumentException when {@code field} is missing or {@code byReference} is no method reference
         */
        public <F> SetupStep<E> fields(Class<F> field, Ref.Of2<String, String, ? extends F> byReference) {
            if (field == null) throw new IllegalArgumentException(key.id() + ": no field type given");
            Ref.resolve(byReference);
            return new SetupStep<>(key, element, field, byReference);
        }
    }

    /** After {@link FieldsStep#fields}: the method each nested class overrides. */
    public static final class SetupStep<E> {

        private final Key key;
        private final Class<E> element;
        private final Class<?> field;
        private final Ref.Of2<String, String, ?> byField;

        private SetupStep(Key key, Class<E> element, Class<?> field, Ref.Of2<String, String, ?> byField) {
            this.key = key;
            this.element = element;
            this.field = field;
            this.byField = byField;
        }

        /**
         * The method of the element type each nested class overrides with one {@code return <expression>;} — the
         * rest of the class, say its name and how it is reached — which the host reads and writes as a value of the
         * method's return type ({@link PluginValues#open(ManagedValue, PluginValues.Member)} on
         * {@code Member.of("MARKET")}). A method rather than a constructor argument: the expression may name the
         * class's own fields, which a {@code super(…)} call cannot, so the plugin's runtime calls it once the
         * instance is built.
         *
         * @param method a method reference to a public, non-static, non-final method of the element type that
         *               takes nothing and returns the value — {@code Screen::setup}
         * @throws IllegalArgumentException when {@code method} is no such reference
         */
        public ReferableStep<E, ManagedClasses<E>> setup(Ref.Of1<? super E, ?> method) {
            if (!(Ref.resolve(method) instanceof Method setup) || setup.getParameterCount() != 0
                    || setup.getReturnType() == void.class
                    || Modifier.isStatic(setup.getModifiers())
                    || Modifier.isFinal(setup.getModifiers())
                    || !Modifier.isPublic(setup.getModifiers())
                    || !setup.getDeclaringClass().isAssignableFrom(element)) {
                throw new IllegalArgumentException(key.id() + ": setup names a public method of "
                        + element.getName() + " taking nothing, returning a value, that a subclass can override");
            }
            return new ReferableStep<>(key, (holder, referred) -> reason ->
                    new ManagedClasses<>(key, holder, element, field, byField, setup, referred, reason));
        }
    }
}

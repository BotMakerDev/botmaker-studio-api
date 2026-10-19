package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.params.Param;
import com.botmaker.plugin.api.value.Ref;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * A parameter of your plugin's, kept as a {@code @Param} field in the bot that the person running it sets:
 * {@code @SdkValue(SdkValue.Id.DO_SETUP) @Param(…) public static boolean doSetup = true;}. The host writes the field
 * when the holder lacks it and keeps its declaration (name, type, annotations) the plugin's; its value is edited like
 * any parameter's, in the Parameters window and the Runner. The bot reads the field itself, so nothing is claimed at
 * run time. Declared with {@link ManagedValue#param}.
 *
 * @param <T> the field's class, boxed when it is a primitive
 */
public final class ManagedParam<T> extends ManagedValue<T> {

    private final Type form;
    private final T initial;
    private final String category;
    private final String description;
    private final String visibility;
    private final Predicate<? super ParamScope> applies;

    private ManagedParam(Key key, String holder, Class<T> type, Type form, T initial, String category,
                         String description, String visibility, Predicate<? super ParamScope> applies,
                         String reason) {
        super(key, holder, type, reason);
        this.form = form;
        this.initial = initial;
        this.category = category;
        this.description = description;
        this.visibility = visibility;
        this.applies = applies;
    }

    /**
     * The field's whole type, which {@link #type()} cannot say: {@code Set<Class<? extends Outcome>>},
     * {@code boolean}.
     */
    @Override
    public Type form() {
        return form;
    }

    /** What the field is first set to; null is written as {@code null}. */
    @Override
    public T initial() {
        return initial;
    }

    /** Its {@code @Param(category)}: the heading the Parameters window lists it under. */
    @Override
    public String category() {
        return category;
    }

    /** Its {@code @Param(description)}: the line beside it. */
    @Override
    public String description() {
        return description;
    }

    /** Its {@code @Param(visibility)}: {@code Param.EDITOR} or {@code Param.PUBLIC}. */
    @Override
    public String visibility() {
        return visibility;
    }

    /** Whether it was declared with a condition ({@link ConditionStep#when}). */
    @Override
    public boolean conditional() {
        return applies != null;
    }

    /**
     * Whether it applies to the bot {@code scope} reads: true without a condition, and for one whose condition
     * throws. What the host asks before writing the field or listing it.
     */
    @Override
    public boolean appliesIn(ParamScope scope) {
        if (applies == null) return true;
        try {
            return applies.test(scope == null ? ParamScope.NONE : scope);
        } catch (RuntimeException | LinkageError unreadable) {
            return true;
        }
    }

    /** After {@link ManagedValue#param}: the class the field is in. */
    public static final class Steps {

        private final Key key;

        Steps(Key key) {
            this.key = key;
        }

        /** In the class {@code holder} — {@code "Sdk"} — which the host writes the field into, or creates. */
        public TypeStep in(String holder) {
            return new TypeStep(key, requireText(holder, "holder"));
        }
    }

    /** After {@link Steps#in}: the field's type and first value. */
    public static final class TypeStep {

        private final Key key;
        private final String holder;

        private TypeStep(Key key, String holder) {
            this.key = key;
            this.holder = holder;
        }

        /**
         * A field of the type {@code initial}'s method returns — generic arguments included, which a class
         * literal cannot say — first set to what it returns. The method is called once, here.
         *
         * @param initial a method reference to a {@code static} method of no argument —
         *                {@code SdkParams::skip}, returning {@code Set<Class<? extends Outcome>>}
         * @throws IllegalArgumentException when {@code initial} is no method reference, or it returns nothing
         */
        public <T> ShownStep<T> startsAs(Ref.Of0<T> initial) {
            if (!(Ref.resolve(initial) instanceof Method method) || method.getReturnType() == void.class) {
                throw new IllegalArgumentException(key.id() + ": startsAs names a method returning the value");
            }
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) MethodType.methodType(method.getReturnType()).wrap().returnType();
            return new ShownStep<>(key, holder, type, method.getGenericReturnType(), initial.call());
        }
    }

    /** After {@link TypeStep#startsAs}: what the field's {@code @Param} says. */
    public static final class ShownStep<T> {

        private final Key key;
        private final String holder;
        private final Class<T> type;
        private final Type form;
        private final T initial;

        private ShownStep(Key key, String holder, Class<T> type, Type form, T initial) {
            this.key = key;
            this.holder = holder;
            this.type = type;
            this.form = form;
            this.initial = initial;
        }

        /**
         * The heading it is listed under, the line beside it, and who sees it — {@link Param.Visibility#PUBLIC} for
         * the Runner too, {@link Param.Visibility#EDITOR} for the Parameters window only; {@code null} reads as the
         * latter.
         */
        public ConditionStep<T> shown(String category, String description, Param.Visibility visibility) {
            return new ConditionStep<>(new ManagedParam<>(key, holder, type, form, initial,
                    category == null ? "" : category, description == null ? "" : description,
                    (visibility == null ? Param.Visibility.EDITOR : visibility).id(), null, null));
        }
    }

    /** After {@link ShownStep#shown}: a condition, if any, then the sentence shown on a refused edit. */
    public static final class ConditionStep<T> {

        private final ManagedParam<T> shown;

        private ConditionStep(ManagedParam<T> shown) {
            this.shown = shown;
        }

        /**
         * This parameter, declared only while {@code applies} holds: {@code .when(scope -> scope.value(SETTINGS)
         * .map(s -> s.runIn() == Where.VM).orElse(false))}. The condition reads the bot's other values of this plugin
         * through the {@link ParamScope}, and anything about the computer — its system, what is installed — from the
         * plugin's own code. While it does not hold, the host neither writes the field into a bot that lacks it nor
         * lists it in the Parameters window or the Runner; a field already written stays, as the bot's Java.
         *
         * <p>So <b>the bot's own code must not read a conditional parameter by name</b>: on a bot whose condition
         * never held, the field is not there. The plugin's runtime reads it by its mark, and a missing one is its
         * initial value.
         *
         * <p>A condition that throws counts as holding: a parameter shown that need not be costs less than one hidden
         * that should not be.
         *
         * @throws IllegalArgumentException for no condition
         */
        public ConditionStep<T> when(Predicate<? super ParamScope> applies) {
            if (applies == null) throw new IllegalArgumentException("when needs a condition");
            return new ConditionStep<>(shown.with(applies, null));
        }

        /** What owns this parameter and where to change it instead. */
        public ManagedParam<T> because(String reason) {
            return shown.with(shown.applies, requireText(reason, "reason"));
        }
    }

    private ManagedParam<T> with(Predicate<? super ParamScope> applies, String reason) {
        return new ManagedParam<>(key(), holder(), type(), form, initial, category, description, visibility,
                applies, reason);
    }
}

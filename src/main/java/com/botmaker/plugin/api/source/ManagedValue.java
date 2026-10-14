package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.managed.ManagedMarker;
import com.botmaker.plugin.api.params.Param;
import com.botmaker.plugin.api.value.Ref;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One value this plugin keeps up to date through its own window, named by the id it is annotated with.
 *
 * <p><b>Declare each one once, as a constant, and use the constant everywhere</b>: in
 * {@code StudioPlugin.managedValues()}, in the plugin's runtime half through
 * {@link com.botmaker.plugin.api.managed.ManagedValues#claim}, and in its own window to open the value. The id
 * then exists in exactly one place, and {@link #type()} is what lets the runtime hand the sink a {@code T} and
 * the window read one without a cast.
 *
 * <p><b>What matches</b>: a method or type in the bot's own Java marked with this value's id — the plugin's
 * own {@link ManagedMarker} annotation holding a constant of the enum nested in it,
 * {@code @SdkValue(SdkValue.Id.FLOW)}, which javac checks. A bot has the annotation on its classpath through
 * the plugin that brings it at {@code compile}.
 *
 * <p><b>Two shapes, one marker.</b> On a <b>method</b> it is a fixed value the plugin shipped a declaration
 * for — the host rewrites the expression that method returns and never adds or removes a method. On a
 * <b>type</b> it is an open set the user grows — a class of constants the rest of the bot names at its use
 * sites — and the whole class is the plugin's window's, so the canvas may not add to it, rename in it or
 * delete from it. There {@link #type()} is the class of each constant: see {@link #openSet}. Which of the two
 * a value is, is its {@link #shape()}. An open set is a class of {@code public static final} fields, an enum
 * of bare names implementing {@link #type()} ({@link #isEnum()}), or a class of such enums nested in it
 * ({@link #isGrouped()}).
 *
 * <p><b>A third shape, a parameter</b> ({@link #param}, 2026-10-11): on a <b>field</b> that also carries
 * {@code @Param}, a value the person running the bot picks — {@code Sdk.doSetup}. The host writes the field when
 * the holder lacks it and keeps its declaration (name, type, annotations) the plugin's; its value is edited like
 * any parameter's, in the Parameters window and the Runner. The bot reads the field itself, so nothing is
 * claimed at run time.
 *
 * <p><b>Where it goes when it is missing.</b> A project that never had the plugin's file has no
 * marked method to open, and nothing else would ever write one. {@code holder} and {@code type} are
 * what the host needs to write it once ({@link PluginValues#create}, or on every bind): the class the value
 * lives in, under {@code plugins/<last segment of the plugin's id>/}, and the type the method returns. What the method first
 * returns is {@code initial}, written by the host's grammar as any value is; with none, the type's own fresh
 * value — which a type declared only by its parts ({@code ComponentType}) has not got. A value with no holder
 * can only be opened, never created.
 *
 * <p>Plain data, like every other contribution: the host reads the id as text and does the matching itself,
 * so no plugin code runs while a file is drawn.
 *
 * <p><b>A class, not a record</b>: only {@link #method} and {@link #openSet}'s steps build one.
 * A record's public canonical constructor changes when a component is added, and every plugin compiled
 * against the old one would throw {@code NoSuchMethodError}; through the steps a new component is a new step.
 *
 * @param <T> the value's class
 */
public final class ManagedValue<T> {

    /** Where the marker annotation goes, and so what the host may change. */
    public enum Shape {
        /** On a method: one value, the expression the method returns. */
        METHOD("method", "a managed value"),
        /** On a type: a class of constants the plugin adds to, renames in and removes from. */
        OPEN_SET("open-set", "a managed set"),
        /** On a field that is also a {@code @Param}: its declaration the plugin's, its value the runner's. */
        PARAM("param", "a plugin's parameter");

        private final String id;
        private final String displayName;

        Shape(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** The stable key: {@code "method"}, {@code "open-set"}, {@code "param"}. */
        public String id() {
            return id;
        }

        /** For a sentence: {@code "a managed set"}. */
        public String displayName() {
            return displayName;
        }
    }

    /** The id and the binary name of the plugin's marker annotation. */
    private record Key(String id, String marker) {

        static Key of(Enum<?> constant, ElementType on) {
            if (constant == null) throw new IllegalArgumentException("A managed value needs an id");
            return new Key(idOf(constant), markerOf(constant, on));
        }
    }

    /** A set of nested enums ({@link SetSteps#ofEnums}): how a constant and a whole enum read as the plugin's. */
    private record Grouped(Ref.Of2<String, String, ?> byName, Class<?> groupType, Ref.Of1<String, ?> byGroup) {}

    /**
     * A parameter ({@link #param}): the field's full type, what its {@code @Param} says, and when it applies
     * ({@code null} for always).
     */
    private record Field(Type form, String category, String description, String visibility,
                         Predicate<? super ParamScope> applies) {

        Field when(Predicate<? super ParamScope> condition) {
            return new Field(form, category, description, visibility, condition);
        }
    }

    private final Key key;
    private final String reason;
    private final String holder;
    private final Shape shape;
    private final Class<T> type;
    private final T initial;
    private final Ref.Of1<String, ? extends T> byName;
    private final Grouped grouped;
    private final Field field;
    private final Ref.Of1<String, ? extends T> referred;

    private ManagedValue(Key key, String reason, String holder, Shape shape, Class<T> type, T initial,
                         Ref.Of1<String, ? extends T> byName, Grouped grouped, Field field,
                         Ref.Of1<String, ? extends T> referred) {
        this.referred = referred;
        this.key = key;
        this.reason = reason;
        this.holder = holder;
        this.shape = shape;
        this.type = type;
        this.initial = initial;
        this.byName = byName;
        this.grouped = grouped;
        this.field = field;
    }

    /** The id: {@link #idOf} its constant — {@code "com.botmaker.sdk.api.bot.SdkValue$Id.FLOW"}. */
    public String id() {
        return key.id();
    }

    /**
     * The binary name of the plugin's {@link ManagedMarker} annotation a bot marks this value with —
     * {@code "com.botmaker.sdk.api.bot.SdkValue"}.
     */
    public String marker() {
        return key.marker();
    }

    /**
     * The id of a typed value: the binary name of {@code constant}'s enum, a dot, and the constant's name —
     * {@code "com.botmaker.sdk.api.bot.SdkValue$Id.FLOW"}. The enum's name is the <b>binary</b> one, with
     * {@code $} before a nested class: a host reading a bot's annotation off JDT takes the constant's
     * declaring type's {@code getBinaryName()}, never its {@code getQualifiedName()}, and the runtime reads the
     * same through reflection.
     */
    public static String idOf(Enum<?> constant) {
        return constant.getDeclaringClass().getName() + "." + constant.name();
    }

    /**
     * The binary name of the annotation {@code constant}'s enum is nested in, refused unless that annotation
     * has the shape {@link ManagedMarker} asks for — marked, runtime-retained, placeable {@code on} a method or
     * a type as the value's shape needs, its {@code value()} that enum — checked once, when the plugin
     * declares the value.
     */
    private static String markerOf(Enum<?> constant, ElementType on) {
        Class<?> ids = constant.getDeclaringClass();
        Class<?> marker = ids.getEnclosingClass();
        String where = ids.getName() + "." + constant.name();
        if (marker == null || !marker.isAnnotation() || !marker.isAnnotationPresent(ManagedMarker.class)) {
            throw new IllegalArgumentException(where + ": the enum must be nested in an annotation marked "
                    + "@ManagedMarker");
        }
        Retention retention = marker.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + " needs "
                    + "@Retention(RetentionPolicy.RUNTIME), or a bot's runtime never sees it");
        }
        Target target = marker.getAnnotation(Target.class);
        if (target != null && !Arrays.asList(target.value()).contains(on)) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + "'s @Target must include "
                    + "ElementType." + on + ", where a bot puts it");
        }
        try {
            if (marker.getDeclaredMethod("value").getReturnType() != ids) {
                throw new IllegalArgumentException(where + ": " + marker.getName() + ".value() must be "
                        + ids.getSimpleName());
            }
        } catch (NoSuchMethodException none) {
            throw new IllegalArgumentException(where + ": " + marker.getName() + " has no value()");
        }
        return marker.getName();
    }

    /** The sentence the host shows when it refuses an edit: what owns this and where to go instead. */
    public String reason() {
        return reason;
    }

    /**
     * The simple name of the class that holds it — {@code "Sdk"}, {@code "Pictures"} — or null when the host
     * may not create it.
     */
    public String holder() {
        return holder;
    }

    /** Whether the annotation goes on a method or on the holder. */
    public Shape shape() {
        return shape;
    }

    /**
     * The class the marked method returns, or — for an open set — the class of each constant, or — for a
     * parameter — the field's class, boxed when it is a primitive.
     */
    public Class<T> type() {
        return type;
    }

    /**
     * The type the host writes: a parameter's whole field type — {@code Set<Class<? extends Outcome>>},
     * {@code boolean} — which {@link #type()} cannot say; {@link #type()} for any other shape.
     */
    public Type form() {
        return field == null ? type : field.form();
    }

    /**
     * What a created method first returns, or null for the type's fresh value; for a parameter, what the field
     * is first set to, null written as {@code null}.
     */
    public T initial() {
        return initial;
    }

    /** Whether this is a parameter ({@link #param}) — the marker on a {@code @Param} field. */
    public boolean isParam() {
        return shape == Shape.PARAM;
    }

    /** A parameter's {@code @Param(category)}; {@code ""} for any other shape. */
    public String category() {
        return field == null ? "" : field.category();
    }

    /** A parameter's {@code @Param(description)}; {@code ""} for any other shape. */
    public String description() {
        return field == null ? "" : field.description();
    }

    /** A parameter's {@code @Param(visibility)}, {@code Param.EDITOR} or {@code Param.PUBLIC}; {@code ""} otherwise. */
    public String visibility() {
        return field == null ? "" : field.visibility();
    }

    /** Whether this parameter was declared with a condition ({@link Reason#when}). */
    public boolean conditional() {
        return field != null && field.applies() != null;
    }

    /**
     * Whether this value applies to the bot {@code scope} reads: true for anything but a conditional parameter, and
     * for one whose condition throws. What the host asks before writing a parameter or listing it.
     */
    public boolean appliesIn(ParamScope scope) {
        if (!conditional()) return true;
        try {
            return field.applies().test(scope == null ? ParamScope.NONE : scope);
        } catch (RuntimeException | LinkageError unreadable) {
            return true;
        }
    }

    /**
     * Whether this open set is an enum of the bot's ({@link SetSteps#ofEnum}), each member a bare constant
     * whose name is the whole of it, rather than a class of {@code public static final} fields.
     */
    public boolean isEnum() {
        return byName != null;
    }

    /**
     * The value the enum constant {@code name} of this set stands for, made by the plugin from the name alone —
     * what a host that cannot load the bot's enum reads {@code Outcomes.WON} as. Null when this is no enum set,
     * the plugin's function refused the name, or it answered something other than {@link #type()}.
     */
    public T byName(String name) {
        if (byName == null || name == null) return null;
        try {
            return cast(byName.call(name));
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /**
     * Whether this open set is a class of enums nested in it ({@link SetSteps#ofEnums}) — one enum per group,
     * each a set of bare constants — rather than one class's constants.
     */
    public boolean isGrouped() {
        return grouped != null;
    }

    /**
     * The value the constant {@code name} of the nested enum {@code group} stands for —
     * {@code Activities.Collect.WON} read by a host that cannot load the bot's enum is what this makes of
     * {@code ("Collect", "WON")}. Null when this set is not {@link #isGrouped() grouped}, the plugin refused, or
     * it answered something other than {@link #type()}.
     */
    public T byName(String group, String name) {
        if (grouped == null || group == null || name == null) return null;
        try {
            return cast(grouped.byName().call(group, name));
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /**
     * Whether a reference to one of this open set's constants reads as the constant's name
     * ({@link SetHolderStep#referredToAs}) rather than as what its initialiser holds.
     */
    public boolean isReferredByName() {
        return referred != null;
    }

    /**
     * The value a reference to the constant {@code name} of this set stands for — what {@code Flows.DAILY} inside
     * another value reads as — made by the plugin from the name alone. Null when this set is not
     * {@link #isReferredByName() referred to by name}, the plugin refused, or it answered something other than
     * {@link #type()}.
     */
    public T byReference(String name) {
        if (referred == null || name == null) return null;
        try {
            return cast(referred.call(name));
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /**
     * The plugin's class for a whole nested enum — what a class literal of one, {@code Activities.Collect.class},
     * reads as wherever a value is read. Null when this set is not {@link #isGrouped() grouped}.
     */
    public Class<?> groupType() {
        return grouped == null ? null : grouped.groupType();
    }

    /**
     * The value the nested enum {@code group} stands for — what {@code Activities.Collect.class} reads as — made
     * by the plugin from the enum's simple name, as {@code as}: {@link #groupType()} or a supertype of it. Empty
     * when this set is not grouped, the plugin refused, or the value is not an {@code as}.
     */
    public <G> Optional<G> byGroup(Class<G> as, String group) {
        if (grouped == null || group == null || as == null) return Optional.empty();
        try {
            Object made = grouped.byGroup().call(group);
            return grouped.groupType().isInstance(made) && as.isInstance(made)
                    ? Optional.of(as.cast(made)) : Optional.empty();
        } catch (RuntimeException refused) {
            return Optional.empty();
        }
    }

    /**
     * <b>The way to declare a method-shaped value</b>, step by step. {@code id} is a constant of the enum nested
     * in the plugin's {@link ManagedMarker} annotation, which a bot marks the method with —
     * {@code @SdkValue(SdkValue.Id.FLOW)}:
     *
     * <pre>{@code
     * public static final ManagedValue<Flow> FLOW = ManagedValue.method(SdkValue.Id.FLOW)
     *         .in("Sdk")                              // the class that holds it | .openedOnly()
     *         .holds(Flow.class, Flow.NONE)           // what it returns, and what a created one starts as
     *         .because("This is the bot's activity flow. Draw it in 🔀 Activity Flow.");
     * }</pre>
     *
     * @throws IllegalArgumentException when the enum is not nested in such an annotation
     */
    public static MethodSteps method(Enum<?> id) {
        return new MethodSteps(Key.of(id, ElementType.METHOD));
    }

    /**
     * <b>The way to declare a parameter of the plugin's own</b> — a {@code @Param} field in its holder that the
     * person running the bot sets, and the plugin's runtime reads by its mark — {@code Bot.runFlow(Sdk.class)} reads
     * {@code Sdk.flow}:
     *
     * <pre>{@code
     * public static final ManagedValue<Boolean> DO_SETUP = ManagedValue.param(SdkValue.Id.DO_SETUP)
     *         .in("Sdk")
     *         .startsAs(SdkParams::doSetup)           // its type, and the value a written field starts as
     *         .shown("Run", "Set the game up first", Param.PUBLIC)
     *         .because("The SDK reads this parameter. Change its value in the Runner or Project ▸ Parameters.");
     * }</pre>
     *
     * <p>The field is named after the id, camel-cased — {@code DO_SETUP} is {@code doSetup} — and marked with it,
     * {@code @SdkValue(SdkValue.Id.DO_SETUP) @Param(…) public static boolean doSetup = true;}. The host appends it
     * to the holder whenever the holder lacks a field carrying the id, and never lets the Parameters window
     * rename, retype, re-describe or remove it: the bot's own code names the field.
     *
     * @throws IllegalArgumentException when the enum is not nested in such an annotation, or its {@code @Target}
     *                                  leaves out fields
     */
    public static ParamSteps param(Enum<?> id) {
        return new ParamSteps(Key.of(id, ElementType.FIELD));
    }

    /**
     * <b>The way to declare an open set</b> — a class of constants the user grows, which a bot marks the holder
     * class with — {@code @SdkValue(SdkValue.Id.PICTURES)} — created empty in the holder:
     *
     * <pre>{@code
     * public static final ManagedValue<ImageTemplate> PICTURES = ManagedValue.openSet(SdkValue.Id.PICTURES)
     *         .of(ImageTemplate.class)                // the class of each constant
     *         .in("Pictures")
     *         .because("Picture constants are managed in 🖼 Manage Pictures.");
     *
     * public static final ManagedValue<Outcome> ACTIVITIES = ManagedValue.openSet(SdkValue.Id.ACTIVITIES)
     *         .ofEnums(Outcome.class, BotConstants::outcome)  // enums nested in the holder: enum Collect { NEXT }
     *         .groupedAs(Activity.class, BotConstants::activity)
     *         .in("Activities")
     *         .because("Activities are managed in 🔀 Activity Flow.");
     * }</pre>
     *
     * @throws IllegalArgumentException when the enum is not nested in a {@link ManagedMarker} annotation
     */
    public static SetSteps openSet(Enum<?> id) {
        return new SetSteps(Key.of(id, ElementType.TYPE));
    }

    /** After {@link #method}: where the value lives. */
    public static final class MethodSteps {

        private final Key key;

        private MethodSteps(Key key) {
            this.key = key;
        }

        /** In the class {@code holder} — {@code "Sdk"} — which the host writes when a project has none. */
        public TypeStep in(String holder) {
            return new TypeStep(key, requireText(holder, "holder"));
        }

        /** Somewhere the host may open and never create: {@code .openedOnly().holds(T.class)}. */
        public OpenedStep openedOnly() {
            return new OpenedStep(key);
        }
    }

    /** After {@link MethodSteps#openedOnly}: what the method returns. */
    public static final class OpenedStep {

        private final Key key;

        private OpenedStep(Key key) {
            this.key = key;
        }

        /** A method returning a {@code type}, which the host opens and never writes. */
        public <T> Reason<T> holds(Class<T> type) {
            if (type == null) throw new IllegalArgumentException(key.id() + ": no type given");
            return new Reason<>(key, null, Shape.METHOD, type, null, null, null, null);
        }
    }

    /** After {@link MethodSteps#in}: what the method returns. */
    public static final class TypeStep {

        private final Key key;
        private final String holder;

        private TypeStep(Key key, String holder) {
            this.key = key;
            this.holder = holder;
        }

        /**
         * A method returning a {@code type}, first returning {@code initial} when the host creates it — or,
         * with {@code null}, the type's own fresh value.
         */
        public <T> Reason<T> holds(Class<T> type, T initial) {
            if (type == null) throw new IllegalArgumentException(key.id() + ": no type given");
            return new Reason<>(key, holder, Shape.METHOD, type, initial, null, null, null);
        }
    }

    /** After {@link #param}: the class the field is in. */
    public static final class ParamSteps {

        private final Key key;

        private ParamSteps(Key key) {
            this.key = key;
        }

        /** In the class {@code holder} — {@code "Sdk"} — which the host writes the field into, or creates. */
        public ParamTypeStep in(String holder) {
            return new ParamTypeStep(key, requireText(holder, "holder"));
        }
    }

    /** After {@link ParamSteps#in}: the field's type and first value. */
    public static final class ParamTypeStep {

        private final Key key;
        private final String holder;

        private ParamTypeStep(Key key, String holder) {
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
        public <T> ParamShownStep<T> startsAs(Ref.Of0<T> initial) {
            if (!(Ref.resolve(initial) instanceof Method method) || method.getReturnType() == void.class) {
                throw new IllegalArgumentException(key.id() + ": startsAs names a method returning the value");
            }
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) boxed(method.getReturnType());
            return new ParamShownStep<>(key, holder, type, method.getGenericReturnType(), initial.call());
        }

        private static Class<?> boxed(Class<?> type) {
            if (!type.isPrimitive()) return type;
            return java.lang.invoke.MethodType.methodType(type).wrap().returnType();
        }
    }

    /** After {@link ParamTypeStep#startsAs}: what the field's {@code @Param} says. */
    public static final class ParamShownStep<T> {

        private final Key key;
        private final String holder;
        private final Class<T> type;
        private final Type form;
        private final T initial;

        private ParamShownStep(Key key, String holder, Class<T> type, Type form, T initial) {
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
        public Reason<T> shown(String category, String description, Param.Visibility visibility) {
            Field field = new Field(form, category == null ? "" : category, description == null ? "" : description,
                    (visibility == null ? Param.Visibility.EDITOR : visibility).id(), null);
            return new Reason<>(key, holder, Shape.PARAM, type, initial, null, null, field);
        }
    }

    /** After {@link #openSet}: what each constant is. */
    public static final class SetSteps {

        private final Key key;

        private SetSteps(Key key) {
            this.key = key;
        }

        /**
         * Constants of the class {@code element} — what {@link PluginValues#add} accepts for this set, and what
         * a constant's initialiser reads as.
         */
        public <E> SetHolderStep<E> of(Class<E> element) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (element.isPrimitive()) {
                throw new IllegalArgumentException(key.id() + ": a constant is an object; use the boxed class of "
                        + element.getName());
            }
            return new SetHolderStep<>(key, element, null, null);
        }

        /**
         * Constants of an enum of the bot's own implementing the interface {@code element}, each a bare name —
         * {@code public enum Outcomes implements Outcome { WON, LOST }} — which the host creates, adds to,
         * renames in and removes from as it does a class's fields, and creates as an empty enum implementing
         * {@code element}.
         *
         * <p>A host cannot load the bot's enum, so it reads {@code Outcomes.WON} as what {@code byName} makes of
         * {@code "WON"}: a value of the plugin's own that equals itself by name, which the type's declaration
         * writes back and the host spells as that constant. The name is the whole member; nothing else about it
         * is written, so a plugin derives anything it shows (a label) from the name.
         *
         * @param byName a method reference from a constant's name to the value it stands for —
         *               {@code BotConstants::outcome}
         * @throws IllegalArgumentException when {@code element} is no interface, or {@code byName} is no method
         *                                  reference
         */
        public <E> SetHolderStep<E> ofEnum(Class<E> element, Ref.Of1<String, ? extends E> byName) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (!element.isInterface()) {
                throw new IllegalArgumentException(key.id() + ": a bot's enum implements its element type, so "
                        + element.getName() + " must be an interface");
            }
            Ref.resolve(byName);
            return new SetHolderStep<>(key, element, byName, null);
        }

        /**
         * Enums of the bot's own <b>nested in the holder class</b>, each implementing the interface
         * {@code element} and each a set of bare names — one enum per group:
         *
         * <pre>{@code
         * @SdkValue(SdkValue.Id.ACTIVITIES)
         * public final class Activities {
         *     public enum Collect implements Outcome { NEXT, NOTHING_LEFT }
         *     public enum Battle implements Outcome { NEXT, WON, LOST }
         * }
         * }</pre>
         *
         * <p>For a set whose members belong to something: here each enum is one activity, and its constants are
         * what that activity can report, so a method returning {@code Activities.Collect} cannot return
         * {@code Battle}'s. The host adds, renames and removes whole enums ({@link PluginValues#addGroup}) as well
         * as the constants in one, and reads {@code Activities.Collect.WON} as what {@code byName} makes of
         * {@code ("Collect", "WON")}. The holder is created as an empty {@code public final class}.
         *
         * @param byName a method reference from an enum's simple name and a constant's name to the value the
         *               constant stands for — {@code BotConstants::outcome}
         * @throws IllegalArgumentException when {@code element} is no interface, or {@code byName} is no method
         *                                  reference
         */
        public <E> GroupStep<E> ofEnums(Class<E> element, Ref.Of2<String, String, ? extends E> byName) {
            if (element == null) throw new IllegalArgumentException(key.id() + ": no element type given");
            if (!element.isInterface()) {
                throw new IllegalArgumentException(key.id() + ": a bot's enum implements its element type, so "
                        + element.getName() + " must be an interface");
            }
            Ref.resolve(byName);
            return new GroupStep<>(key, element, byName);
        }
    }

    /** After {@link SetSteps#ofEnums}: what a whole nested enum reads as. */
    public static final class GroupStep<E> {

        private final Key key;
        private final Class<E> element;
        private final Ref.Of2<String, String, ? extends E> byName;

        private GroupStep(Key key, Class<E> element, Ref.Of2<String, String, ? extends E> byName) {
            this.key = key;
            this.element = element;
            this.byName = byName;
        }

        /**
         * A class literal of one nested enum — {@code Activities.Collect.class} — reads as what {@code byGroup}
         * makes of its simple name, a {@code group}: how a plugin's value names a whole enum, as the SDK's flow
         * names an activity, without the host loading the bot's class. The host writes such a value back as
         * the class literal it was read from.
         *
         * @param group   the plugin's class for one enum — {@code Activity}
         * @param byGroup a method reference from an enum's simple name to that value — {@code BotConstants::activity}
         * @throws IllegalArgumentException when {@code group} is missing or {@code byGroup} is no method reference
         */
        public <G> SetHolderStep<E> groupedAs(Class<G> group, Ref.Of1<String, ? extends G> byGroup) {
            if (group == null) throw new IllegalArgumentException(key.id() + ": no group type given");
            Ref.resolve(byGroup);
            return new SetHolderStep<>(key, element, null, new Grouped(byName, group, byGroup));
        }
    }

    /** After {@link SetSteps#of}, {@link SetSteps#ofEnum} or {@link GroupStep#groupedAs}: the class that is the set. */
    public static final class SetHolderStep<E> {

        private final Key key;
        private final Class<E> element;
        private final Ref.Of1<String, ? extends E> byName;
        private final Grouped grouped;

        private final Ref.Of1<String, ? extends E> referred;

        private SetHolderStep(Key key, Class<E> element, Ref.Of1<String, ? extends E> byName, Grouped grouped) {
            this(key, element, byName, grouped, null);
        }

        private SetHolderStep(Key key, Class<E> element, Ref.Of1<String, ? extends E> byName, Grouped grouped,
                              Ref.Of1<String, ? extends E> referred) {
            this.key = key;
            this.element = element;
            this.byName = byName;
            this.grouped = grouped;
            this.referred = referred;
        }

        /**
         * A reference to one of the set's constants — {@code Flows.DAILY} in {@code Flow.subflow(Flows.DAILY)}, or a
         * parameter {@code = Flows.DAILY} — reads as what {@code byReference} makes of the constant's name, wherever
         * it is read, not as what its initialiser holds; the host writes a value equal to that back as the
         * reference. A constant's own initialiser is still read as its value ({@link PluginValues#open(String,
         * String)}). For a set
         * whose constants are told apart by name, not by value: two flows that both start empty, or one just
         * duplicated from another, are still two flows, and a value that names one must keep naming it. The
         * initialiser is not read either, so one constant naming another never reads the other's.
         *
         * <p>The plugin's value must be of the element type and equal itself by name alone. Only for a set
         * declared {@link SetSteps#of}: an enum's constants are read by name already.
         *
         * @param byReference a method reference from a constant's name to the value that stands for it —
         *                    {@code BotConstants::flow}
         * @throws IllegalArgumentException when this set is an enum set, or {@code byReference} is no method
         *                                  reference
         */
        public SetHolderStep<E> referredToAs(Ref.Of1<String, ? extends E> byReference) {
            if (byName != null || grouped != null) {
                throw new IllegalArgumentException(key.id() + ": an enum's constants are read by name already");
            }
            Ref.resolve(byReference);
            return new SetHolderStep<>(key, element, null, null, byReference);
        }

        /** The class {@code holder} — {@code "Pictures"} — created empty when a project has none. */
        public Reason<E> in(String holder) {
            return new Reason<>(key, requireText(holder, "holder"), Shape.OPEN_SET, element, null, byName, grouped,
                    null, referred);
        }
    }

    /** The last step: the sentence shown when the canvas refuses an edit. */
    public static final class Reason<T> {

        private final Key key;
        private final String holder;
        private final Shape shape;
        private final Class<T> type;
        private final T initial;
        private final Ref.Of1<String, ? extends T> byName;
        private final Grouped grouped;
        private final Field field;
        private final Ref.Of1<String, ? extends T> referred;

        private Reason(Key key, String holder, Shape shape, Class<T> type, T initial,
                       Ref.Of1<String, ? extends T> byName, Grouped grouped, Field field) {
            this(key, holder, shape, type, initial, byName, grouped, field, null);
        }

        private Reason(Key key, String holder, Shape shape, Class<T> type, T initial,
                       Ref.Of1<String, ? extends T> byName, Grouped grouped, Field field,
                       Ref.Of1<String, ? extends T> referred) {
            this.key = key;
            this.holder = holder;
            this.shape = shape;
            this.type = type;
            this.initial = initial;
            this.byName = byName;
            this.grouped = grouped;
            this.field = field;
            this.referred = referred;
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
         * @throws IllegalArgumentException for a value that is not a parameter, or no condition
         */
        public Reason<T> when(Predicate<? super ParamScope> applies) {
            if (field == null) throw new IllegalArgumentException("Only a parameter has a condition");
            if (applies == null) throw new IllegalArgumentException("when needs a condition");
            return new Reason<>(key, holder, shape, type, initial, byName, grouped, field.when(applies), referred);
        }

        /** What owns this value and where to change it instead. */
        public ManagedValue<T> because(String reason) {
            return new ManagedValue<>(key, requireText(reason, "reason"), holder, shape, type, initial, byName,
                    grouped, field, referred);
        }
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("A managed value needs a " + what);
        return text;
    }

    /** Whether this is an open set — the marker on a type — rather than one method's value. */
    public boolean isOpenSet() {
        return shape == Shape.OPEN_SET;
    }

    /**
     * {@code value} as this value's type — for an open set, one constant's — or null when it is not one: how a
     * sink or a window reads what the host or the bot handed over without writing a cast of its own.
     */
    public T cast(Object value) {
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return "ManagedValue[" + key.id() + (holder == null ? "" : " in " + holder) + "]";
    }
}

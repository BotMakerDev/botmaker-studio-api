package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.managed.ManagedMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

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
 * a value is, is its {@link #shape()}.
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
        OPEN_SET("open-set", "a managed set");

        private final String id;
        private final String displayName;

        Shape(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** The stable key: {@code "method"}, {@code "open-set"}. */
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

    private final Key key;
    private final String reason;
    private final String holder;
    private final Shape shape;
    private final Class<T> type;
    private final T initial;

    private ManagedValue(Key key, String reason, String holder, Shape shape, Class<T> type, T initial) {
        this.key = key;
        this.reason = reason;
        this.holder = holder;
        this.shape = shape;
        this.type = type;
        this.initial = initial;
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

    /** The class the marked method returns, or — for an open set — the class of each constant. */
    public Class<T> type() {
        return type;
    }

    /** What a created method first returns, or null for the type's fresh value. */
    public T initial() {
        return initial;
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
     * <b>The way to declare an open set</b> — a class of constants the user grows, which a bot marks the holder
     * class with — {@code @SdkValue(SdkValue.Id.PICTURES)} — created empty in the holder:
     *
     * <pre>{@code
     * public static final ManagedValue<ImageTemplate> PICTURES = ManagedValue.openSet(SdkValue.Id.PICTURES)
     *         .of(ImageTemplate.class)                // the class of each constant
     *         .in("Pictures")
     *         .because("Picture constants are managed in 🖼 Manage Pictures.");
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
            return new Reason<>(key, null, Shape.METHOD, type, null);
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
            return new Reason<>(key, holder, Shape.METHOD, type, initial);
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
            return new SetHolderStep<>(key, element);
        }
    }

    /** After {@link SetSteps#of}: the class that is the set. */
    public static final class SetHolderStep<E> {

        private final Key key;
        private final Class<E> element;

        private SetHolderStep(Key key, Class<E> element) {
            this.key = key;
            this.element = element;
        }

        /** The class {@code holder} — {@code "Pictures"} — created empty when a project has none. */
        public Reason<E> in(String holder) {
            return new Reason<>(key, requireText(holder, "holder"), Shape.OPEN_SET, element, null);
        }
    }

    /** The last step: the sentence shown when the canvas refuses an edit. */
    public static final class Reason<T> {

        private final Key key;
        private final String holder;
        private final Shape shape;
        private final Class<T> type;
        private final T initial;

        private Reason(Key key, String holder, Shape shape, Class<T> type, T initial) {
            this.key = key;
            this.holder = holder;
            this.shape = shape;
            this.type = type;
            this.initial = initial;
        }

        /** What owns this value and where to change it instead. */
        public ManagedValue<T> because(String reason) {
            return new ManagedValue<>(key, requireText(reason, "reason"), holder, shape, type, initial);
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

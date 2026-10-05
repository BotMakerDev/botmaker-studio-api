package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.meta.ReplacedBy;

/**
 * One value this plugin keeps up to date through its own window, named by the id it is annotated with.
 *
 * <p><b>Declare each one once, as a constant, and use the constant everywhere</b>: in
 * {@code StudioPlugin.managedValues()}, in the plugin's runtime half through
 * {@link com.botmaker.plugin.api.managed.ManagedValues#claim}, and in its own window to open the value. The id
 * then exists in exactly one place, and {@link #type()} is what lets the runtime hand the sink a {@code T} and
 * the window read one without a cast.
 *
 * <p><b>What matches</b>: a {@code @Managed("<id>")} method or type in the bot's own Java, where the id is
 * this record's. The annotation is {@code com.botmaker.plugin.api.managed.Managed}, which a bot has on its
 * classpath through the plugin that brings the contract at {@code compile}.
 *
 * <p><b>Two shapes, one marker.</b> On a <b>method</b> it is a fixed value the plugin shipped a declaration
 * for — the host rewrites the expression that method returns and never adds or removes a method. On a
 * <b>type</b> it is an open set the user grows — a class of constants the rest of the bot names at its use
 * sites — and the whole class is the plugin's window's, so the canvas may not add to it, rename in it or
 * delete from it. There {@link #type()} is the class of each constant: see {@link #openSet}. Which of the two
 * a value is, is its {@link #shape()}.
 *
 * <p><b>Where it goes when it is missing.</b> A project that never had the plugin's file has no
 * {@code @Managed} method to open, and nothing else would ever write one. {@code holder} and {@code type} are
 * what the host needs to write it once ({@link PluginValues#create}, or on every bind): the class the value
 * lives in, under {@code plugins/<last id segment>/}, and the type the method returns. What the method first
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

    /** Where the {@code @Managed} annotation goes, and so what the host may change. */
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

    private final String id;
    private final String reason;
    private final String holder;
    private final Shape shape;
    private final Class<T> type;
    private final T initial;

    private ManagedValue(String id, String reason, String holder, Shape shape, Class<T> type, T initial) {
        this.id = id;
        this.reason = reason;
        this.holder = holder;
        this.shape = shape;
        this.type = type;
        this.initial = initial;
    }

    /** The annotation's id — {@code "flow"}, {@code "capture"}, {@code "pictures"}. */
    public String id() {
        return id;
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
     * The class the {@code @Managed} method returns, or — for an open set — the class of each constant. Null
     * only for a value declared through a deprecated untyped step by a plugin built against contract 0.3.
     */
    public Class<T> type() {
        return type;
    }

    /** What a created method first returns, or null for the type's fresh value. */
    public T initial() {
        return initial;
    }

    /**
     * <b>The way to declare a method-shaped value</b>, step by step:
     *
     * <pre>{@code
     * public static final ManagedValue<Flow> FLOW = ManagedValue.method("flow")
     *         .in("Sdk")                              // the class that holds it | .openedOnly()
     *         .holds(Flow.class, Flow.NONE)           // what it returns, and what a created one starts as
     *         .because("This is the bot's activity flow. Draw it in 🔀 Activity Flow.");
     * }</pre>
     */
    public static MethodSteps method(String id) {
        return new MethodSteps(id);
    }

    /**
     * <b>The way to declare an open set</b> — a class of constants the user grows, {@code @Managed} on the
     * type, created empty in the holder:
     *
     * <pre>{@code
     * public static final ManagedValue<ImageTemplate> PICTURES = ManagedValue.openSet("pictures")
     *         .of(ImageTemplate.class)                // the class of each constant
     *         .in("Pictures")
     *         .because("Picture constants are managed in 🖼 Manage Pictures.");
     * }</pre>
     */
    public static SetSteps openSet(String id) {
        return new SetSteps(id);
    }

    /** After {@link #method}: where the value lives. */
    public static final class MethodSteps {

        private final String id;

        private MethodSteps(String id) {
            this.id = requireText(id, "id");
        }

        /** In the class {@code holder} — {@code "Sdk"} — which the host writes when a project has none. */
        public TypeStep in(String holder) {
            return new TypeStep(id, requireText(holder, "holder"));
        }

        /** Somewhere the host may open and never create: {@code .openedOnly().holds(T.class)}. */
        public OpenedStep openedOnly() {
            return new OpenedStep(id);
        }

        /**
         * @deprecated untyped — the value it builds has no {@link #type()}, so the runtime never claims it. Kept
         *             so a plugin built against contract 0.3 still links; use {@link #openedOnly()}.
         */
        @Deprecated
        @ReplacedBy(note = "Declare what the method returns: .openedOnly().holds(T.class).")
        public Reason<Void> notCreated() {
            return new Reason<>(id, null, Shape.METHOD, null, null);
        }
    }

    /** After {@link MethodSteps#openedOnly}: what the method returns. */
    public static final class OpenedStep {

        private final String id;

        private OpenedStep(String id) {
            this.id = id;
        }

        /** A method returning a {@code type}, which the host opens and never writes. */
        public <T> Reason<T> holds(Class<T> type) {
            if (type == null) throw new IllegalArgumentException(id + ": no type given");
            return new Reason<>(id, null, Shape.METHOD, type, null);
        }
    }

    /** After {@link MethodSteps#in}: what the method returns. */
    public static final class TypeStep {

        private final String id;
        private final String holder;

        private TypeStep(String id, String holder) {
            this.id = id;
            this.holder = holder;
        }

        /**
         * A method returning a {@code type}, first returning {@code initial} when the host creates it — or,
         * with {@code null}, the type's own fresh value.
         */
        public <T> Reason<T> holds(Class<T> type, T initial) {
            if (type == null) throw new IllegalArgumentException(id + ": no type given");
            return new Reason<>(id, holder, Shape.METHOD, type, initial);
        }
    }

    /** After {@link #openSet}: what each constant is. */
    public static final class SetSteps {

        private final String id;

        private SetSteps(String id) {
            this.id = requireText(id, "id");
        }

        /**
         * Constants of the class {@code element} — what {@link PluginValues#add} accepts for this set, and what
         * a constant's initialiser reads as.
         */
        public <E> SetHolderStep<E> of(Class<E> element) {
            if (element == null) throw new IllegalArgumentException(id + ": no element type given");
            if (element.isPrimitive()) {
                throw new IllegalArgumentException(id + ": a constant is an object; use the boxed class of "
                        + element.getName());
            }
            return new SetHolderStep<>(id, element);
        }

        /**
         * @deprecated untyped, kept so a plugin built against contract 0.3 still links; a host checks nothing
         *             it adds. Say what each constant is with {@link #of} first.
         */
        @Deprecated
        @ReplacedBy(note = "Declare what each constant is: .of(E.class).in(holder).")
        public Reason<Void> in(String holder) {
            return new Reason<>(id, requireText(holder, "holder"), Shape.OPEN_SET, null, null);
        }
    }

    /** After {@link SetSteps#of}: the class that is the set. */
    public static final class SetHolderStep<E> {

        private final String id;
        private final Class<E> element;

        private SetHolderStep(String id, Class<E> element) {
            this.id = id;
            this.element = element;
        }

        /** The class {@code holder} — {@code "Pictures"} — created empty when a project has none. */
        public Reason<E> in(String holder) {
            return new Reason<>(id, requireText(holder, "holder"), Shape.OPEN_SET, element, null);
        }
    }

    /** The last step: the sentence shown when the canvas refuses an edit. */
    public static final class Reason<T> {

        private final String id;
        private final String holder;
        private final Shape shape;
        private final Class<T> type;
        private final T initial;

        private Reason(String id, String holder, Shape shape, Class<T> type, T initial) {
            this.id = id;
            this.holder = holder;
            this.shape = shape;
            this.type = type;
            this.initial = initial;
        }

        /** What owns this value and where to change it instead. */
        public ManagedValue<T> because(String reason) {
            return new ManagedValue<>(id, requireText(reason, "reason"), holder, shape, type, initial);
        }
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("A managed value needs a " + what);
        return text;
    }

    /** Whether this is an open set — {@code @Managed} on a type — rather than one method's value. */
    public boolean isOpenSet() {
        return shape == Shape.OPEN_SET;
    }

    /**
     * {@code value} as this value's type — for an open set, one constant's — or null when it is not one: how a
     * sink or a window reads what the host or the bot handed over without writing a cast of its own.
     */
    public T cast(Object value) {
        return type != null && type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return "ManagedValue[" + id + (holder == null ? "" : " in " + holder) + "]";
    }
}

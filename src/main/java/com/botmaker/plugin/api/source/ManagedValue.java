package com.botmaker.plugin.api.source;

/**
 * One value this plugin keeps up to date through its own window, named by the id it is annotated with.
 *
 * <p><b>Declare each one once, as a constant, and use the constant everywhere</b> (2026-09-28): in
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
 * delete from it. That one has no {@link #type()}: see {@link #openSet}.
 *
 * <p><b>Where it goes when it is missing</b> (2026-09-25). A project that never had the plugin's file has no
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
 * <p><b>A class, not a record</b> (2026-09-28): only {@link #method} and {@link #openSet}'s steps build one.
 * A record's public canonical constructor changes when a component is added, and every plugin compiled
 * against the old one would throw {@code NoSuchMethodError}; through the steps a new component is a new step.
 *
 * @param <T> the value's class
 */
public final class ManagedValue<T> {

    private final String id;
    private final String reason;
    private final String holder;
    private final Class<T> type;
    private final T initial;

    private ManagedValue(String id, String reason, String holder, Class<T> type, T initial) {
        this.id = id;
        this.reason = reason;
        this.holder = holder;
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

    /** The class the {@code @Managed} method returns, or null for an open set, whose annotation goes on the holder. */
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
     *         .in("Sdk")                              // the class that holds it | .notCreated()
     *         .holds(Flow.class, Flow.NONE)           // what it returns, and what a created one starts as
     *         .because("This is the bot's activity flow. Draw it in 🔀 Activity Flow.");
     * }</pre>
     */
    public static MethodSteps method(String id) {
        return new MethodSteps(id);
    }

    /**
     * <b>The way to declare an open set</b> — a class of constants the user grows, {@code @Managed} on the
     * type: {@code ManagedValue.openSet("pictures").in("Pictures").because("…")}. Created empty in the holder.
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

        /** Somewhere the host may open and never create. */
        public Reason<Void> notCreated() {
            return new Reason<>(id, null, null, null);
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
            return new Reason<>(id, holder, type, initial);
        }
    }

    /** After {@link #openSet}: the class that is the set. */
    public static final class SetSteps {

        private final String id;

        private SetSteps(String id) {
            this.id = requireText(id, "id");
        }

        /** The class {@code holder} — {@code "Pictures"} — created empty when a project has none. */
        public Reason<Void> in(String holder) {
            return new Reason<>(id, requireText(holder, "holder"), null, null);
        }
    }

    /** The last step: the sentence shown when the canvas refuses an edit. */
    public static final class Reason<T> {

        private final String id;
        private final String holder;
        private final Class<T> type;
        private final T initial;

        private Reason(String id, String holder, Class<T> type, T initial) {
            this.id = id;
            this.holder = holder;
            this.type = type;
            this.initial = initial;
        }

        /** What owns this value and where to change it instead. */
        public ManagedValue<T> because(String reason) {
            return new ManagedValue<>(id, requireText(reason, "reason"), holder, type, initial);
        }
    }

    private static String requireText(String text, String what) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("A managed value needs a " + what);
        return text;
    }

    /** Whether this is an open set — {@code @Managed} on a type — rather than one method's value. */
    public boolean isOpenSet() {
        return type == null;
    }

    /**
     * {@code value} as this value's type, or null when it is not one — how a sink or a window reads what the
     * host or the bot handed over without writing a cast of its own.
     */
    public T cast(Object value) {
        return type != null && type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public String toString() {
        return "ManagedValue[" + id + (holder == null ? "" : " in " + holder) + "]";
    }
}

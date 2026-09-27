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
 * @param id      the annotation's id — {@code "flow"}, {@code "capture"}, {@code "pictures"}
 * @param reason  the sentence the host shows when it refuses an edit: what owns this and where to go instead
 * @param holder  the simple name of the class that holds it — {@code "Sdk"}, {@code "Pictures"} — or null
 *                when the host may not create it
 * @param type    the class the {@code @Managed} method returns, or null for an open set, whose annotation goes
 *                on the holder itself
 * @param initial what a created method first returns, or null for the type's fresh value
 * @param <T>     the value's class
 */
public record ManagedValue<T>(String id, String reason, String holder, Class<T> type, T initial) {

    /** A value the host may create in {@code holder}, starting as {@code initial}. */
    public static <T> ManagedValue<T> of(String id, String holder, Class<T> type, T initial, String reason) {
        return new ManagedValue<>(id, reason, holder, type, initial);
    }

    /** A class of constants the user grows ({@code @Managed} on the type), created empty in {@code holder}. */
    public static ManagedValue<Void> openSet(String id, String holder, String reason) {
        return new ManagedValue<>(id, reason, holder, null, null);
    }

    /** A value the host can open and never create. */
    public static ManagedValue<Void> openOnly(String id, String reason) {
        return new ManagedValue<>(id, reason, null, null, null);
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
}

package com.botmaker.plugin.api.source;

import java.lang.reflect.Type;

/**
 * One value this plugin keeps up to date through its own window, named by the id it is annotated with.
 *
 * <p><b>What matches</b>: a {@code @Managed("<id>")} method or type in the bot's own Java, where the id is
 * this record's. The annotation is {@code com.botmaker.plugin.api.managed.Managed}, which a bot has on its
 * classpath through the plugin that brings the contract at {@code compile}.
 *
 * <p><b>Two shapes, one marker.</b> On a <b>method</b> it is a fixed value the plugin shipped a declaration
 * for — the host rewrites the expression that method returns and never adds or removes a method. On a
 * <b>type</b> it is an open set the user grows — a class of constants the rest of the bot names at its use
 * sites — and the whole class is the plugin's window's, so the canvas may not add to it, rename in it or
 * delete from it.
 *
 * <p><b>This replaced a match on a declared type</b> (2026-09-20). A {@code ManagedField} said "every
 * {@code static final} field of type {@code T}", which cannot tell two same-typed classes apart and had to
 * guess that a class of nothing but such constants was managed whole. An annotation is a statement where
 * those were inferences from shape, and it carries the id that says <em>which</em> of a plugin's values a
 * method holds — so two {@code CaptureSource} methods are distinguishable.
 *
 * <p><b>Where it goes when it is missing</b> (2026-09-25). A project that never had the plugin's file has no
 * {@code @Managed} method to open, and nothing else would ever write one. {@code holder} and {@code valueType}
 * are what the host needs to write it once, on the plugin's request ({@link PluginValues#create}): the class
 * the value lives in, under {@code plugins/<last id segment>/}, and the type the method returns. What the
 * method first returns is {@code initial}, written by the host's grammar as any value is; with none, the
 * type's own fresh value — which a type declared only by its parts ({@code ComponentType}) has not got, hence
 * the component. A plugin that gives no holder has values that can only be opened, never created.
 *
 * <p>Plain data, like every other contribution: the host reads the id as text and does the matching itself,
 * so no plugin code runs while a file is drawn.
 *
 * @param id        the annotation's id — {@code "flow"}, {@code "capture"}, {@code "pictures"}
 * @param reason    the sentence the host shows when it refuses an edit: what owns this and where to go instead
 * @param holder    the simple name of the class that holds it — {@code "Sdk"}, {@code "Pictures"} — or null
 *                  when the host may not create it
 * @param valueType what the {@code @Managed} method returns, or null when the annotation goes on the holder
 *                  itself: an open set, created as an empty class
 * @param initial   what a created method first returns — a value of {@code valueType} — or null for the
 *                  type's fresh value
 */
public record ManagedValue(String id, String reason, String holder, Type valueType, Object initial) {

    /** A value the host can open and never create. */
    public ManagedValue(String id, String reason) {
        this(id, reason, null, null, null);
    }

    /** A value the host can create, starting as its type's fresh value. */
    public ManagedValue(String id, String reason, String holder, Type valueType) {
        this(id, reason, holder, valueType, null);
    }
}

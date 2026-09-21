package com.botmaker.plugin.api.source;

/**
 * One value this plugin keeps up to date through its own window, named by the id it is annotated with.
 *
 * <p><b>What matches</b>: a {@code @Managed("<id>")} method or type in the bot's own Java, where the id is
 * this record's. The annotation is {@code com.botmaker.plugin.basics.managed.Managed} — a bot's own
 * classpath, never the contract's, for the same reason {@code @Param} lives there: a bot has no contract
 * jar.
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
 * <p>Plain data, like every other contribution: the host reads the id as text and does the matching itself,
 * so no plugin code runs while a file is drawn.
 *
 * @param id     the annotation's id — {@code "flow"}, {@code "capture"}, {@code "pictures"}
 * @param reason the sentence the host shows when it refuses an edit: what owns this and where to go instead
 */
public record ManagedValue(String id, String reason) {
}

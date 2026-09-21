/**
 * The Java in a bot that holds a plugin's values, and the one expression in it the host may rewrite.
 *
 * <p>{@link com.botmaker.plugin.api.source.ManagedValue} names a value the plugin's own window keeps up to
 * date, and {@link com.botmaker.plugin.api.source.PluginValues} is how the plugin reads and writes that
 * value while a project is open. {@link com.botmaker.plugin.api.source.SourceSeed} is a different thing
 * with a similar name: what a <em>fresh</em> value of one of this plugin's types looks like, written as
 * Java, for the moment a slot needs one and there is nothing there yet.
 *
 * <h2>What is deliberately not here</h2>
 * Generation, and — since 2026-09-21 — <b>the file itself</b>. {@code PluginSource} stood here for a day:
 * a plugin handed over a class's whole text and the host copied it into the project. The half that
 * mattered survived and is everything above — a plugin's values are compiled Java in the bot's own source,
 * and the host rewrites nothing but the expression a {@code @Managed} method returns, never the class, a
 * method, an import block or a body it did not write. What went is the host putting the first copy there:
 * a project gets its file from the template it was created from, and a plugin that wants to add one to an
 * existing project writes it from its own window, at a click.
 *
 * <p>A body that is not exactly {@code return <expr>;} is read-only with a reason rather than partially
 * parsed. The annotation itself lives in {@code botmaker-plugin-basics}, beside {@code @Param}, and not in
 * this contract: a bot has no contract jar.
 */
package com.botmaker.plugin.api.source;

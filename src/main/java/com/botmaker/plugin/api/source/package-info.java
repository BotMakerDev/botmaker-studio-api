/**
 * The Java a plugin gives a bot, and the one expression in it the host may rewrite.
 *
 * <p>{@link com.botmaker.plugin.api.source.PluginSource} is the file, copied into the project once and the
 * user's from that moment. {@link com.botmaker.plugin.api.source.ManagedValue} names a value inside it that
 * the plugin's own window keeps up to date, and
 * {@link com.botmaker.plugin.api.source.PluginValues} is how the plugin reads and writes that value while a
 * project is open. {@link com.botmaker.plugin.api.source.SourceSeed} is a different thing with a similar
 * name: what a <em>fresh</em> value of one of this plugin's types looks like, written as Java, for the
 * moment a slot needs one and there is nothing there yet.
 *
 * <h2>What is deliberately not here</h2>
 * Generation. The host writes a plugin's file once, never regenerates it, and afterwards rewrites nothing
 * but the expression a {@code @Managed} method returns — never the class, a method, an import block or a
 * body it did not write. A body that is not exactly {@code return <expr>;} is read-only with a reason
 * rather than partially parsed. The annotation itself lives in {@code botmaker-plugin-basics}, beside
 * {@code @Param}, and not in this contract: a bot has no contract jar.
 */
package com.botmaker.plugin.api.source;

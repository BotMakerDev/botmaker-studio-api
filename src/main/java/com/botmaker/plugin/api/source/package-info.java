/**
 * The Java in a bot that holds a plugin's values, and the one expression in it the host may rewrite.
 *
 * <p>{@link com.botmaker.plugin.api.source.ManagedValue} names a value the plugin's own window keeps up to
 * date, and {@link com.botmaker.plugin.api.source.PluginValues} is how the plugin reads and writes that
 * value while a project is open. What a <em>fresh</em> value of one of the plugin's types is, is
 * {@link com.botmaker.plugin.api.value.PluginType#fresh()}, a value rather than Java text.
 *
 * <h2>What is deliberately not here</h2>
 * Generation, and <b>the file itself</b>. A plugin's values are compiled Java in the bot's own source, and the
 * host rewrites nothing but the expression a {@code @Managed} method returns, never the class, a method, an
 * import block or a body it did not write. A project gets its file from the template it was created from; a
 * value whose holder is missing is written once by the host from what {@code ManagedValue} declares.
 *
 * <p>A body that is not exactly {@code return <expr>;} is read-only with a reason rather than partially
 * parsed. The annotation itself is {@link com.botmaker.plugin.api.managed.Managed}, beside
 * {@link com.botmaker.plugin.api.params.Param}: both sit on a bot's own declarations.
 */
package com.botmaker.plugin.api.source;

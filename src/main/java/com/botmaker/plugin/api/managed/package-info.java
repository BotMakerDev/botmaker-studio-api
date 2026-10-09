/**
 * The annotation a <b>bot's own</b> methods and types carry to say a plugin's window keeps them up to date.
 *
 * <p>{@link com.botmaker.plugin.api.managed.Managed}, or a plugin's own annotation marked
 * {@link com.botmaker.plugin.api.managed.ManagedMarker} for a typed id. The host rewrites the expression such a
 * method returns and nothing else — never the class, a method, an import block or a body it did not write
 * — which is the whole of {@code docs/refactor/33-plugin-java.md}. The plugin side is
 * {@link com.botmaker.plugin.api.source.ManagedValue} and
 * {@link com.botmaker.plugin.api.source.PluginValues}, paired by the id.
 *
 * <p>It is here, beside {@link com.botmaker.plugin.api.params.Param}, for the reason given there.
 */
package com.botmaker.plugin.api.managed;

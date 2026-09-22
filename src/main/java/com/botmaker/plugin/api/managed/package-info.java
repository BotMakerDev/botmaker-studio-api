/**
 * The annotation a <b>bot's own</b> methods and types carry to say a plugin's window keeps them up to date.
 *
 * <p>One type, {@link com.botmaker.plugin.api.managed.Managed}. The host rewrites the expression such a
 * method returns and nothing else — never the class, a method, an import block or a body it did not write
 * — which is the whole of {@code docs/refactor/33-plugin-java.md}. The plugin side is
 * {@link com.botmaker.plugin.api.source.ManagedValue} and
 * {@link com.botmaker.plugin.api.source.PluginValues}, paired by the id.
 *
 * <p>It moved here from {@code botmaker-plugin-basics} on 2026-09-22, with
 * {@link com.botmaker.plugin.api.params.Param}, for the reason given there.
 */
package com.botmaker.plugin.api.managed;

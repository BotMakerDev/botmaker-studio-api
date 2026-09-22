/**
 * The annotation a <b>bot's own fields</b> carry to become parameters its user may change.
 *
 * <p>One type, {@link com.botmaker.plugin.api.params.Param}, and it is the only thing in this module that
 * is compiled into somebody's bot rather than into a plugin. It moved here from
 * {@code botmaker-plugin-basics} on 2026-09-22 with {@link com.botmaker.plugin.api.managed.Managed}: both
 * sit on a bot's own declarations, both were kept out of the contract only because this module was
 * {@code provided} on the SDK and so absent from a bot's classpath, and that scope is {@code compile} now.
 *
 * <p>Not to be confused with {@code com.botmaker.plugin.api.parameters}, which is the <em>row</em> a
 * parameter is read and drawn as. This package is the declaration; that one is what the host makes of it.
 */
package com.botmaker.plugin.api.params;

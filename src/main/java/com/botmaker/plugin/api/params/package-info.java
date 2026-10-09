/**
 * The annotation a <b>bot's own fields</b> carry to become parameters its user may change.
 *
 * <p>One type, {@link com.botmaker.plugin.api.params.Param}, and it is the only thing in this module that
 * is compiled into somebody's bot rather than into a plugin, beside
 * {@link com.botmaker.plugin.api.managed.ManagedMarker} (meta-annotating a plugin's own annotation) and the
 * {@code managed} runtime: both sit on a bot's own declarations, and a bot has this
 * module because the SDK declares it at {@code compile}. This package is the declaration; the row it is read
 * and drawn as is the host's.
 */
package com.botmaker.plugin.api.params;

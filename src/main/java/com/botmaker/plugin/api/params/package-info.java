/**
 * What a <b>bot's own</b> {@code params} package is made of: its parameters, its pools, and the claims a run makes.
 *
 * <ul>
 *   <li>{@link com.botmaker.plugin.api.params.Param}: a group of parameters, the values its user may change.
 *   <li>{@link com.botmaker.plugin.api.params.Pools} and {@link com.botmaker.plugin.api.params.Pool} (2026-11-15): the
 *       layer above them, what several runs at once are dealt from, each pool filling one parameter.
 *   <li>{@link com.botmaker.plugin.api.params.Claims} (2026-11-15): a value only a running bot knows, held so no other
 *       live run takes it.
 * </ul>
 *
 * <p>This package is compiled into somebody's bot rather than into a plugin, beside
 * {@link com.botmaker.plugin.api.managed.ManagedMarker} (meta-annotating a plugin's own annotation) and the
 * {@code managed} runtime: all sit in a bot's own code, and a bot has this module because the SDK declares it at
 * {@code compile}. This package is the declaration; how it is read, dealt and drawn is the host's.
 */
package com.botmaker.plugin.api.params;

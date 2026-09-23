/**
 * Curation written on the thing it curates.
 *
 * <p>A plugin annotates its palette classes, and everything is read off them:
 *
 * <pre>{@code
 * @Palette(category = "vision", categoryLabel = "Vision", icon = "🔍", order = 20)
 * public final class ImageFinder {
 *
 *     public static MatchResult find(ImageTemplate t) { … }        // offered
 *
 *     @Hidden("the confidence override belongs in BotSettings")
 *     public static MatchResult find(ImageTemplate t, double c) { … }
 *
 *     @PaletteLabel("Find any of…")
 *     public static MatchResult findAny(ImageTemplate... t) { … }
 * }
 *
 * // in the plugin, which finds every @Palette class in its own jar:
 * PaletteCatalog.scan(MyPlugin.class);
 * }</pre>
 *
 * <p><b>Opt-out, not opt-in, and that is the whole point.</b> Under a hand-written catalog a new public
 * method defaults to <em>absent from the menus</em>, which is a silent outcome — the method exists, compiles
 * and is supported, and nobody notices it was never proposed. Here it is offered the moment it is written,
 * and declining it is a deliberate line of source carrying a reason.
 *
 * <h2>Two bits per class, not three</h2>
 *
 * <p>{@link com.botmaker.plugin.api.palette.Palette} means <b>catalogued</b>: recognised as a call into this
 * plugin, filed under it, and available for the editor's "who owns this simple name" question.
 * {@link com.botmaker.plugin.api.palette.Hidden} on the type means <b>not offered</b> — catalogued all the
 * same, just never listed in the insert menus. A three-valued {@code role} element said this until
 * 2026-08-27, and every consumer of it only ever read one bit.
 *
 * <h2>Runtime retention, and why all four are alike now</h2>
 *
 * <p>All four annotations here are {@code RUNTIME}-retained, because the plugin reflects its own classes to
 * build the catalog. {@code @Palette} was {@code CLASS}-retained while an annotation processor read it at
 * compile time and generated the catalog into the plugin's jar; the processor was deleted on 2026-08-27,
 * along with the module it lived in.
 *
 * <p>It went because its one defended property does not need defending. A generated catalog <em>named</em>
 * members, so javac refusing a name that no longer compiled was what kept it honest — but reflection
 * <em>discovers</em> them, and an annotation cannot be attached to a method that does not exist. It also cost
 * something real: a plugin whose pom forgot {@code <annotationProcessorPaths>} silently got no catalog at
 * all, with nothing to tell its author why.
 *
 * <p>Runtime retention on classes a bot loads is safe. A bot does not depend on this module (the SDK's
 * dependency on it is {@code optional}, so it is never transitive), and the JVM parses annotations lazily and
 * silently omits any whose type cannot be resolved. A bot that never reflects over its own facades never
 * looks; a bot that does gets the facade's methods without these.
 */
package com.botmaker.plugin.api.palette;

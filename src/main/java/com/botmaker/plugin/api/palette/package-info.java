/**
 * Curation written on the thing it curates.
 *
 * <p>A plugin annotates its palette classes, the host finds them in the plugin's jar, and everything else is
 * read off them:
 *
 * <pre>{@code
 * @Palette(category = "vision", categoryLabel = "Vision", icon = "🔍")
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
 * }</pre>
 *
 * <p><b>Opt-out, not opt-in, and that is the whole point.</b> Under a hand-written catalog a new public
 * method defaults to <em>absent from the menus</em>, which is a silent outcome — the method exists, compiles
 * and is supported, and nobody notices it was never proposed. Here it is offered the moment it is written,
 * and declining it is a deliberate line of source carrying a reason.
 *
 * <h2>Offered is annotated; catalogued is derived</h2>
 *
 * <p>{@link com.botmaker.plugin.api.palette.Palette} means <b>offered</b>: the class has its own entry in the
 * insert menus. Every type of the same jar an offered call takes or returns, transitively, is
 * <b>catalogued</b> with it — recognised as this plugin's, its members listed on a variable of it — with no
 * annotation at all.
 *
 * <h2>Runtime retention, and why all four are alike now</h2>
 *
 * <p>All four annotations here are {@code RUNTIME}-retained, because the plugin reflects its own classes to
 * build the catalog, rather than an annotation processor generating it at compile time.
 *
 * <p>A processor's one defended property does not need defending. A generated catalog <em>names</em>
 * members, so javac refusing a name that no longer compiles is what keeps it honest — but reflection
 * <em>discovers</em> them, and an annotation cannot be attached to a method that does not exist. A processor
 * also costs something real: a plugin whose pom forgets {@code <annotationProcessorPaths>} silently gets no
 * catalog at all, with nothing to tell its author why.
 *
 * <p>Runtime retention on classes a bot loads is safe. A bot that has this module on its classpath (through
 * a plugin that brings it at {@code compile}) carries them at no cost; one that does not still loads, because
 * the JVM parses annotations lazily and silently omits any whose type cannot be resolved. A bot that never
 * reflects over its own facades never looks.
 */
package com.botmaker.plugin.api.palette;

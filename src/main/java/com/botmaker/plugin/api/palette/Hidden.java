package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keep this out of the palette. It exists, it is public, it is supported — it is simply never proposed.
 *
 * <p>This is the exact claim the annotation makes, and it is worth stating because it has been both wider and
 * narrower before. It began as {@code @NotInPalette}, became {@code @Internal} on 2026-08-27 with the wider
 * meaning <em>not versioned surface</em>, and came back to this on the same day's rework. Versioning is
 * {@link com.botmaker.plugin.api.meta.ReplacedBy}'s business and {@code @Deprecated}'s; a package called
 * {@code internal} is how a module says what is freely breakable. Neither needs an annotation whose real job
 * is a menu.
 *
 * <p>The name is Swagger's, for the same reason Swagger has it: <em>this exists, keep it out of the generated
 * surface</em>.
 *
 * <h2>Not on a type</h2>
 *
 * <p>It sat on thirty-eight types until 2026-09-30, beside {@link Palette}, to mean "catalogued, never
 * offered". A type that should not be offered now simply carries no {@code @Palette}: if an offered call
 * takes or returns it, it is catalogued anyway ({@link Palette} says how), and if none does, nothing in the
 * editor should name it. A hidden member is still left out of that reach — a type only a hidden member names
 * is not catalogued by it.
 *
 * <h2>On a member, the unit is the member name</h2>
 *
 * <p>A palette entry is one name; the menu shows a single lead shape and puts the rest behind a submenu, so
 * every overload of a name is reachable from the one entry and there is nothing an overload could usefully be
 * excluded <em>from</em>. Mark <b>one</b> overload and the whole name goes. This never means "prefer a
 * different shape of the same call" — that is {@link PaletteDefault}.
 *
 * <p>{@code Object} overrides ({@code toString}, {@code equals}, {@code hashCode}) and the synthetic
 * {@code values()} / {@code valueOf(String)} of an enum need no annotation:
 * {@link com.botmaker.plugin.api.catalog.PaletteCatalog#of(Class[])} never offers them.
 *
 * <h2>Retention</h2>
 *
 * <p>{@code RUNTIME}, because the catalog is built by reflecting the real {@code Class<?>}. That is safe on a
 * class a bot loads: a bot does not depend on this module (the SDK's dependency on it is {@code optional}, so
 * it is never transitive), and the JVM parses annotations lazily and silently omits any whose type cannot be
 * resolved.
 *
 * @see Palette
 * @see PaletteDefault
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Hidden {

    /**
     * Why, in the author's own words. Never shown to a user — the member is simply absent — but it is the
     * only record of the judgement, and the next person to wonder why a public method is missing from a menu
     * reads it here.
     *
     * <p>On a member it reads as a statement about the <em>name</em>, since that is what is being hidden,
     * even though it is written on one overload of it.
     */
    String value() default "";
}

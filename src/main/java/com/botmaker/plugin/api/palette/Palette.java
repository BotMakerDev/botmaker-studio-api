package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Offers a class in the insert menus. The host finds every class carrying it in the plugin's jar and
 * catalogues it ({@link com.botmaker.plugin.api.catalog.PaletteCatalog#of(Class[]) PaletteCatalog.of}),
 * offering every public method it declares except the ones marked {@link Hidden}.
 *
 * <p>This annotation is the whole declaration: no list of classes is written anywhere else. The retention is
 * {@code RUNTIME} because the catalog is read off live {@code Class} objects.
 *
 * <h2>Offered, and what is catalogued with it</h2>
 *
 * <p>A class carrying this annotation is <b>offered</b>: it has its own entry in the insert menus. Everything
 * its offered methods take or give back that lives in the same jar — {@code Point} from
 * {@code Mouse.click(Point)}, {@code MatchResult} from {@code ImageFinder.find}, and what those reach in turn
 * — is <b>catalogued</b> with it without an annotation: recognised as this plugin's, so {@code Point} in a
 * bot's source means this plugin's and not {@code java.awt}'s, and its members listed on a variable of it.
 * A public class nothing offered reaches is neither, and the editor never proposes it.
 *
 * <p>Until 2026-09-30 this annotation meant catalogued and a type-level {@link Hidden} beside it meant "not
 * offered": thirty-eight SDK types carried both, and the class dropdown, which read only the first bit,
 * listed every one of them. What a user reaches through an offered call is the catalogue by construction now.
 *
 * <h2>Why every element is a {@code String}</h2>
 *
 * <p>{@link #category()} names a {@link com.botmaker.plugin.api.catalog.Category}, which is a record and
 * deliberately open — a plugin defines its own — so no closed element type could express it. An annotation
 * element's type must also be resolvable wherever the annotation is applied, and the interesting application
 * sites are in modules that depend on this one.
 *
 * <h2>Order is the label's</h2>
 *
 * <p>Offered classes are listed alphabetically by their label, categories too. An {@code order} element
 * ranked them until 2026-09-30, and a menu of thirty facades in an order only their author could read was
 * the complaint that removed it. Member order within a class is still the author's: it is read from the order
 * the methods appear in the compiled class file, which javac writes in source order.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Palette {

    /**
     * The {@link com.botmaker.plugin.api.catalog.Category#id() category id} this class is filed under.
     * Classes sharing an id land in one menu group, so the id — not the label — is what two plugins must
     * agree on.
     */
    String category();

    /**
     * The category's user-visible name. Only one class in a category need give it; the rest may leave it
     * blank and inherit. Two classes giving <em>different</em> non-blank labels for one id is a problem
     * {@link com.botmaker.plugin.api.catalog.PaletteCatalog#of(Class[])} reports, since a menu group cannot
     * have two names.
     */
    String categoryLabel() default "";

    /** A glyph for the class's own menu entry. */
    String icon() default "";

    /** The class's user-visible name; blank means its simple name. */
    String label() default "";
}

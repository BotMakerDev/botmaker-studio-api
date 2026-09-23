package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as part of this plugin's palette. The host finds every class carrying it in the plugin's
 * jar and catalogues it ({@link com.botmaker.plugin.api.catalog.PaletteCatalog#of(Class[])
 * PaletteCatalog.of}), offering every public method it declares except the ones marked {@link Hidden}.
 *
 * <p>This annotation is the whole declaration: no list of classes is written anywhere else. The retention is
 * {@code RUNTIME} because the catalog is read off live {@code Class} objects.
 *
 * <h2>Catalogued, and offered or not</h2>
 *
 * <p>There are two bits here, not three. A class carrying this annotation is <b>catalogued</b> — the editor
 * recognises a call into it, files it under this plugin, and can decide that {@code Point} in a bot's source
 * means this plugin's and not {@code java.awt}'s. Whether it is also <b>offered</b> in the insert menus is
 * the separate question {@link Hidden} answers on the type.
 *
 * <p>The predecessor spelled this as a three-valued {@code role} element — {@code MENU}, {@code HIDDEN} and
 * {@code VALUE} — where every consumer only ever read one bit of it and nothing distinguished the last two.
 * A value type carrying {@code @Palette} and {@code @Hidden} says the same thing more plainly, and being in
 * the recognition set is the truthful answer for it: a plugin's own value type <em>is</em> its API.
 *
 * <h2>Why every element is a {@code String}</h2>
 *
 * <p>{@link #category()} names a {@link com.botmaker.plugin.api.catalog.Category}, which is a record and
 * deliberately open — a plugin defines its own — so no closed element type could express it. An annotation
 * element's type must also be resolvable wherever the annotation is applied, and the interesting application
 * sites are in modules that depend on this one.
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

    /**
     * Where it sits among its peers. Classes are ordered by this number and then by simple name, so a group
     * can be laid out without every member of it being renumbered when one is inserted — leave gaps.
     *
     * <p>Member order within a class needs no element: it is read from the order the methods appear in the
     * compiled class file, which javac writes in source order — the order the author already wrote.
     */
    int order() default 100;
}

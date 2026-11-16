// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The name this member reads under in the palette, when its own is not the clearest thing to show — a
 * {@code sleepMillis} that should read <i>Wait (milliseconds)</i>, a {@code findAny} that should read
 * <i>Find any of…</i>.
 *
 * <p>It labels a <em>member name</em>, not one overload: every overload of the annotated method shares the
 * label, because they share a submenu. Annotating two overloads of one name with different labels is an
 * error the processor reports.
 *
 * <p>A label on a member that is not offered — {@link Hidden}, or not public — is reported in
 * {@link com.botmaker.plugin.api.catalog.PaletteCatalog#problems()} rather than silently ignored: it means
 * the author believed a menu entry existed that does not.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface PaletteLabel {

    /** The text shown in the menu. */
    String value();
}

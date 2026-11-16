// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Of the overloads sharing this member's name, <b>this</b> is the one the palette leads with.
 *
 * <p>A palette entry is a member <em>name</em>. Picking it inserts one shape — the lead — and the name's
 * other shapes sit behind a submenu, so all of them stay one gesture away. That is why an overload is never
 * hidden: hiding is a verdict about a name ({@link Hidden}), and choosing
 * between shapes of one name
 * is this.
 *
 * <p><b>Usually you write nothing.</b> With no annotation the lead is the shape with the fewest parameters,
 * which is right almost every time — it is the one whose omitted arguments come from project settings, and
 * the longer shapes are what a bot reaches for once it wants to override them. One annotation settles a
 * name however many overloads it has, so a six-overload family needs at most one line, and needs it only
 * when the count does not decide:
 *
 * <ul>
 *   <li><b>a tie</b> — two shapes of the same width, where the wider-typed one is the better first offer
 *       ({@code seconds(double)} over {@code seconds(int)}: the fractional spelling accepts whole seconds
 *       too, so it never has to be widened later);
 *   <li><b>a deliberately longer lead</b> — where the short shape exists for a caller who already holds
 *       something, and the shape worth proposing to someone starting out is the explicit one.
 * </ul>
 *
 * <p>The choice is editorial and nothing derives it, which is the whole reason it is written down rather
 * than computed. What <em>is</em> computed is everything else: the submenu is every other overload, in
 * parameter-count order, and it needs no declaration at all.
 *
 * <p>Retention is {@code RUNTIME} for the same reason as {@link Hidden} —
 * {@link com.botmaker.plugin.api.catalog.PaletteCatalog#of(Class[])} reads it off the live {@code Class}. It
 * is safe on a class a bot loads: the plugin-contract dependency is {@code optional} and never transitive,
 * so this annotation's type is simply absent from a bot's classpath, and the JVM omits an annotation whose
 * type it cannot resolve rather than failing.
 *
 * @see Hidden
 * @see PaletteLabel
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface PaletteDefault {
}

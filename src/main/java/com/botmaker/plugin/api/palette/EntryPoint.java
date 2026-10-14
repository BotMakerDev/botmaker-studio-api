package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Offer this only where a program starts: as a statement inside {@code public static void main(String[])}, and
 * nowhere else.
 *
 * <p>For the call that <em>is</em> a bot — the SDK's {@code Bot.runFlow(Sdk.class)} — which belongs in
 * {@code main} and is wrong anywhere else: inside an activity it would start a second run within the first.
 * {@link Hidden} kept such a call out of every menu, so a new project had no statement that launched it; this
 * keeps it in the one menu where it belongs.
 *
 * <p>Like {@link Hidden}, the unit is the member <em>name</em>: mark one overload and every overload of the name
 * is an entry point. A member carrying both is hidden.
 *
 * <p>{@code RUNTIME}, for the reason {@link Hidden} gives: the host reads it off the facade's real class
 * ({@link com.botmaker.plugin.api.catalog.FacadeEntry#entryPoint(String)}).
 *
 * @see Palette
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EntryPoint {
}

// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class of the bot's {@code params} package that holds its <b>pools</b>: what several runs of the bot at once are
 * dealt from (2026-11-15, spec {@code docs/superpowers/specs/2026-11-15-run-claims-design.md} §1).
 *
 * <pre>{@code
 * @Pools
 * public final class FarmPools {
 *     public Pool<GameAccount> accounts() {
 *         return Pool.of(GameAccount.MAIN, GameAccount.ALT_1).fills(Farm::account).rotate();
 *     }
 * }
 * }</pre>
 *
 * <p>A pool is a layer above the parameters, never one of them: each instance method with no parameter returning a
 * {@link Pool} is one pool, which fills one parameter of a {@link Param} group by method reference. The parameter
 * itself, its type, its body and every call of it, stays as a single run wrote it, so a bot run once reads its own
 * values and never sees a pool.
 *
 * <p>Like a parameter group, the class is private to this computer ({@code params/}) and mirrored with the author's
 * starting pools under {@code starting-params/}. The host reads it as values, through its grammar, and deals each run a
 * row of elements; the run's agent makes the filled methods return that run's elements.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Pools {
}

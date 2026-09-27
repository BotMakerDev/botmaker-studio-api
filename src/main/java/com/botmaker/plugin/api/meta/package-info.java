/**
 * What a plugin says <em>about</em> its own surface over time, as opposed to what the surface is.
 *
 * <p>One annotation lives here: {@link com.botmaker.plugin.api.meta.ReplacedBy}, the forward pointer a
 * renamed or retired element carries to whatever takes its place. It is the whole compatibility vocabulary,
 * and that is a deliberate narrowing — as of 2026-08-27 this package held three.
 *
 * <p>Since 2026-09-27 it holds a second, facing the other way: {@link com.botmaker.plugin.api.meta.Refactor} is
 * what a host writes on a <em>bot's</em> function when a refactor had to guess there — a plugin upgrade, a
 * signature change, a plugin's own rewrite — so the user can find and review it.
 *
 * <p>{@code @Replaces}, the back edge, existed because a host upgrading a bot holds only two jars: the
 * version the bot pins and the version it is moving to. A bot jumping 1.0 → 3.0 could not see a pointer added
 * in 2.0 on an element deleted in 3.0, so the surviving element had to name what it replaced. It goes because
 * the SDK now <b>never deletes</b> a public {@code api.*} element — a rename adds the new name, deprecates
 * the old and points one at the other, and both stay — which is enforced by japicmp rather than by an
 * annotation. Under that rule the target jar always still carries the forward pointer, and one end answers
 * every upgrade including a skipped one.
 *
 * <p>{@code @Since} went with it. It grouped a <i>what's new</i> list by version, which the release's own
 * changelog already says; a version written on an element is a second record of a fact the release notes
 * carry, and it drifts.
 *
 * <p>{@link com.botmaker.plugin.api.palette.Hidden} used to live here too, as {@code @Internal}, on the
 * argument that declining to offer a member was one consequence of the larger claim <em>not versioned
 * surface</em>. It moved back to {@code palette} because the larger claim was never what any of its sixteen
 * uses meant, and because a package named {@code internal} says it better than an annotation can.
 */
package com.botmaker.plugin.api.meta;

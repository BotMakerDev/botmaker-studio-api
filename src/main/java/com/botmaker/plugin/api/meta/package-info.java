/**
 * The marks a host and a bot's author write on a bot's own code as it moves between versions of the APIs it
 * calls. A plugin writes none of them, and says nothing about its own changes.
 *
 * <p>{@link com.botmaker.plugin.api.meta.Migrate} is the author's rule: what one element of an old API — a
 * method, a constructor, a field, a whole type — becomes, named by the rule's own signature and kept in the bot.
 * The host resolves the old element against the jar the bot moves <em>from</em> and substitutes the rule at each
 * use, so a plugin may delete or reshape any element outright: no version has to carry the old and the new side
 * by side, and no {@link java.lang.Deprecated} is needed for the move to work.
 * {@link com.botmaker.plugin.api.meta.Migrated} marks a managed value such a rule wrote beside its original.
 *
 * <p>{@link com.botmaker.plugin.api.meta.Refactor} is what a host writes on a bot's function when a refactor had
 * to guess there — a site no rule could rewrite, a rule only seeded, a signature change — so the user can find
 * and review it.
 */
package com.botmaker.plugin.api.meta;

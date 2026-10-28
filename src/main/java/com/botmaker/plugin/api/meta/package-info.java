/**
 * The marks that carry a bot's code between versions of the APIs it calls: written by the host and the bot's
 * author on the bot's own code, and, for {@link com.botmaker.plugin.api.meta.Migrate}, by a plugin on its own.
 *
 * <p>{@link com.botmaker.plugin.api.meta.Migrate} is a rule: what one element of an old API — a method, a
 * constructor, a field, a whole type — becomes, named by the rule's own signature. The bot's author keeps one in
 * the bot; a plugin may ship one beside an element it marks {@code @Deprecated(forRemoval = true)}, read by the
 * host from its sources jar, and removes both in its next release (2026-10-28). The author's rule wins. The host
 * resolves the old element against the jar the bot moves <em>from</em> and substitutes the rule at each use. A
 * plugin need not ship a rule: it may delete or reshape an element outright, and the bot's author writes the rule.
 * {@link com.botmaker.plugin.api.meta.Migrated} marks a managed value such a rule wrote beside its original.
 *
 * <p>{@link com.botmaker.plugin.api.meta.Refactor} is what a host writes on a bot's function when a refactor had
 * to guess there — a site no rule could rewrite, a rule only seeded, a signature change — so the user can find
 * and review it.
 */
package com.botmaker.plugin.api.meta;

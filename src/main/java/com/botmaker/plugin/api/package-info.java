/**
 * The contract a BotMaker Studio plugin compiles against.
 *
 * <h2>What is here, and what is deliberately not</h2>
 * Interfaces and records. No implementation, no Studio types, and no syntax tree: a plugin describes what it
 * offers ({@link com.botmaker.plugin.api.catalog.PaletteCatalog}) and how it edits a value
 * ({@link com.botmaker.plugin.api.slot.SlotEditor}), and hands values back as values — the host writes every
 * character of Java. The host's parser, its project model and its UI internals are all on the other side of
 * this line.
 *
 * <h2>This package, and the ones under it</h2>
 * Here: {@link com.botmaker.plugin.api.StudioPlugin}, which every plugin implements, and
 * {@link com.botmaker.plugin.api.StudioServices} with the facilities it hands back —
 * {@link com.botmaker.plugin.api.Dialogs}, {@link com.botmaker.plugin.api.Theme},
 * {@link com.botmaker.plugin.api.Runs} (with the {@link com.botmaker.plugin.api.TraceLine} it relays),
 * {@link com.botmaker.plugin.api.StyleClasses}. A plugin reads those as one facility, which is why they are not
 * four packages. ({@code Sources}, a find-and-replace over token
 * needles, was the fourth until 2026-09-28: a plugin's names are {@code @Managed} constants now, renamed by
 * binding through {@link com.botmaker.plugin.api.source.PluginValues}.)
 *
 * <p>One package per contribution surface below: {@code catalog} (the palette),
 * {@code slot} (editing one value), {@code toolbar} (a button), {@code source} (a plugin's values in the
 * bot's Java), {@code value} (the value vocabulary), {@code record} (what a recorded gesture writes),
 * {@code palette} (the curation annotations), {@code params} and {@code managed} (the two annotations on a
 * bot's own declarations) and {@code meta} ({@code @ReplacedBy}, {@code @Refactor}). They were all at this
 * root until 2026-09-21, which is how the root became the place a new type landed. {@code parameters}
 * (the Parameters window's row) went to Studio on 2026-09-28.
 *
 * <h2>The one dependency</h2>
 * JavaFX, because a slot editor returns a {@code javafx.scene.Node}. That pins the platform to JavaFX
 * permanently and was chosen over a UI-factory abstraction: the editors worth writing are bespoke, and a
 * factory able to express them would be larger than JavaFX. It is {@code provided}-scoped, so it never
 * reaches a generated bot's classpath.
 *
 * <h2>Compatibility</h2>
 * A bot's source can be rewritten when the SDK changes; a plugin's bytecode cannot be rewritten by anyone.
 * So this module changes far more slowly than the plugins that implement it: new members arrive as
 * {@code default} methods, and a plugin built against an older release keeps working until a Studio
 * <em>major</em> release explicitly refuses it.
 */
package com.botmaker.plugin.api;

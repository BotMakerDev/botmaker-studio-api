/**
 * The contract a BotMaker Studio plugin compiles against.
 *
 * <h2>What is here, and what is deliberately not</h2>
 * Interfaces and records. No implementation, no Studio types, and no syntax tree: a plugin describes what it
 * offers ({@link com.botmaker.plugin.api.catalog.PaletteCatalog}) and how it edits a value
 * ({@link com.botmaker.plugin.api.slot.SlotEditor}), and writes back Java source as text. The host's parser,
 * its project model and its UI internals are all on the other side of this line.
 *
 * <h2>This package, and the ones under it</h2>
 * Here: {@link com.botmaker.plugin.api.StudioPlugin}, which every plugin implements, and
 * {@link com.botmaker.plugin.api.StudioServices} with the facilities it hands back —
 * {@link com.botmaker.plugin.api.Dialogs}, {@link com.botmaker.plugin.api.Theme},
 * {@link com.botmaker.plugin.api.Runs}, {@link com.botmaker.plugin.api.Sources}. A plugin reads those four
 * as one facility, which is why they are not four packages.
 *
 * <p>One package per contribution surface below: {@code catalog} (the palette),
 * {@code slot} (editing one value), {@code parameters} (the Parameters window),
 * {@code toolbar} (a button), {@code source} (the Java a plugin gives a bot), {@code value} (the value
 * vocabulary), {@code palette} (the curation annotations) and {@code meta} ({@code @ReplacedBy}). They were
 * all at this root until 2026-09-21, which is how the root became the place a new type landed.
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

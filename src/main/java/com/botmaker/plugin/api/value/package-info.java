/**
 * The vocabulary a project's variables are written in — and, since it lives here rather than in the SDK, one
 * any plugin may extend.
 *
 * <h2>What moved, and why it moved here</h2>
 *
 * <p>These types were an enum and a pair of {@code switch} statements in {@code botmaker-sdk}, plus a
 * near-duplicate set in the editor: two answers to the question "what does this stored text mean?", in two
 * repositories, kept in step by hand and occasionally not. They are in the contract now because the SDK is
 * <em>plugin #1</em> and gets no back door — a vocabulary only the SDK could add to would make every other
 * plugin a second-class citizen with no way to have a variable of its own type.
 *
 * <h2>The shape of it</h2>
 *
 * <ul>
 *   <li>{@link com.botmaker.plugin.api.value.ValueType} — one kind of value. No longer an enum; identity is
 *       its persisted {@code id}, never object identity, which two plugin classloaders would make useless.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ValueForm} — the type as a variable declares it, as a tree: a
 *       leaf, a container over other forms, or a class the bot itself declares.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ValueContainer} — one composite, registered beside a type. The
 *       contract seeds {@code List}, {@code Map} and {@code Map.Entry} with no privilege over a plugin's
 *       own.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ValueCodec} — what one type's text means, in the plugin's own
 *       terms. Its {@code T} never crosses to the host.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ValueCatalog} — the registry, and the merge that assembles one
 *       vocabulary out of several plugins'.</li>
 *   <li>{@link com.botmaker.plugin.api.value.Visibility}, {@link com.botmaker.plugin.api.value.Range} — the
 *       two declared facts about a variable that are neither its type nor its value.</li>
 * </ul>
 *
 * <h2>Storage is text, and stays text</h2>
 *
 * <p>Every value is a list of strings on disk whatever its type — one entry for an ordinary variable, one per
 * item for a list-shaped one. That is what lets a value survive being retyped, hand-edited, and read by an
 * editor whose plugin for that type is not installed. The cost is that a duration reads as {@code ["90s"]} in
 * a file nobody is expected to open by hand, and it is worth it.
 *
 * <h2>No JSON library</h2>
 *
 * <p>Nothing here carries a Jackson annotation. The contract declares the wire <em>form</em> — an id out, a
 * total factory back — and leaves the choice of parser to whoever owns the file. Putting Jackson in the
 * contract would pin every plugin, forever, to the host's serialisation library.
 */
package com.botmaker.plugin.api.value;

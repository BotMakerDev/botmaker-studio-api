/**
 * The types a project's values may be — and, since this lives here rather than in the SDK, a set any plugin
 * may extend.
 *
 * <h2>Three declarations, and that is the whole package</h2>
 *
 * <ul>
 *   <li>{@link com.botmaker.plugin.api.value.PluginType} — a type this plugin owns: the class it is and what
 *       a fresh one is.</li>
 *   <li>{@link com.botmaker.plugin.api.value.EditableType} — a {@code PluginType} its owner also draws.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ComponentType} — beside it, for a type whose Java is a call:
 *       the components that go in the brackets, as values.</li>
 * </ul>
 *
 * <p>{@code Visibility} stood here until 2026-09-28. It is about a {@code @Param} declaration rather than a
 * type, no plugin read it, and it is Studio's now.
 *
 * <h2>What was here until 2026-09-22, and why none of it is</h2>
 *
 * <p>Seven types: {@code ValueType} (a persisted id and its spellings), {@code ValueCodec} (four string
 * methods per type), {@code ValueCatalog} (the registry and its merge), {@code ValueForm} /
 * {@code ValueContainer} / {@code HostContainers} / {@code SourceSplit} (the grammar), and {@code Range}.
 *
 * <p><b>The codec half was dead.</b> Storage stopped being text when a user parameter became a
 * {@code @Param} field (2026-09-17) and a plugin's values became {@code @Managed} methods (2026-09-21), so
 * {@code parse}, {@code store} and {@code defaultWire} had no caller outside their own plumbing. What was
 * still wired was wrong: a leaf cell round-tripped Java through wire text through
 * {@code literal(parse(java))}, and a {@code java.awt.Color} parameter opened and closed with no edit came
 * back rewritten.
 *
 * <p><b>The grammar was never a plugin's to read.</b> A {@code ValueForm} is how the host walks
 * {@code Map<String, List<Point>>} while writing it out and reading it back; nothing outside the host ever
 * walked one, and now that a value crosses as a <em>value</em>
 * ({@link com.botmaker.plugin.api.slot.ValueContext#value}) nothing outside the host can want to. It is
 * {@code com.botmaker.studio.plugin.grammar} now.
 *
 * <p><b>And four declarations described one type.</b> For a {@code Point} an author wrote a
 * {@code ValueType} with an id, a {@code ValueCodec}, a {@code SourceSeed} carrying the fresh value as Java
 * <em>text</em>, and a {@code SlotEditor} predicate naming the type a third time — in three files, with
 * nothing checking that they agreed. Two of the four were strings the compiler never looked at. One
 * {@link com.botmaker.plugin.api.value.PluginType} is all of it, and javac asks for every method.
 *
 * <h2>No JSON library</h2>
 *
 * <p>Nothing here carries a Jackson annotation, and none is coming. Putting a serialisation library in the
 * contract would pin every plugin to the host's, forever.
 */
package com.botmaker.plugin.api.value;

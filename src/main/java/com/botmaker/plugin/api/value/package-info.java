/**
 * The types a project's values may be — and, since this lives here rather than in the SDK, a set any plugin
 * may extend.
 *
 * <h2>Three declarations, and the steps that write them</h2>
 *
 * <ul>
 *   <li>{@link com.botmaker.plugin.api.value.PluginType} — a type this plugin owns: the class it is and what
 *       a fresh one is.</li>
 *   <li>{@link com.botmaker.plugin.api.value.EditableType} — a {@code PluginType} its owner also draws.</li>
 *   <li>{@link com.botmaker.plugin.api.value.ComponentType} — beside it, for a type whose Java is a call:
 *       the components that go in the brackets, as values.</li>
 * </ul>
 *
 * <p>A plugin declares them with {@link com.botmaker.plugin.api.value.PluginType#value} and
 * {@link com.botmaker.plugin.api.value.ComponentType#part}: {@link
 * com.botmaker.plugin.api.value.TypeSteps} and {@link com.botmaker.plugin.api.value.CallSteps} offer each
 * step only when it is valid, a factory is a method reference ({@link com.botmaker.plugin.api.value.Ref})
 * rather than a name, and an editor is named behind a {@link com.botmaker.plugin.api.value.Drawn} so building
 * the list links no JavaFX. Implementing the interfaces by hand still works.
 *
 * <p><b>No grammar here.</b> How the host walks {@code Map<String, List<Point>>} while writing it out and
 * reading it back is the host's alone: a value crosses as a <em>value</em>
 * ({@link com.botmaker.plugin.api.slot.ValueContext#value}), so nothing outside the host can want to walk it.
 * One {@link com.botmaker.plugin.api.value.PluginType} says all of a type, and javac asks for every method.
 *
 * <h2>No JSON library</h2>
 *
 * <p>Nothing here carries a Jackson annotation, and none is coming. Putting a serialisation library in the
 * contract would pin every plugin to the host's, forever.
 */
package com.botmaker.plugin.api.value;

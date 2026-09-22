/**
 * One row of the Parameters window, as everything that draws or edits a parameter reads it.
 *
 * <p>{@link com.botmaker.plugin.api.parameters.ParameterRow} is the whole package: a name, a
 * {@link com.botmaker.plugin.api.value.ValueForm}, the value as Java source, a prose label, a free-text
 * category, a {@link com.botmaker.plugin.api.value.Visibility}, a declared option set and a
 * {@link com.botmaker.plugin.api.value.Range}. Every component is vocabulary the contract already owns, and
 * the value is text exactly as it is through {@code ValueCodec}, so nothing plugin-specific crosses.
 *
 * <p>It is a final class with a builder rather than a record because a <em>plugin</em> may build one, and a
 * record's canonical constructor is part of its binary signature — see {@code docs/refactor/25-compatibility.md}
 * trap #2.
 *
 * <h2>What is deliberately not here</h2>
 * <b>Any way to declare, store or edit a row through this contract.</b> {@code ParameterDeclaration} and
 * {@code parameterDeclared} went on 2026-09-17; {@code ParameterGroup}, {@code ParameterEdit},
 * {@code StudioPlugin.parameters(String)}, {@code parameterRows(String)} and {@code parameterEdited} went on
 * 2026-09-22. A parameter is a {@code @Param} static field in the bot's own Java, which the host reads and
 * writes off the syntax tree — including one in a file a plugin ships, which the same walk finds with no
 * surface at all. A category is free text the window files a row under, never a vocabulary this package owns.
 */
package com.botmaker.plugin.api.parameters;

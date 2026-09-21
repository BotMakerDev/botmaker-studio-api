/**
 * The Parameters window, as the plugin that owns a value sees it.
 *
 * <p>A {@link com.botmaker.plugin.api.parameters.ParameterGroup} is one section,
 * {@link com.botmaker.plugin.api.parameters.ParameterRow} is one row of it as the owning plugin hands it
 * over, and {@link com.botmaker.plugin.api.parameters.ParameterEdit} is a value the user changed on its way
 * back. Value only: the host draws the window and the row's cell is the same slot editor the canvas uses.
 *
 * <h2>What is deliberately not here</h2>
 * A way to declare a row. {@code ParameterDeclaration} and {@code parameterDeclared} were deleted on
 * 2026-09-17: a plugin declares its own rows in its own code, and a <em>user's</em> parameter is not a row
 * at all — it is a {@code @Param} field in the bot's own Java, which the host reads and writes off the
 * syntax tree. A category is free text the window files a row under, never a vocabulary this package owns.
 */
package com.botmaker.plugin.api.parameters;

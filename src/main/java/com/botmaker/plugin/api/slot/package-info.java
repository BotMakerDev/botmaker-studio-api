/**
 * Editing one value: the editor a plugin contributes, and what the host tells it.
 *
 * <p>{@link com.botmaker.plugin.api.slot.SlotEditor} is the contribution — a predicate and a
 * {@code javafx.scene.Node}. The rest is what it is handed:
 * {@link com.botmaker.plugin.api.slot.ValueContext} where there is no call site (a parameter row, a
 * plugin's own value), {@link com.botmaker.plugin.api.slot.SlotContext} where there is one, and
 * {@link com.botmaker.plugin.api.slot.SlotRun} where several sibling slots are edited as one thing.
 * {@link com.botmaker.plugin.api.slot.TypeRef} is the declared type, as the host resolved it.
 *
 * <h2>What is deliberately not here</h2>
 * A syntax tree. An editor is given the value as the bot's Java writes it — text — and writes text back;
 * it never sees an AST node, a project model or a host window. That is what lets the same editor draw in
 * Studio, in a headless {@code botmaker plugin validate} run and in whatever host comes next.
 */
package com.botmaker.plugin.api.slot;

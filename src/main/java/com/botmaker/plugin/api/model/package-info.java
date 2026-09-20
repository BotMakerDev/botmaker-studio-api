/**
 * How a plugin's model is written into the bot's own Java: as a sequence of calls.
 *
 * <p>{@link com.botmaker.plugin.api.model.ModelCall} describes a call the host may write —
 * {@code Activities.declare(…)} — with the owner, the method and what each argument is.
 * {@link com.botmaker.plugin.api.model.ModelStatement} is one line of a model: which call, and its argument
 * values. {@link com.botmaker.plugin.api.model.MethodRef} is the one argument that is not a value, a
 * reference to a method the user wrote, written {@code Owner::method}.
 *
 * <p>{@link com.botmaker.plugin.api.Models}, off {@code StudioServices}, is the capability that writes and
 * reads them, and its javadoc is where the reasoning lives. The short form: the contract already writes
 * {@code java.util.List.of(a, b, c)} and reads it back
 * ({@link com.botmaker.plugin.api.value.ValueCatalog#initializer}), so a statement is that with a semicolon
 * — the host owns all the syntax, once, and a plugin writes no emitter and no parser.
 *
 * <p>Nothing in this package names a concept belonging to any plugin. A class, a method name and an
 * argument's {@link com.botmaker.plugin.api.value.ValueForm} are facts about Java; what a flow or an
 * activity <em>is</em> stays entirely with whoever owns it.
 */
package com.botmaker.plugin.api.model;

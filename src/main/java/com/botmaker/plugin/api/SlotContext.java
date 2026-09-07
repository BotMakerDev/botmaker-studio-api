package com.botmaker.plugin.api;

import com.botmaker.plugin.api.meta.ReplacedBy;

import java.util.Optional;

/**
 * Everything an editor is told about the slot it is editing, and the one way it writes back.
 *
 * <p>The whole contract crosses as strings. That is not a simplification of a richer model — it is what the
 * host already does: fifteen of its nineteen built-in editors hand back Java source text and let the host
 * re-parse it, and a slot's current contents were already exposed as a {@code String}. So a plugin never
 * sees a syntax tree, the host's parser never becomes plugin surface, and this module depends on no parsing
 * library at all.
 *
 * <p><b>It is a {@link ValueContext} with a call site.</b> The supertype is the half that is true of every
 * value the host edits — a type, the current value, a way to write it back — and this interface adds the
 * half that only source code has. An editor written against {@link ValueContext} therefore works in the
 * Parameters window <em>and</em> here; one written against this interface works only where there is Java to
 * replace. Prefer the supertype unless the call site is genuinely what chooses the editor.
 *
 * <p>{@link #value()} and {@link #set} are inherited and are the same value {@link #currentSource()} spells:
 * a one-element list holding the slot's Java expression. Writing through either is the same edit, and
 * {@link #replaceWith} is the one that can also add imports.
 */
public interface SlotContext extends ValueContext {

    /**
     * The Java source currently in the slot — {@code "new Rect(12, 40, 300, 80)"}, {@code "\"gold.png\""},
     * or empty for a slot never filled in.
     *
     * <p>An editor parses this itself when it wants to open showing the current value. Failing to parse it
     * is normal (the user may have typed anything) and must degrade to a default, never to an exception.
     */
    String currentSource();

    /**
     * The simple name of the type declaring the called method — {@code "Game"} — or empty when the host
     * could not resolve the call.
     *
     * <p>Present because a few editors are chosen by <em>where</em> a value is used rather than by its type:
     * a Steam app id and a window title are both {@code String}. Unresolved is an ordinary state — a bot
     * that does not currently compile still opens in the editor — so an editor keyed on this must answer
     * "not mine" when it is empty rather than assume a name is always there.
     */
    default Optional<String> enclosingClassName() {
        return Optional.ofNullable(enclosingClass());
    }

    /** The name of the called method — {@code "launchSteam"} — or empty when it is unresolved. */
    default Optional<String> enclosingMethodName() {
        return Optional.ofNullable(enclosingMethod());
    }

    /**
     * The simple name of the type declaring the called method — {@code "Game"} — or {@code null} when the
     * host could not resolve the call.
     *
     * @deprecated by {@link #enclosingClassName()}. Unresolved is ordinary, so
     *         {@code ctx.enclosingClass().equals("Game")} — the shape a predicate is written in — throws on
     *         a bot that does not currently compile. Behaviour is unchanged and the host still implements
     *         this one.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.SlotContext#enclosingClassName")
    String enclosingClass();

    /**
     * The name of the called method — {@code "launchSteam"} — or {@code null} when it is unresolved.
     *
     * @deprecated by {@link #enclosingMethodName()}, for the reason {@link #enclosingClass()} is.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.SlotContext#enclosingMethodName")
    String enclosingMethod();

    /** The zero-based position of this slot in the call's argument list, or {@code -1} if it is not an argument. */
    int argIndex();

    /**
     * The Java source of the whole call this slot is an argument of — {@code "Wait.time(Duration.ofSeconds(2))"}
     * — or {@code null} when the slot is not an argument of one.
     *
     * <p>The companion to {@link #replaceEnclosingCall}: an editor that may rewrite the call has to read the
     * <em>other</em> arguments first, since it is about to replace them. Parsing it is the editor's own job
     * and failing to is normal, exactly as for {@link #currentSource()}.
     *
     * @deprecated by {@link #enclosingCall()}. Behaviour is unchanged and the host still implements this one.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.SlotContext#enclosingCall")
    default String enclosingSource() {
        return null;
    }

    /**
     * The Java source of the whole call this slot is an argument of —
     * {@code "Wait.time(Duration.ofSeconds(2))"} — or empty when the slot is not an argument of one.
     *
     * <p>Empty is the answer for a slot that is a receiver, an assignment right-hand side, or anything else
     * that is not an argument — which is most slots — and it is also what an older host answers, since
     * {@link #enclosingSource()} is {@code default}. An editor cannot tell those two apart and does not
     * need to: both mean there is no call to rewrite.
     */
    default Optional<String> enclosingCall() {
        return Optional.ofNullable(enclosingSource());
    }

    /**
     * Replaces the slot's contents with a Java expression, adding any imports it needs.
     *
     * <p>The expression is source text the host re-parses; write it as a user would type it. Name the
     * imports as fully-qualified type names — the host adds the ones that are missing and skips the ones
     * already present, and an import that turns out to be unnecessary is dropped rather than left behind.
     * Passing a fully-qualified expression and no imports is always safe.
     *
     * <p>Call it on the JavaFX application thread. Calling it more than once is fine — each call replaces
     * what the previous one wrote — which is what lets a live editor track a drag.
     *
     * @param javaExpression a Java expression, never a statement and never blank
     * @param importsNeeded  fully-qualified type names the expression refers to by simple name
     */
    void replaceWith(String javaExpression, String... importsNeeded);

    /**
     * Replaces the <em>whole call</em> this slot is an argument of, rather than the slot.
     *
     * <p>For the case where the choice being edited is not a value but a <b>shape</b>: a wait of "somewhere
     * between 800ms and 2s" is not a different duration from a wait of "2s", it is
     * {@code Wait.between(a, b)} where there was {@code Wait.time(x)}. An editor that could only write inside
     * its own slot would have to express that by nesting something in the argument, which is not what the
     * author would have typed.
     *
     * <p>Same rules as {@link #replaceWith}: source text the host re-parses, fully-qualified names are always
     * safe, on the JavaFX application thread, and repeatable. Does nothing when
     * {@link #enclosingSource()} is {@code null} — so an editor may call it without first checking, and gets
     * the honest outcome (the source is left alone) rather than an exception.
     *
     * <p><b>It is a capability, not a vocabulary</b>, which is the test on {@link StudioServices}: the bot's
     * syntax tree is something only the host has, and nothing in the signature names a concept belonging to
     * any one plugin. {@code default} for the reason every method here but {@code id()} is — an older host
     * ignores the call instead of throwing {@code AbstractMethodError}.
     *
     * @param javaExpression a Java expression replacing the whole call, never a statement and never blank
     * @param importsNeeded  fully-qualified type names the expression refers to by simple name
     */
    default void replaceEnclosingCall(String javaExpression, String... importsNeeded) {
    }

    /**
     * The run of sibling slots this one belongs to, or {@code null} when it stands alone.
     *
     * <p>Non-null only where the author writes a value as several arguments of one call — three pictures to
     * match any of — and the host is willing to let one editor rewrite all of them. Every other slot, which
     * is nearly all of them, answers {@code null}, and an editor that never asks is unaffected.
     *
     * <p>A method rather than a second context type, for the reason {@link ValueContext#asSlot()} is one:
     * the question reads as a question in plugin code, and an editor asks it only when it can actually use
     * the answer. {@code default} so an older host answers "no run" instead of throwing.
     *
     * @deprecated by {@link #siblingRun()}. Behaviour is unchanged and the host still implements this one.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.SlotContext#siblingRun")
    default SlotRun run() {
        return null;
    }

    /**
     * The run of sibling slots this one belongs to, or empty when it stands alone.
     *
     * <p>Nearly every slot stands alone, so empty is overwhelmingly the common answer and an editor that
     * never asks is unaffected. It is also what an older host answers, since {@link #run()} is
     * {@code default}.
     */
    default Optional<SlotRun> siblingRun() {
        return Optional.ofNullable(run());
    }

    /**
     * Always {@code this}: a slot is its own call site.
     *
     * @deprecated by {@link ValueContext#slot()}, with the supertype's method. This override answers
     *         {@code this} rather than {@code null} and never had the hazard; it carries the mark so the two
     *         halves of one method deprecate together.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.ValueContext#slot")
    @Override
    default SlotContext asSlot() {
        return this;
    }
}

package com.botmaker.plugin.api.slot;

import java.lang.reflect.Executable;
import java.util.Optional;

/**
 * Everything an editor is told about the slot it is editing, and the one way it writes back.
 *
 * <p>A plugin never sees a syntax tree or writes Java: the value crosses as a value
 * ({@link ValueContext#value}, {@link ValueContext#set}), and what this interface adds is only the
 * call site — which method, resolved, and which argument of it.
 *
 * <p><b>It is a {@link ValueContext} with a call site.</b> The supertype is the half that is true of every
 * value the host edits — a type, the current value, a way to write it back — and this interface adds the
 * half that only source code has. An editor written against {@link ValueContext} therefore works in the
 * Parameters window <em>and</em> here; one written against this interface works only where there is a call
 * site. Prefer the supertype unless the call site is genuinely what chooses the editor.
 */
public interface SlotContext extends ValueContext {

    /**
     * The method or constructor this slot is an argument of — {@code Game.launchSteam(String)} — resolved by
     * the host from the call's binding and loaded on the plugin's own classloader; empty when the call did
     * not resolve, or when the called class is not on the plugin's classpath (a method of the bot itself).
     *
     * <p>Present because a few editors are chosen by <em>where</em> a value is used rather than by its type:
     * a Steam app id and a window title are both {@code String}. Unresolved is an ordinary state — a bot
     * that does not currently compile still opens in the editor — so an editor keyed on this must answer
     * "not mine" when it is empty rather than assume a call is always there.
     *
     * <p><b>An {@code Executable}, not two names</b> (since 0.3.0). {@code enclosingClassName()} and
     * {@code enclosingMethodName()} answered what the source wrote before the dot, so {@code game.launch(…)}
     * on a local named {@code game} was not a call on {@code Game}, and any class called {@code Game} was.
     * The resolved declaration is exact: its declaring class, its overload, whether it is varargs.
     * {@link SlotEditor#calls} is the ordinary way to ask about it.
     */
    Optional<Executable> enclosingExecutable();

    /** The zero-based position of this slot in the call's argument list, or {@code -1} if it is not an argument. */
    int argIndex();

    // enclosingCall() and replaceEnclosingCall(String, String...) were deleted on 2026-09-23. They handed a
    // plugin the whole call as Java text to split and a way to write a new one back; their one user was the
    // SDK's duration picker turning Wait.time(x) into Wait.between(a, b), and that toggle went with them.

    /**
     * The run of sibling slots this one belongs to, or empty when it stands alone.
     *
     * <p>Present only where the author writes a value as several arguments of one call — three pictures to
     * match any of — and the host is willing to let one editor rewrite all of them. Nearly every slot stands
     * alone, so empty is overwhelmingly the common answer and an editor that never asks is unaffected.
     *
     * <p>A method rather than a second context type, for the reason {@link ValueContext#slot()} is one: the
     * question reads as a question in plugin code, and an editor asks it only when it can actually use the
     * answer. {@code default} so a host with no runs answers "no run" without implementing anything.
     */
    default Optional<SlotRun> siblingRun() {
        return Optional.empty();
    }

    /** Always this slot: a slot is its own call site. */
    @Override
    default Optional<SlotContext> slot() {
        return Optional.of(this);
    }
}

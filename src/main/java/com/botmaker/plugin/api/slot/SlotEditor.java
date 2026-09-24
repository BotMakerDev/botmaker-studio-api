package com.botmaker.plugin.api.slot;

import javafx.scene.Node;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A UI for editing one value — a colour swatch instead of {@code 0xFF00AA}, a region picker instead of
 * {@code new Rect(12, 40, 300, 80)}.
 *
 * <p>Two methods, because that is all the host needs: does this editor claim the value, and what does it
 * look like. Everything an editor needs to answer either question is on {@link ValueContext}, and
 * everything it needs to <em>write back</em> is {@link ValueContext#set(Object)}. No syntax tree and no
 * source text crosses this boundary in either direction, which is what keeps the host's grammar out of
 * every plugin.
 *
 * <p><b>Most editors are not declared here at all.</b> A plugin's own type declares its editor beside
 * itself, on {@link com.botmaker.plugin.api.value.PluginType#editor}, so the type is named once. This
 * surface is for the two cases that are not about a type the plugin owns: {@link #forCall}, an editor
 * chosen by the call a slot sits in, and {@link #forType}, an override of the editor for a type another
 * plugin declared.
 *
 * <p><b>It takes a {@link ValueContext}, not a {@link SlotContext}, and that is the whole point of the
 * split.</b> The host edits values in two places — a slot in a bot's Java and a row in the Parameters
 * window — and an editor written against the narrower type could only ever appear in the first. Written
 * against this one, the same editor serves both. An editor that genuinely needs the call site asks
 * {@link ValueContext#slot()} and declines when the answer is empty.
 *
 * <p>{@link #matches(ValueContext)} is called for every value the user opens, so it must be cheap: read the
 * type, maybe ask about the enclosing call, decide. Do the work in {@link #create(ValueContext)}.
 */
public interface SlotEditor {

    /** Whether this editor wants to render the value. */
    boolean matches(ValueContext ctx);

    /**
     * Builds the editor's UI. Called only after {@link #matches} returned {@code true}, and always on the
     * JavaFX application thread.
     */
    Node create(ValueContext ctx);

    /**
     * A small, non-interactive picture of the value in {@code ctx}, or {@code null} for none.
     *
     * <p>The host shows a value in one more place than it edits one: beside a <em>declared choice</em>, in
     * the list an author picks from. There the value is not being edited at all, so {@link #create} is the
     * wrong thing to call — it would hand back a live control in a list of options — and yet plain text is
     * the wrong answer too whenever the stored string is a <b>reference</b> rather than the value. A
     * template name is not a picture and {@code #3A7F2B} is not a colour, so offering the author a gallery
     * to pick a choice from and then listing what they picked as raw text puts the decoding back on the
     * person the choices exist for.
     *
     * <p>The context is read-only: {@link ValueContext#set} does nothing and {@link ValueContext#asSlot()}
     * is {@code null}, because a declared choice has no call site and nothing to write back to. Build a
     * label, a swatch or a thumbnail; do not build anything that expects to be clicked.
     *
     * <p>{@code default null} — which is exactly today's behaviour for every type the host does not answer
     * itself, so an editor that does not implement it costs its type nothing it already had.
     */
    default Node preview(ValueContext ctx) {
        return null;
    }

    /** An editor from two lambdas, for the common case where neither half needs state. */
    static SlotEditor of(Predicate<ValueContext> matches, Function<ValueContext, Node> create) {
        return of(matches, create, null);
    }

    /**
     * An editor for every value of {@code type}, wherever one is edited.
     *
     * <p>The ordinary case, and the one to reach for: an editor chosen by type is drawn in a bot's source
     * <b>and</b> in the Parameters window and beside a {@code @Managed} value, because all three know the
     * type and none of them is a call.
     *
     * <p>A plugin declaring a type of its own does <em>not</em> need this — the editor is a method on its
     * {@link com.botmaker.plugin.api.value.PluginType}, declared once beside the type. This is for the other
     * case: <b>overriding an editor for a type another plugin owns</b>, which is how the SDK offers a
     * colour picker that samples the capture target for a {@code java.awt.Color} that plugin-basics
     * declares. Where two plugins claim one type the host asks the user once and remembers.
     */
    static SlotEditor forType(Class<?> type, Function<ValueContext, Node> create) {
        return forType(type, create, null);
    }

    /** {@link #forType(Class, Function)} with a {@link #preview}. */
    static SlotEditor forType(Class<?> type, Function<ValueContext, Node> create,
                              Function<ValueContext, Node> preview) {
        return of(ctx -> ctx.type().is(type), create, preview);
    }

    /**
     * An editor for argument {@code index} of any call {@code call} accepts — {@code index} {@code -1} for
     * any argument.
     *
     * <p>For values the type cannot tell apart. A Steam app id, a program path, a command-line flag and a
     * window title are all {@code String}, and only the call around a slot says which of them it holds:
     *
     * <pre>{@code
     * SlotEditor.forCall(SlotEditor.calls(Game.class, "launchSteam", "launchSteamIfNotRunning"), 0,
     *         GameEditors::steamAppId)
     * }</pre>
     *
     * <p><b>It declines when there is no call</b>, which is a row of the Parameters window and every
     * {@code @Managed} value — neither has one by construction — and when the host could not resolve the
     * call. Declining is the honest answer, and it is why an editor chosen this way is <b>absent</b> there
     * rather than misfiring. A value that must be editable in both places needs a type-matched editor too,
     * or needs to be a type of its own.
     *
     * <p>The call is the host's resolved {@link SlotContext#enclosingExecutable()}, so a slot on somebody
     * else's {@code Game} is not claimed and a call on a local variable of type {@code Game} is. Until 0.3.0
     * this took the class and the method names as text and matched the class by simple name.
     */
    static SlotEditor forCall(Predicate<Executable> call, int index, Function<ValueContext, Node> create) {
        return of(ctx -> onCall(ctx, call, index), create);
    }

    /**
     * Whether {@code ctx} is argument {@code index} of a call {@code call} accepts — {@code index} {@code -1}
     * for any argument.
     *
     * <p>Public because a plugin whose predicate needs a shape {@link #forCall} does not cover — an index that
     * depends on the overload — should ask the question the same way it does.
     */
    static boolean onCall(ValueContext ctx, Predicate<Executable> call, int index) {
        SlotContext slot = ctx == null ? null : ctx.slot().orElse(null);
        if (slot == null || call == null || (index >= 0 && slot.argIndex() != index)) return false;
        return slot.enclosingExecutable().filter(call).isPresent();
    }

    /**
     * Every method and constructor {@code owner} itself declares — compared by the declaring class's binary
     * name, since the host loads the call on the plugin's loader and a {@code Class} is never compared by
     * identity across one.
     */
    static Predicate<Executable> declaredOn(Class<?> owner) {
        String name = owner.getName();
        return executable -> executable != null && executable.getDeclaringClass().getName().equals(name);
    }

    /**
     * The public methods of {@code owner} named {@code methods}, every overload of each.
     *
     * <p><b>Checked when it is built</b>: a name {@code owner} declares no public method by throws
     * {@link IllegalArgumentException} at once, so a typo or a rename fails the plugin's own tests — and
     * {@code botmaker plugin validate} — instead of leaving an editor that silently never appears.
     */
    static Predicate<Executable> calls(Class<?> owner, String... methods) {
        Set<String> names = new HashSet<>();
        for (String method : methods == null ? new String[0] : methods) {
            boolean declared = false;
            for (Method each : owner.getDeclaredMethods()) {
                if (each.getName().equals(method) && Modifier.isPublic(each.getModifiers())) declared = true;
            }
            if (!declared) {
                throw new IllegalArgumentException(owner.getName() + " declares no public method " + method);
            }
            names.add(method);
        }
        return declaredOn(owner).and(executable -> executable instanceof Method && names.contains(executable.getName()));
    }

    /**
     * An editor from two lambdas plus a {@link #preview}, for a type whose stored form is a reference the
     * author should not have to decode.
     */
    static SlotEditor of(Predicate<ValueContext> matches, Function<ValueContext, Node> create,
                         Function<ValueContext, Node> preview) {
        return new SlotEditor() {
            @Override
            public boolean matches(ValueContext ctx) {
                return matches.test(ctx);
            }

            @Override
            public Node create(ValueContext ctx) {
                return create.apply(ctx);
            }

            @Override
            public Node preview(ValueContext ctx) {
                return preview == null ? null : preview.apply(ctx);
            }
        };
    }
}

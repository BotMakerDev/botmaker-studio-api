package com.botmaker.plugin.api.slot;

import javafx.scene.Node;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A UI for editing one value — a colour swatch instead of {@code 0xFF00AA}, a region picker instead of
 * {@code new Rect(12, 40, 300, 80)}.
 *
 * <p>Two methods, because that is all the host needs: does this editor claim the value, and what does it
 * look like. Everything an editor needs to answer either question is on {@link ValueContext}, and
 * everything it needs to <em>write back</em> is {@link ValueContext#set(java.util.List)} — or, where the
 * value is a slot in Java source, {@link SlotContext#replaceWith(String, String...)}. No syntax tree
 * crosses this boundary in either direction, which is what keeps the host's editor internals out of the
 * contract.
 *
 * <p><b>It takes a {@link ValueContext}, not a {@link SlotContext}, and that is the whole point of the
 * split.</b> The host edits values in two places — a slot in a bot's Java and a row in the Parameters
 * window — and an editor written against the narrower type could only ever appear in the first. Written
 * against this one, the same editor serves both. An editor that genuinely needs the call site (a Steam app
 * id and a window title are both {@code String}) asks {@link ValueContext#asSlot()} and declines when the
 * answer is {@code null}.
 *
 * <p>{@link #matches(ValueContext)} is called for every value the user opens, so it must be cheap: read the
 * type, maybe read the enclosing method name, decide. Do the work in {@link #create(ValueContext)}.
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

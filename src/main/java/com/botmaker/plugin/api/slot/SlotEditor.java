package com.botmaker.plugin.api.slot;

import javafx.scene.Node;

import java.lang.annotation.Annotation;
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
 * itself, on {@link com.botmaker.plugin.api.value.EditableType#editor}, so the type is named once. This
 * surface is for the cases that are not about a type the plugin owns, each one step and then
 * {@link EditorSteps.Drawing#draw}:
 *
 * <pre>{@code
 * SlotEditor.onParameter(SteamAppId.class).draw(() -> LaunchEditors::steamGame)   // chosen by the parameter
 * SlotEditor.forType(Color.class).draw(() -> ColorEditors::color)                 // another plugin's type
 * SlotEditor.when(TemplateEditors::isRunOfPictures).draw(() -> TemplateEditors::group)
 * }</pre>
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
     * <p>The context is read-only: {@link ValueContext#set} does nothing and {@link ValueContext#slot()}
     * is empty, because a declared choice has no call site and nothing to write back to. Build a label, a
     * swatch or a thumbnail; do not build anything that expects to be clicked.
     *
     * <p>{@code default null} — which is exactly today's behaviour for every type the host does not answer
     * itself, so an editor that does not implement it costs its type nothing it already had.
     */
    default Node preview(ValueContext ctx) {
        return null;
    }

    /**
     * An editor for every argument passed to a parameter carrying {@code marker} — the way to tell apart
     * values the type cannot. A Steam app id, a program path and a launch flag are all {@code String}; the
     * plugin says which is which where it declares the parameter:
     *
     * <pre>{@code
     * public static void launchSteam(@SteamAppId String appId)
     * SlotEditor.onParameter(SteamAppId.class).draw(() -> LaunchEditors::steamGame)
     * }</pre>
     *
     * <p>Every argument of a varargs tail is passed to its last parameter, so an annotation there claims each
     * of them. The editor reads the annotation's own elements through {@link SlotContext#parameter()}.
     *
     * <p><b>It declines where there is no call</b> — a row of the Parameters window and every
     * {@code @Managed} value, neither of which has one — and where the host could not resolve the call.
     * A value that must be editable in both places needs a type of its own.
     *
     * <p><b>Checked when it is built</b>: an annotation without {@code @Retention(RUNTIME)}, or whose
     * {@code @Target} excludes parameters, throws {@link IllegalArgumentException}, since no parameter could
     * ever be seen to carry it. Since 2026-09-28; it replaced {@code forCall(calls(Owner.class, "name"), index,
     * …)}, which named methods by string and arguments by position.
     */
    static EditorSteps.Drawing onParameter(Class<? extends Annotation> marker) {
        return EditorSteps.onParameter(marker);
    }

    /**
     * An editor for every value of {@code type}, wherever one is edited.
     *
     * <p>A plugin declaring a type of its own does <em>not</em> need this — the editor is declared once
     * beside the type, on its {@link com.botmaker.plugin.api.value.PluginType}. This is for <b>overriding an
     * editor for a type another plugin owns</b>, which is how the SDK offers a colour picker that samples the
     * capture target for a {@code java.awt.Color} that plugin-basics declares. Where two plugins claim one
     * type the host asks the user once and remembers.
     */
    static EditorSteps.Drawing forType(Class<?> type) {
        return EditorSteps.forType(type);
    }

    /**
     * An editor for every value {@code matches} accepts — for a claim neither a parameter nor a type states,
     * such as a picture that the host says is one of a run of siblings. Prefer the other two: a predicate is
     * the one shape nothing can check.
     */
    static EditorSteps.Drawing when(Predicate<ValueContext> matches) {
        return EditorSteps.when(matches);
    }
}

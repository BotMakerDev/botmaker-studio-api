package com.botmaker.plugin.api.source;

import java.util.List;

/**
 * What a fresh value of one of this plugin's types looks like, written as Java.
 *
 * <p>The host fills an empty slot with {@code new T()}, which is right for most types and uncompilable for
 * three kinds it cannot recognise from the outside: an <b>interface</b> ({@code new CaptureSource()}), a
 * <b>record with required components</b> ({@code new Precision()}), and a type whose meaning lives in a
 * <b>named constant</b> rather than in a number — {@code Precision.DEFAULT} says what the slot is for in a
 * way {@code new Precision(12.0, …)} does not. Until 2026-09-01 the host carried an arm per such type, each
 * naming an SDK class, which is the host holding one plugin's vocabulary on its behalf.
 *
 * <p><b>It is data, not a {@link javafx.scene.Node}, and that is the platform's standing test applied.</b> A
 * bespoke picker cannot be described as a record; "what does a fresh {@code Precision} look like" can, in one
 * line. Describing it rather than drawing it is what lets the host keep owning the seeding path — when to
 * seed, what to do when the text will not parse, which imports to add — for every plugin at once.
 *
 * <h2>Asked at seed time, never cached</h2>
 *
 * <p>The host calls {@link StudioPlugin#sourceSeeds()} each time it needs to fill a slot, so a seed may
 * depend on the project's live state: the SDK's capture-source seed is the project's <em>current</em> default
 * target, and a snapshot taken at load would freeze a slot onto whatever that was when the project opened.
 * Building the list is therefore expected to be cheap.
 *
 * @param typeName   the type this seeds, by simple name ({@code "Precision"}) or fully-qualified name
 *                   ({@code "com.botmaker.sdk.api.vision.Precision"}). The host matches either, because a
 *                   slot's type is routinely known only by the name written in the source.
 * @param expression the Java expression to write. A fully-qualified expression needing no import is always
 *                   safe; the host falls back to its own generic seed if this will not parse.
 * @param imports    fully-qualified type names {@code expression} refers to by simple name. The host adds
 *                   the missing ones and skips the rest, exactly as {@link SlotContext#replaceWith} does.
 */
public record SourceSeed(String typeName, String expression, List<String> imports) {

    public SourceSeed {
        imports = imports == null ? List.of() : List.copyOf(imports);
    }

    /** A seed whose expression is fully qualified and so needs no import. */
    public static SourceSeed of(String typeName, String expression) {
        return new SourceSeed(typeName, expression, List.of());
    }

    /** A seed written with simple names, naming the types it needs imported. */
    public static SourceSeed of(String typeName, String expression, String... imports) {
        return new SourceSeed(typeName, expression, List.of(imports));
    }

    /** Whether this seed answers for a slot whose type is written {@code written} — simple or qualified. */
    public boolean claims(String written) {
        if (written == null || typeName == null) return false;
        return written.equals(typeName)
                || typeName.endsWith("." + written)
                || written.endsWith("." + typeName);
    }
}

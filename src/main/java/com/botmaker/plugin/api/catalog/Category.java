package com.botmaker.plugin.api.catalog;

import java.util.Objects;

/**
 * The group a facade is filed under in the block palette.
 *
 * <p>A record rather than an enum, because a plugin defines its own: a closed set here would mean every new
 * plugin's contributions land in {@code OTHER} until this module is released again, which is exactly the
 * versioning tax the contract exists to avoid.
 *
 * <p><b>Eight constants — {@code VISION}, {@code INTERACTION}, {@code CAPTURE}, {@code LAUNCH},
 * {@code EMULATOR}, {@code GEOMETRY}, {@code BOT}, {@code UTIL} — stood here until 2026-09-07 and are
 * deleted.</b> They were offered so two plugins naming one group would agree on its {@link #id()} rather
 * than on its spelling, and nothing could ever reference them: {@code @Palette(category = …)} takes a
 * {@code String}, so a facade names its group as text and never as a constant. They were plugin #1's menu
 * sitting in the artifact every plugin must agree on for ever. A plugin joining an existing group writes
 * the id, which is what the annotation makes it write anyway.
 *
 * @param id    a stable identifier, compared for equality and never shown
 * @param label the name the user reads
 */
public record Category(String id, String label) {

    public Category {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        if (id.isBlank()) {
            throw new IllegalArgumentException("a category id must not be blank");
        }
    }

    /** A category with the label derived from the id — {@code Category.of("audio")} reads "audio". */
    public static Category of(String id) {
        return new Category(id, id);
    }

    /**
     * A category with an explicit label, falling back to the id when the label is blank.
     *
     * <p>This is the shape a generated catalog uses: an annotation element cannot be left out, so "no label
     * given" arrives as the empty string rather than as an absence, and one overload absorbs both cases.
     */
    public static Category of(String id, String label) {
        return label == null || label.isBlank() ? of(id) : new Category(id, label);
    }
}

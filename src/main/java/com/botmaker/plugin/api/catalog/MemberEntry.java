package com.botmaker.plugin.api.catalog;

import java.util.Objects;

/**
 * One member a plugin offers, and the words the palette shows for it.
 *
 * <p>{@code label} is nullable and usually is: a member's own name is the right label almost every time, and
 * a catalog full of labels restating it is a second name to keep in step. Set one only where the member name
 * genuinely reads badly in a menu.
 *
 * @param id    which member, resolved from the method reference that named it
 * @param label what to show, or {@code null} to show {@link MemberId#name()}
 */
public record MemberEntry(MemberId id, String label) {

    public MemberEntry {
        Objects.requireNonNull(id, "id");
    }

    /** The label if one was given, the member's own name otherwise. Never {@code null}. */
    public String displayLabel() {
        return label != null && !label.isBlank() ? label : id.name();
    }

    public MemberEntry withLabel(String newLabel) {
        return new MemberEntry(id, newLabel);
    }
}

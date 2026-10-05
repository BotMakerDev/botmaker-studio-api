package com.botmaker.plugin.api.catalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One type a plugin catalogues, its category, and the members of it worth proposing — in the order the plugin
 * declared them, which is the order the menu shows.
 *
 * <p>The type is a real {@link Class} rather than a name, and that is load-bearing: it is what lets the editor
 * answer "does this plugin own this simple name?" — the deduction that decides whether {@code Point} in a
 * bot's source means the SDK's or {@code java.awt}'s — without a hand-mirrored list of fully-qualified names.
 *
 * <p><b>Present means curated.</b> A type in the catalog offers exactly the members it lists and nothing
 * else; a type absent from the catalog is not catalogued at all. An entry with an empty member list is
 * therefore a verdict rather than an omission — it is how an enum whose constants are the whole point, or a
 * type reached only as a variable, is catalogued for its identity without proposing any of its methods.
 *
 * <p><b>Catalogued and offered are two bits, not one.</b> Every entry here is catalogued; {@link #offered()}
 * says whether the insert menus also list it — true for a {@code @Palette} class, false for a type one of
 * them reaches.
 *
 * @param type     the class
 * @param category the group it is filed under
 * @param offered  whether the insert menus list it, or whether it is only recognised
 * @param icon     a menu glyph, or {@code null} for the editor's own fallback
 * @param label    what to show, or {@code null} for the class's simple name
 * @param members  the offered members, in declaration order
 */
public record FacadeEntry(Class<?> type, Category category, boolean offered, String icon, String label,
                          List<MemberEntry> members) {

    public FacadeEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        members = List.copyOf(members);
    }

    public String simpleName() {
        return type.getSimpleName();
    }

    public String qualifiedName() {
        return type.getName();
    }

    /** The label if one was given, the class's simple name otherwise. Never {@code null}. */
    public String displayLabel() {
        return label != null && !label.isBlank() ? label : simpleName();
    }

    /**
     * The <b>lead</b> shape of this member name — what picking the name out of a menu inserts.
     *
     * <p>A palette entry is a name, not an overload, so a name with four shapes is one entry: this is the
     * one it offers first and {@link #overloads(String)} is the submenu behind it. The lead is first in
     * {@link #members()} by construction rather than by a flag, which is why nothing here has to be
     * declared twice.
     */
    public Optional<MemberEntry> member(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).findFirst();
    }

    /**
     * Every offered member of this facade with the given name — one per overload, the lead first and the
     * rest in the order the menu should list them behind it.
     */
    public List<MemberEntry> overloads(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).toList();
    }

    public boolean offers(String memberName) {
        return members.stream().anyMatch(m -> m.id().name().equals(memberName));
    }
}

package com.botmaker.plugin.api.value;

import java.util.List;
import java.util.Objects;

/**
 * One kind of value a project variable can hold — {@code DURATION}, {@code POINT}, a plugin's own
 * {@code CHANNEL}. <b>Not an enum any more</b>, which is the whole of this class's reason to exist.
 *
 * <h2>Why it stopped being an enum</h2>
 *
 * <p>It was seventeen constants in the SDK, and a closed set is exactly right for as long as there is one
 * plugin. It is wrong the moment there are two: an enum cannot be extended, so a Discord plugin wanting a
 * {@code Channel} variable would have to be granted a constant in somebody else's enum — a back door of
 * precisely the kind the plugin platform exists to close. The seventeen are still declared in one place; that
 * place is now the SDK's own {@linkplain ValueCatalog catalog}, registered the same way any plugin registers
 * its own, through the same builder, with no privilege the second plugin is denied.
 *
 * <p>What is lost with the enum is the exhaustive {@code switch}, and it was load-bearing in two places (the
 * literal writer and the closed-set test). Both become {@link ValueCatalog} lookups, which is the honest
 * shape: neither could ever have been exhaustive over a set the host does not own.
 *
 * <h2>Identity is the id, and the id is persisted</h2>
 *
 * <p>{@link #id()} is what the project file says and what {@link #equals} compares — never the object, which
 * a plugin classloader would make useless (two loaders, two {@code Channel} types, one file). Renaming an id
 * rewrites every stored project's meaning silently; don't.
 *
 * <h2>Unknown types</h2>
 *
 * <p>{@link #unknown(String)} is what a catalog answers for an id nothing registered — a plugin that is not
 * installed, or was uninstalled after writing the file. It is {@linkplain #known() marked}, holds its raw
 * text, renders read-only and <b>declines to emit</b>. That state was impossible while this was an enum, and
 * is unavoidable now: an open registry is a registry with holes in it. The alternative — dropping the value —
 * destroys a user's data because a jar is missing.
 *
 * <h2>Built, never constructed</h2>
 *
 * <p>Instances come from {@link #of(String)}. There is no public constructor and this is not a record,
 * deliberately: a record's canonical constructor is part of its binary signature, so gaining one component
 * would throw {@code NoSuchMethodError} in every plugin already compiled. A builder can gain a method.
 */
public final class ValueType {

    private final String id;
    private final String label;
    private final String group;
    private final String sourceName;
    private final String boxedName;
    private final boolean primitive;
    private final boolean closedSet;
    private final List<String> options;
    private final boolean bounded;
    private final String importName;
    private final boolean known;

    private ValueType(Builder b, boolean known) {
        this.id = b.id;
        this.label = b.label == null ? b.id : b.label;
        this.group = b.group == null ? "" : b.group;
        this.sourceName = b.sourceName;
        this.boxedName = b.boxedName == null ? b.sourceName : b.boxedName;
        this.primitive = b.primitive;
        this.closedSet = b.closedSet;
        this.options = List.copyOf(b.options);
        this.bounded = b.bounded;
        this.importName = b.importName == null ? "" : b.importName;
        this.known = known;
    }

    /**
     * A builder for the type {@code id} names. The id is the persisted wire form; by convention it is
     * {@code SCREAMING_SNAKE_CASE}, and it must be unique within the catalog that registers it.
     */
    public static Builder of(String id) {
        return new Builder(id);
    }

    /**
     * The placeholder for an id nothing registered. Its value survives untouched, it renders read-only, and
     * it emits nothing — see the class note.
     */
    public static ValueType unknown(String id) {
        String safe = id == null || id.isBlank() ? "?" : id.trim();
        return new ValueType(new Builder(safe).label(safe).source(safe), false);
    }

    /** The stable wire form, as the project file spells it. Persisted — do not change. */
    public String id() {
        return id;
    }

    /** What a menu calls this. Free to change; nothing is stored from it. */
    public String label() {
        return label;
    }

    /**
     * The heading a type picker files this under — {@code "Vision"}, {@code "Geometry"} — or {@code ""} for
     * one that sits at the top level.
     *
     * <p>A free {@code String} and not an enum, for the reason the whole class is not one: a second plugin's
     * types have to be groupable without a constant being granted to it here. Types carrying the same group
     * are shown together, in the order they were registered, and the <em>first</em> registration of a group
     * decides where that group sits — so a plugin cannot reorder another's menu by naming its heading.
     */
    public String group() {
        return group;
    }

    /** How a generator writes this type in source: {@code int}, {@code Key}, {@code java.time.Duration}. */
    public String sourceName() {
        return sourceName;
    }

    /** How it is written inside {@code List<…>} — {@code Integer} for {@code int}. */
    public String boxedName() {
        return boxedName;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    /**
     * The class a generated file must import to write {@link #sourceName()}, or {@code ""} for a primitive
     * and for a type written fully qualified.
     *
     * <p>A name, not a {@code Class<?>}: the host and the plugin that owns this type may be on different
     * classloaders, where an identity comparison silently answers false.
     */
    public String importName() {
        return importName;
    }

    /**
     * The Java type this one <em>is</em>, as a name: {@code int}, {@code java.time.Duration},
     * {@code com.botmaker.sdk.api.interaction.Key}. The {@linkplain #importName() import} when there is one,
     * and the {@linkplain #sourceName() source spelling} otherwise — which covers a primitive and a type
     * written fully qualified, the two cases that need no import.
     *
     * <p>It is what {@link ValueCatalog#forJava(Class)} indexes on, and it is a <b>name</b> for the reason
     * {@link #importName()} is: the plugin that owns this type and the host reading it are on different
     * classloaders, where comparing {@code Class} objects silently answers false.
     *
     * <p>Two registered types may not share one of these — see {@link ValueCatalog.Builder#add}.
     */
    public String javaName() {
        return importName.isEmpty() ? sourceName : importName;
    }

    /**
     * Whether this type's values <em>are</em> a set the editor already shows in full — which is what makes
     * {@link ValueShape#ONE_OF} over it meaningless. "One of yes and no" is a boolean, said twice and worse.
     */
    public boolean isClosedSet() {
        return closedSet;
    }

    /** Whether an author can usefully write down a set of values of this type. */
    public boolean shapeable() {
        return !closedSet;
    }

    /**
     * The values this type brings with it, in the order they should be offered — an enum's own constants.
     * Empty when the set is not the type's to supply, which is every type whose choices the author writes
     * down, and also a {@linkplain #isClosedSet() closed} one whose two states are a single control rather
     * than a list ({@code Yes/No} is a tick box, not a dropdown of two).
     *
     * <p>Each entry is a stored item, in the same spelling {@link ValueCodec#store} produces, so the label a
     * radio button carries and the value it stores are one string rather than two that must agree.
     */
    public List<String> options() {
        return options;
    }

    /**
     * Whether a declared minimum and maximum mean anything here — the numeric types, and nothing else.
     *
     * <p>It is the type's answer rather than the editor's so that the dialog offering a range and the
     * normaliser enforcing one cannot come to disagree about which types have one.
     */
    public boolean bounded() {
        return bounded;
    }

    /**
     * False when this is a {@linkplain #unknown(String) placeholder} for an unregistered id. A generator must
     * check this before emitting a field: there is no source spelling for a type nobody could describe.
     */
    public boolean known() {
        return known;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValueType other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    /** Collects one type's description. Every setter is optional except {@link #source(String)}. */
    public static final class Builder {

        private final String id;
        private String label;
        private String group;
        private String sourceName;
        private String boxedName;
        private boolean primitive;
        private boolean closedSet;
        private List<String> options = List.of();
        private boolean bounded;
        private String importName;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id").trim();
            if (this.id.isEmpty()) throw new IllegalArgumentException("a value type needs an id");
        }

        /** What a menu calls this. Defaults to the id. */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /** The picker heading this type is filed under. Defaults to none, which means the top level. */
        public Builder group(String group) {
            this.group = group;
            return this;
        }

        /** How a generator writes the type in source. Required. */
        public Builder source(String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        /** How it is written inside {@code List<…>}, when that differs — a primitive's box. */
        public Builder boxed(String boxedName) {
            this.boxedName = boxedName;
            return this;
        }

        /** Marks a Java primitive, which changes only what a list of them is written as. */
        public Builder primitive() {
            this.primitive = true;
            return this;
        }

        /** Marks a type whose values are already a set the editor shows in full. */
        public Builder closedSet() {
            this.closedSet = true;
            return this;
        }

        /**
         * The values this type supplies itself — an enum's constants, in declaration order, each spelled the
         * way {@link ValueCodec#store} spells it. Declaring them does <em>not</em> make the type closed; the
         * two questions are separate, and {@link #closedSet()} answers the other one.
         */
        public Builder options(List<String> options) {
            this.options = options == null ? List.of() : List.copyOf(options);
            return this;
        }

        /** Marks a type a declared minimum and maximum apply to — a number. */
        public Builder bounded() {
            this.bounded = true;
            return this;
        }

        /** The class a generated file imports to write {@link #source(String)} by simple name. */
        public Builder importing(String fqn) {
            this.importName = fqn;
            return this;
        }

        public ValueType build() {
            if (sourceName == null || sourceName.isBlank()) {
                throw new IllegalArgumentException("value type " + id + " declares no source spelling");
            }
            return new ValueType(this, true);
        }
    }
}

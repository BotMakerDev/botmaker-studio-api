package com.botmaker.plugin.api;

import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueChoice;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import com.botmaker.plugin.api.value.Visibility;

import java.util.List;
import java.util.Objects;

/**
 * One row of the Parameters window, as the plugin that owns the data hands it over.
 *
 * <p>{@link ParameterGroup} declares the <em>section</em>; this is a value declared inside it. The two
 * surfaces are deliberately different shapes, because they answer different questions: a group is what the
 * plugin decided at build time, and a row is what the project currently holds. A plugin that declares a
 * group and no rows has a heading and an empty section, which is the truthful rendering of a project whose
 * user has declared nothing yet.
 *
 * <h2>Every component is already contract vocabulary</h2>
 *
 * <p>A name, a {@link ValueForm}, the value's Java source, a {@link Visibility}, a declared option set, a
 * {@link Range}, a category out of {@link ParameterGroup#categories()}. Nothing plugin-specific crosses and
 * nothing here is a {@link Class} the host would have to load.
 *
 * <h2>The value is one source string, because a composite has no other canonical form</h2>
 *
 * <p>It was a {@code List<String>} of wires until 2026-09-20 — one entry for an ordinary row, one per item
 * for a list-shaped one — and that list was only ever there to carry the <em>list</em> shape. A
 * {@link ValueForm} says {@code Map<String, List<Duration>>}, which no flat list of wires can encode, so the
 * value crosses as what {@code ValueCatalog.initializer} writes and {@code ValueCatalog.valueOf} reads back:
 * <b>the Java initialiser a field of this form would take</b>. {@code 32-generic-values.md} decision 6.
 *
 * <p>That is not the same as a wire encoding coming back in a different spelling. A wire existed so a
 * <em>bot</em> could read a value with the contract jar absent; nothing at runtime reads a value once the
 * model is compiled code. The source is the artifact, and a value's canonical form is the source that
 * produces it.
 *
 * <h2>Built, never constructed — and the asymmetry with {@link ParameterEdit} is the rule, not an accident</h2>
 *
 * <p>A plugin <b>builds</b> a row and the host <b>reads</b> it, so this is a final class with a builder: a
 * record's canonical constructor is part of its binary signature, and a component added later would throw
 * {@code NoSuchMethodError} in every plugin already compiled against it. {@link ParameterEdit} is the
 * mirror image — the host builds it and a plugin only reads it — so that one <em>is</em> a record and may
 * grow a component safely. Compatibility trap #2 in {@code docs/refactor/25-compatibility.md}, applied in
 * both directions rather than as one blanket ban on records.
 *
 * <h2>No group id here, deliberately</h2>
 *
 * <p>Rows are asked for one group at a time ({@link StudioPlugin#parameterRows(String)}), so a row carrying
 * its own group could only ever agree with the question or contradict it. The group is on
 * {@link ParameterEdit}, where it is the host that has to say which section an edit came from.
 *
 * @see StudioPlugin#parameterRows(String)
 * @see ParameterEdit
 */
public final class ParameterRow {

    /** The heading a row with no category is listed under. Not a real category: nothing declares it. */
    public static final String GENERAL = "General";

    private final String name;
    private final ValueForm form;
    private final ValueChoice type;
    private final String value;
    private final String description;
    private final String category;
    private final Visibility visibility;
    private final List<String> options;
    private final Range bounds;

    private ParameterRow(Builder b) {
        this.name = b.name;
        this.form = b.form == null ? ValueForm.of(ValueType.unknown("")) : b.form;
        this.value = b.value == null ? "" : b.value;
        this.description = b.description == null ? "" : b.description;
        this.category = b.category == null ? "" : b.category.trim();
        this.visibility = b.visibility == null ? Visibility.PUBLIC : b.visibility;
        this.options = List.copyOf(b.options);
        this.bounds = b.bounds == null ? Range.NONE : b.bounds;
        this.type = b.type == null ? this.form.asChoice(!this.options.isEmpty()) : b.type;
    }

    /**
     * A builder for the row {@code name} names, holding a value of {@code form}.
     *
     * @param name the field name the generated class carries for this value, and the name a bot writes down
     *             to read it — a valid Java identifier, unique within its group rather than across the
     *             project
     * @param form what kind of value — a catalogued leaf, or a container over other forms
     */
    public static Builder named(String name, ValueForm form) {
        return new Builder(name, form);
    }

    /**
     * The same, for a caller that still holds a {@link ValueChoice}.
     *
     * <p>The choice is kept as well as its form, so a row built this way answers {@link #type()} exactly what
     * it was given rather than a value derived back out of the form — the bridge must not change what an
     * existing caller stores. It goes with {@code ValueChoice} itself.
     */
    public static Builder named(String name, ValueChoice type) {
        return new Builder(name, type);
    }

    /** The field name, unique within its group. Never blank — the builder refuses one. */
    public String name() {
        return name;
    }

    /** What kind of value: a catalogued leaf, or a container over other forms. */
    public ValueForm form() {
        return form;
    }

    /**
     * What kind of value, as a {@link ValueChoice} — lossy for anything a choice cannot say, which reads as
     * the unknown type. For the surfaces that have not moved to {@link #form()} yet.
     */
    public ValueChoice type() {
        return type;
    }

    /**
     * The value, as the Java initialiser a field of this {@link #form()} takes. Never {@code null}; blank
     * when there is none.
     */
    public String value() {
        return value;
    }

    /** A sentence explaining what the value is for, in the user's words. May be empty. */
    public String description() {
        return description;
    }

    /**
     * The category inside the section this row is filed under — one of the owning
     * {@link ParameterGroup#categories()}, compared the way {@link ParameterGroup#declares(String)} compares
     * it. Blank means uncategorised, which is the ordinary case.
     */
    public String category() {
        return category;
    }

    /** The category this is filed under, or {@link #GENERAL} when it carries none. */
    public String categoryOrGeneral() {
        return category.isBlank() ? GENERAL : category;
    }

    /** Whether whoever runs the bot is offered this row at all. */
    public Visibility visibility() {
        return visibility;
    }

    /** True when whoever runs the bot is offered this row. */
    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }

    /**
     * The set of values this row may take, for a shape that {@link ValueChoice#hasOptions() has one} —
     * each spelled the way the type's codec stores it. Empty for a free value.
     */
    public List<String> options() {
        return options;
    }

    /** The declared minimum and maximum, for a bounded number. {@link Range#NONE} when none is declared. */
    public Range bounds() {
        return bounds;
    }

    /** What an editor calls this — its {@link #description()} when it has one, else its {@link #name()}. */
    public String displayLabel() {
        return description.isBlank() ? name : description;
    }

    /**
     * This row with another value and everything else unchanged — what a plugin answers an edit with once it
     * has stored, and possibly normalised, what the host sent.
     */
    public ParameterRow withValue(String newValue) {
        return toBuilder().value(newValue).build();
    }

    /** A builder holding everything this row holds — the way to change more than one thing at a time. */
    public Builder toBuilder() {
        return new Builder(name, form, type)
                .value(value)
                .description(description)
                .category(category)
                .visibility(visibility)
                .options(options)
                .bounds(bounds);
    }

    /**
     * Equal when every component is. Rows cross a classloader boundary and the host compares what a plugin
     * answered against what it sent, so value equality is what makes "did anything change" answerable at
     * all — object identity would be meaningless here for the same reason it is on {@link ValueType}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterRow other)) return false;
        return name.equals(other.name)
                && form.equals(other.form)
                && value.equals(other.value)
                && description.equals(other.description)
                && category.equals(other.category)
                && visibility == other.visibility
                && options.equals(other.options)
                && bounds.equals(other.bounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, form, value, description, category, visibility, options, bounds);
    }

    @Override
    public String toString() {
        return "ParameterRow[" + name + " : " + form.sourceName() + " = " + value + "]";
    }

    /** Collects a row. Every setter is optional; {@link #named(String, ValueForm)} carries the two that are not. */
    public static final class Builder {

        private final String name;
        private final ValueForm form;
        private final ValueChoice type;
        private String value = "";
        private String description;
        private String category;
        private Visibility visibility;
        private List<String> options = List.of();
        private Range bounds;

        private Builder(String name, ValueForm form) {
            this(name, form, null);
        }

        private Builder(String name, ValueChoice type) {
            this(name, type == null ? null : type.form(), type);
        }

        private Builder(String name, ValueForm form, ValueChoice type) {
            this.name = name == null ? "" : name.trim();
            if (this.name.isEmpty()) throw new IllegalArgumentException("a parameter row needs a name");
            this.form = form;
            this.type = type;
        }

        /** The value, as the Java initialiser a field of this form takes. {@code null} is no value. */
        public Builder value(String value) {
            this.value = value == null ? "" : value;
            return this;
        }

        /** The sentence a user reads instead of the field name. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** The category inside the section, out of the owning group's declared set. */
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        /** Whether the bot's user is offered this. Defaults to {@link Visibility#PUBLIC} — a parameter exists to be set. */
        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        /** The declared set of values, for a shape that has one. */
        public Builder options(List<String> options) {
            this.options = options == null ? List.of() : List.copyOf(options);
            return this;
        }

        /** The declared minimum and maximum, for a bounded number. */
        public Builder bounds(Range bounds) {
            this.bounds = bounds;
            return this;
        }

        public ParameterRow build() {
            return new ParameterRow(this);
        }
    }
}

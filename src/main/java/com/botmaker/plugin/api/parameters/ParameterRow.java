package com.botmaker.plugin.api.parameters;

import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
import com.botmaker.plugin.api.value.Visibility;

import java.util.List;
import java.util.Objects;

/**
 * One row of the Parameters window — one parameter, as everything that draws or edits one reads it.
 *
 * <p><b>What a row comes from is the bot's own Java.</b> A parameter is a {@code @Param} static field the
 * host reads off the syntax tree, and the section it is listed under is the class that declares it. A
 * plugin that wants a row of its own puts a {@code @Param} field in the file it ships, and the same walk
 * finds it — which is why no contribution surface in this package remains.
 *
 * <h2>Every component is already contract vocabulary</h2>
 *
 * <p>A name, a {@link ValueForm}, the value's Java source, a {@link Visibility}, a declared option set, a
 * {@link Range}, and a free-text category. Nothing plugin-specific crosses and nothing here is a
 * {@link Class} the host would have to load.
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
 * <h2>Built, never constructed</h2>
 *
 * <p>A plugin may <b>build</b> a row, so this is a final class with a builder: a record's canonical
 * constructor is part of its binary signature, and a component added later would throw
 * {@code NoSuchMethodError} in every plugin already compiled against it. Compatibility trap #2 in
 * {@code docs/refactor/25-compatibility.md}.
 *
 * <h2>No section here, deliberately</h2>
 *
 * <p>A row does not carry the class it was read out of. Nothing that holds a row holds it alone — the host
 * pairs it with the field it came from, which knows the file, the class and whether the value may be
 * rewritten — so a section component here could only ever repeat that or contradict it.
 */
public final class ParameterRow {

    /** The heading a row with no category is listed under. Not a real category: nothing declares it. */
    public static final String GENERAL = "General";

    private final String name;
    private final ValueForm form;
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

    /** The field name, unique within its group. Never blank — the builder refuses one. */
    public String name() {
        return name;
    }

    /** What kind of value: a catalogued leaf, or a container over other forms. */
    public ValueForm form() {
        return form;
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
     * The category this row is filed under — free text, written by whoever declared the parameter and
     * compared case-insensitively by the window's rail. Blank means uncategorised, the ordinary case.
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
     * The set of values this row may take, each spelled the way the type's codec stores it. Empty for a free
     * value, which is what having a set is asked by: the question belongs to the declaration rather than to
     * the type, which is why a form does not answer it.
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
        return new Builder(name, form)
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
        private String value = "";
        private String description;
        private String category;
        private Visibility visibility;
        private List<String> options = List.of();
        private Range bounds;

        private Builder(String name, ValueForm form) {
            this.name = name == null ? "" : name.trim();
            if (this.name.isEmpty()) throw new IllegalArgumentException("a parameter row needs a name");
            this.form = form;
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

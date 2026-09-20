package com.botmaker.plugin.api.value;

import java.util.List;

/**
 * The type of a value, as a tree: a catalogued leaf, a host container over other forms, or a generic class
 * the bot itself declares.
 *
 * <p>This replaces {@link ValueChoice}, which is a {@link ValueType} plus a {@link ValueShape} and therefore
 * cannot say {@code Map<String, Duration>}, {@code List<List<Point>>}, or anything else with two type
 * arguments or two levels. A bot may perfectly well declare such a field — javac accepts it — and the host
 * read it as unknown and refused to edit it. The design is {@code docs/refactor/32-generic-values.md}.
 *
 * <p><b>Nesting is unbounded here.</b> A host caps what its <em>picker</em> offers, because a four-level
 * value cell is not drawable in a table row; nothing caps what may be read out of a user's file, displayed
 * and left intact. Read-only is a first-class outcome, not a failure path.
 *
 * <h2>Why a sealed interface rather than a record with a nullable container</h2>
 *
 * <p>A {@code Map} has two arguments and a {@code List} one, so arity belongs to the container, and a shape
 * that can be asked for its <em>second</em> argument only when it has one is what keeps a picker and a codec
 * total. Sealing also states the platform rule out loud: these three cases are the host's vocabulary and a
 * plugin may not add a fourth. A plugin adds <b>leaves</b>, through {@link ValueCatalog}, exactly as before —
 * the contract grows capabilities, never vocabularies.
 *
 * <p><b>A record, and frozen as one</b> — see {@link Range} for why a component may never be added.
 */
public sealed interface ValueForm {

    /** One catalogued type: everything a {@link ValueChoice} could say that was not a list. */
    record Leaf(ValueType type) implements ValueForm {

        public Leaf {
            if (type == null) type = ValueType.unknown("");
        }
    }

    /**
     * A host container over other forms. The arity is the container's, and a wrong one is corrected rather
     * than stored, for the same reason {@link ValueChoice} corrects an impossible shape: every reader gets
     * the correction — a file, a fixture, a caller's literal.
     */
    record Of(Container container, List<ValueForm> arguments) implements ValueForm {

        public Of {
            if (container == null) container = Container.LIST;
            arguments = container.fill(arguments);
        }

        /** The element of a {@code List}, or the value of a {@code Map} — the last argument either way. */
        public ValueForm last() {
            return arguments.get(arguments.size() - 1);
        }
    }

    /**
     * A generic class the bot itself declares, by qualified name.
     *
     * <p><b>Not a {@link ValueType}</b>, deliberately. A bot's {@code Box<T>} is not catalogued: nothing
     * registered it, no codec describes it, and a second project's {@code Box} is a different class. What the
     * host knows about it is its qualified name and its type arguments, which is all it needs to write
     * {@code new Box<>(…)} and read it back positionally.
     *
     * @param arguments empty for a non-generic class, which is legal and common
     */
    record Declared(String qualifiedName, List<ValueForm> arguments) implements ValueForm {

        public Declared {
            if (qualifiedName == null) qualifiedName = "";
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }
    }

    /**
     * The containers a host writes and reads.
     *
     * <p>{@code Set} and {@code Optional} are out of scope until something asks for them, and arrays are not
     * a container here at all — they keep the behaviour they have today, read as unknown and shown read-only.
     *
     * <p><b>This enum may grow</b>, like {@link ValueShape} before it, so a {@code switch} over a container
     * needs a {@code default}.
     */
    enum Container {

        /** {@code java.util.List<E>}. */
        LIST(1, "java.util.List", "List of…"),
        /** {@code java.util.Map<K, V>}. */
        MAP(2, "java.util.Map", "Map from… to…");

        private final int arity;
        private final String sourceName;
        private final String label;

        Container(int arity, String sourceName, String label) {
            this.arity = arity;
            this.sourceName = sourceName;
            this.label = label;
        }

        /** How many type arguments this container takes. */
        public int arity() {
            return arity;
        }

        /** The class a generator writes, fully qualified. */
        public String sourceName() {
            return sourceName;
        }

        /** What a menu calls wrapping a form in this container. */
        public String label() {
            return label;
        }

        /** The import a generator needs, which is {@link #sourceName()}. */
        public String importName() {
            return sourceName;
        }

        /**
         * {@code arguments}, padded with unknown leaves or truncated to {@link #arity()}. Total: a caller
         * that supplies the wrong number gets a usable form rather than an exception, and the unknown leaf it
         * is padded with is the state that already means <em>nothing registered this</em>.
         */
        List<ValueForm> fill(List<ValueForm> arguments) {
            List<ValueForm> given = arguments == null ? List.of() : arguments;
            if (given.size() == arity) return List.copyOf(given);
            ValueForm[] filled = new ValueForm[arity];
            for (int i = 0; i < arity; i++) {
                ValueForm argument = i < given.size() ? given.get(i) : null;
                filled[i] = argument == null ? new Leaf(ValueType.unknown("")) : argument;
            }
            return List.of(filled);
        }
    }

    /** One free value of {@code type}. */
    static ValueForm of(ValueType type) {
        return new Leaf(type);
    }

    /** A list of {@code form}. */
    static ValueForm listOf(ValueForm form) {
        return new Of(Container.LIST, List.of(form));
    }

    /** A map from {@code key} to {@code value}. */
    static ValueForm mapOf(ValueForm key, ValueForm value) {
        return new Of(Container.MAP, List.of(key, value));
    }

    /**
     * How a generator writes this type — {@code java.time.Duration}, {@code java.util.Map<String, Point>}.
     *
     * <p>A leaf inside the brackets is spelled the way it is spelled anywhere else, which for a type with an
     * import is its simple name; the import is what {@code ValueCatalog.imports} answers separately. The
     * containers themselves are written fully qualified, exactly as {@link ValueChoice#sourceName()} already
     * writes {@code java.util.List}.
     *
     * <p>Primitives are boxed inside angle brackets and only there, which is the one thing a type argument
     * needs from its leaf that a bare declaration does not.
     */
    default String sourceName() {
        return switch (this) {
            case Leaf leaf -> leaf.type().sourceName();
            case Of of -> of.container().sourceName() + arguments(of.arguments());
            case Declared declared -> declared.qualifiedName()
                    + (declared.arguments().isEmpty() ? "" : arguments(declared.arguments()));
        };
    }

    private static String arguments(List<ValueForm> forms) {
        StringBuilder out = new StringBuilder("<");
        for (int i = 0; i < forms.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(boxed(forms.get(i)));
        }
        return out.append('>').toString();
    }

    private static String boxed(ValueForm form) {
        return form instanceof Leaf leaf ? leaf.type().boxedName() : form.sourceName();
    }

    /**
     * How deep the containers go: {@code 0} for a leaf, {@code 1} for {@code List<Duration>}, {@code 2} for
     * {@code Map<String, List<Point>>}. What a host compares its picker's cap against.
     */
    default int depth() {
        return switch (this) {
            case Leaf ignored -> 0;
            case Of of -> 1 + of.arguments().stream().mapToInt(ValueForm::depth).max().orElse(0);
            case Declared declared -> declared.arguments().stream().mapToInt(ValueForm::depth).max().orElse(0);
        };
    }

    /**
     * Whether every leaf in this form is a type the catalog knows.
     *
     * <p>A form with an unknown leaf anywhere is displayed and never rewritten — one unreadable argument
     * makes the whole form unreadable, the same rule {@code ValueCatalog.valueOfInitializer} already applies
     * to one unreadable item of a list.
     */
    default boolean known() {
        return switch (this) {
            case Leaf leaf -> leaf.type().known();
            case Of of -> of.arguments().stream().allMatch(ValueForm::known);
            case Declared declared -> !declared.qualifiedName().isBlank()
                    && declared.arguments().stream().allMatch(ValueForm::known);
        };
    }

    /**
     * This form as a {@link ValueChoice}, for the surfaces that have not moved yet.
     *
     * <p><b>Lossy on purpose, and one-directional.</b> Anything a {@code ValueChoice} cannot say — a map, a
     * nested container, a declared class — answers the unknown type, which is the state that already means
     * <em>displayed, not edited</em>. The bridge exists so a form can be introduced without moving every
     * caller in one change; it is deleted with {@code ValueChoice} itself.
     *
     * @param hasOptions whether the owner of the declaration wrote the set of values down; the options are
     *                   not part of a form and never were part of a type
     */
    default ValueChoice asChoice(boolean hasOptions) {
        return switch (this) {
            case Leaf leaf -> new ValueChoice(leaf.type(),
                    hasOptions ? ValueShape.ONE_OF : ValueShape.ONE);
            case Of of when of.container() == Container.LIST && of.last() instanceof Leaf leaf ->
                    new ValueChoice(leaf.type(), hasOptions ? ValueShape.ANY_OF : ValueShape.OPEN_LIST);
            default -> ValueChoice.of(ValueType.unknown(sourceName()));
        };
    }
}

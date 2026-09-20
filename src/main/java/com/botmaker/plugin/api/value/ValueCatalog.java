package com.botmaker.plugin.api.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The types a project's variables may be typed with, and what each one's stored text means. What used to be
 * an enum and a {@code switch} in two places.
 *
 * <h2>Open, and therefore holey</h2>
 *
 * <p>A catalog answers for the types it was built with and admits it about the rest. {@link #type(String)} is
 * <b>total</b>: an id nothing registered comes back as {@link ValueType#unknown}, whose value survives, shows
 * read-only, and emits nothing. That is not a degraded case to be minimised — it is the normal state of a
 * project whose plugin is not installed today, and the alternative (dropping the value, or refusing the open)
 * destroys a user's data because a jar is missing.
 *
 * <h2>Merging is how the editor assembles one vocabulary from several plugins</h2>
 *
 * <p>{@link #merge(ValueCatalog)} is left-biased and never throws: the receiver's registrations win an id
 * clash. Refusing would let one plugin's bad id stop the editor from opening any project at all, and
 * last-writer-wins would make the answer depend on jar ordering. Whoever merges should report a clash it
 * dropped; nothing here can, because nothing here knows how to talk to a user.
 *
 * <p>This is deliberately unlike the whole-file collision rule that governs generation, where two claimants
 * is a hard error before a byte is written. The difference is what is at stake: there, refusing costs a
 * regenerate; here, it costs every project.
 */
public final class ValueCatalog {

    /** The id a catalog's fallback text type carries by convention. */
    public static final String TEXT_ID = "TEXT";

    /**
     * The id a catalog's yes/no type carries by convention.
     *
     * <p>Here for the same reason as {@link #TEXT_ID} and for exactly one caller — the SDK's
     * {@code ActivityModel.enabledVariable()}, which builds a
     * {@link ValueChoice} for a flag nobody stored, so it needs an id and has no catalog in hand. A
     * {@link ValueType}'s identity <em>is</em> its id, so naming the id is the whole of what it needs; the
     * label, the group and the Java type it emits stay the registering plugin's, and arrive when a catalog
     * is merged.
     *
     * <p>Two ids and no more. These are the vocabulary's floor — the reading a field with no type has, and
     * the type a switch has — not a place to accumulate a plugin's constants.
     */
    public static final String FLAG_ID = "YES_NO";

    /**
     * The registrations, <b>in registration order</b>.
     *
     * <p>An unmodifiable {@link LinkedHashMap} and deliberately <em>not</em> {@code Map.copyOf}, which is
     * what this was until 2026-08-29. {@code Map.copyOf} produces an immutable map whose iteration order is
     * unspecified <em>and randomised per JVM run</em> — so {@link #types()} answered a different order every
     * time Studio started, contrary to its own javadoc and to {@code ValueWire.registered()}'s. What a user
     * saw was the "what type is this variable" dropdown reshuffling itself between launches, which reads as
     * the application being broken rather than as a bug anybody would report.
     *
     * <p>Found by diffing a generated {@code Parameters} file across two builds of the <em>same</em> source
     * and getting two different files. It had been true since the vocabulary opened, and no single run of
     * anything could have shown it — which is the argument for pinning it with a test rather than trusting
     * that the map type "obviously" preserves order.
     */
    private final Map<String, Entry> byId;

    /**
     * The same registrations, indexed by {@link ValueType#javaName()} — how a plugin author asks for a type
     * without ever writing an id down.
     *
     * <p><b>First registration wins, and nothing is dropped from {@link #byId} to make that true.</b> A
     * second claimant of one Java name is simply not findable this way; it stays findable by its id, so a
     * project that stored values of it still reads them. That is the same judgement as the id clash above —
     * refusing costs every project, and dropping a registration would retype somebody's variable — and it is
     * why this is a merge-time silence rather than a merge-time throw. {@link #javaClashesWith} is how a
     * host reports it. Within <em>one</em> builder it does throw, because there the author is contradicting
     * themselves and there is a compiler-adjacent moment to notice it in.
     */
    private final Map<String, ValueType> byJava;

    /**
     * The composites a value may be built out of, by {@link ValueContainer#id()}, in registration order.
     *
     * <p>Open for the same reason the types are: a plugin's {@code Either<L, R>} is as legitimate as its
     * {@code Channel}, and refusing it would mean every composite anyone ever wants has to be added to the
     * contract by its maintainer. The three the contract seeds are ordinary registrations with no privilege
     * — {@code java.util.List}, {@code java.util.Map} and {@code java.util.Map.Entry} — so a project with no
     * plugin installed still has a list and a map, and the built-ins exercise the same interface a plugin's
     * container will.
     */
    private final Map<String, ValueContainer<?>> containers;

    private ValueCatalog(Map<String, Entry> byId, Map<String, ValueContainer<?>> containers) {
        this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
        Map<String, ValueType> java = new LinkedHashMap<>();
        this.byId.values().forEach(e -> java.putIfAbsent(e.type().javaName(), e.type()));
        this.byJava = Collections.unmodifiableMap(java);
        this.containers = Collections.unmodifiableMap(new LinkedHashMap<>(containers));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A catalog that knows no <em>types</em>. Every type lookup answers unknown; useful as a merge seed and
     * in tests.
     *
     * <p>It still has the three seeded containers, because a list of nothing is still a list: the shape of a
     * field is a separate question from whether anything registered the type inside it, and answering *no
     * such container* for {@code java.util.List} would make an unknown leaf inside a list unreadable twice
     * over.
     */
    public static ValueCatalog empty() {
        return builder().build();
    }

    /**
     * The type {@code id} names — never {@code null}, and {@link ValueType#unknown} when nothing registered
     * it.
     *
     * <p><strong>An absent id is text; a name nobody claimed is unknown.</strong> The two look alike and are
     * not: a {@code null} or blank id is a field older than the vocabulary that has one, and text is the
     * reading it has always had. A name like {@code discord.Channel} is a field whose <em>plugin</em> is
     * missing, and answering text there would retype a user's value because a jar is not installed.
     */
    public ValueType type(String id) {
        if (id == null || id.isBlank()) return text();
        Entry e = byId.get(id.trim());
        return e != null ? e.type() : ValueType.unknown(id);
    }

    /**
     * The catalog's plain-text type, which several fallbacks land on — a legacy {@code CHOICE}, and an editor
     * with nothing better to offer. Unknown when no {@value #TEXT_ID} was registered.
     */
    public ValueType text() {
        return type(TEXT_ID);
    }

    /**
     * The type registered for a Java class — {@code forJava(Duration.class)} answers {@code DURATION}.
     *
     * <p>This is the whole of *"a plugin author says {@code Duration.class} and never writes a
     * {@code ValueType}"*: the id stays the persisted identity, and nobody outside the plugin that registered
     * the type has to know what it is spelled.
     *
     * <p><b>The class is read, never loaded.</b> Only its names are taken off the object the caller already
     * holds — nothing here calls {@code Class.forName}, and nothing here compares {@code Class} objects,
     * which two classloaders make meaningless (rule 2 in this module's {@code CLAUDE.md}). Four spellings are
     * tried, in order, because a registration writes whichever one its generated source needs: the canonical
     * name ({@code java.time.Duration}), the binary name (a nested class, where the canonical has a dot and
     * the binary a {@code $}), the simple name for {@code java.lang} (a type nobody imports, registered as
     * {@code String}), and the primitive a wrapper boxes — so {@code Integer.class} and {@code int.class}
     * find the one type, which is what lets a list of them be asked for the same way a single one is.
     *
     * <p>Empty when nothing registered it, which is an ordinary state: the type belongs to a plugin that is
     * not installed, or to no plugin at all ({@code java.util.Locale}).
     */
    public Optional<ValueType> forJava(Class<?> javaType) {
        if (javaType == null) return Optional.empty();
        for (String name : javaNames(javaType)) {
            ValueType found = byJava.get(name);
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    /** The type registered for a Java type <em>name</em>, exactly as {@link ValueType#javaName()} spells it. */
    public Optional<ValueType> forJava(String javaName) {
        return Optional.ofNullable(javaName == null ? null : byJava.get(javaName.trim()));
    }

    /** The registered containers, in registration order — what a picker offers to wrap a form in. */
    public List<ValueContainer<?>> containers() {
        return List.copyOf(containers.values());
    }

    /** The container {@code id} names, or empty when nothing registered it. */
    public Optional<ValueContainer<?>> container(String id) {
        return Optional.ofNullable(id == null ? null : containers.get(id.trim()));
    }

    /**
     * The container registered for a Java class — {@code containerFor(List.class)} answers {@link
     * ValueContainer#LIST}.
     *
     * <p><b>The class is read, never loaded</b>, exactly as in {@link #forJava(Class)}: only its name is
     * taken off the object the caller already holds. Empty is an ordinary answer and means the field is not
     * a composite this catalog can take apart — a {@code Set} with no plugin contributing one, an array.
     */
    public Optional<ValueContainer<?>> containerFor(Class<?> javaType) {
        if (javaType == null) return Optional.empty();
        String canonical = javaType.getCanonicalName();
        Optional<ValueContainer<?>> found = container(canonical != null ? canonical : javaType.getName());
        return found.isPresent() ? found : container(javaType.getName());
    }

    /** The spellings {@link #forJava(Class)} accepts, in the order it tries them. */
    private static List<String> javaNames(Class<?> type) {
        List<String> names = new ArrayList<>(4);
        String canonical = type.getCanonicalName();
        if (canonical != null) names.add(canonical);
        if (!type.getName().equals(canonical)) names.add(type.getName());
        if (type.getPackage() != null && "java.lang".equals(type.getPackage().getName())) {
            names.add(type.getSimpleName());
        }
        String primitive = UNBOXED.get(type.getName());
        if (primitive != null) names.add(primitive);
        return names;
    }

    /** What each wrapper boxes. A bot asking for {@code Integer.class} means the type {@code int} names. */
    private static final Map<String, String> UNBOXED = Map.of(
            "java.lang.Boolean", "boolean",
            "java.lang.Byte", "byte",
            "java.lang.Character", "char",
            "java.lang.Short", "short",
            "java.lang.Integer", "int",
            "java.lang.Long", "long",
            "java.lang.Float", "float",
            "java.lang.Double", "double");

    /** Whether {@code id} is one this catalog can describe, parse and emit. */
    public boolean knows(String id) {
        return id != null && byId.containsKey(id.trim());
    }

    /** Every registered type, in registration order. */
    public List<ValueType> types() {
        return byId.values().stream().map(Entry::type).toList();
    }

    /** The codec for {@code id}, absent when nothing registered it. */
    public Optional<ValueCodec<?>> codec(String id) {
        Entry e = id == null ? null : byId.get(id.trim());
        return e == null ? Optional.empty() : Optional.of(e.codec());
    }

    /**
     * The initialiser for a field of this type holding this value: a single literal, or
     * {@code java.util.List.of(…)} over one literal per item.
     *
     * <p><b>Empty means "decline"</b>, and the only thing that declines is an unknown type. A generator that
     * gets an empty answer must leave the field out entirely rather than invent one — there is no source
     * spelling for a type nobody could describe, and a wrong guess compiles into the user's bot.
     */
    public Optional<String> initializer(ValueChoice choice, List<String> value) {
        if (choice == null) return Optional.empty();
        Optional<ValueCodec<?>> found = codec(choice.type().id());
        if (found.isEmpty()) return Optional.empty();
        ValueCodec<?> codec = found.get();
        List<String> wires = value == null ? List.of() : value;
        if (!choice.isList()) {
            return initializer(choice.form(), parseOf(codec, wires.isEmpty() ? "" : wires.getFirst()));
        }
        return initializer(choice.form(), wires.stream().map(wire -> parseOf(codec, wire)).toList());
    }

    /**
     * {@link #initializer} read backwards: the stored value a field's initialiser came from, or empty.
     *
     * <p>The shape is composed here and the item is the codec's, exactly as on the way out — so
     * {@code java.util.List.of(a, b)} answers two items for a list-shaped choice, and a codec is written
     * once for both shapes. An item the codec does not recognise makes the <em>whole</em> answer empty:
     * half a list is not a value, and the host must show the source it cannot read rather than a partial
     * reading of it.
     *
     * <p>Empty for an unknown type, for a list-shaped choice whose source is not a {@code List.of(…)} call,
     * and for anything the codec declines. The host then shows the initialiser as written, read-only.
     */
    public Optional<List<String>> valueOfInitializer(ValueChoice choice, String initializer) {
        if (choice == null || initializer == null) return Optional.empty();
        Optional<ValueCodec<?>> found = codec(choice.type().id());
        if (found.isEmpty()) return Optional.empty();
        ValueCodec<?> codec = found.get();
        return valueOf(choice.form(), initializer).map(value -> value instanceof List<?> items
                ? items.stream().map(item -> storeOf(codec, item)).toList()
                : List.of(storeOf(codec, value)));
    }

    /**
     * The stored form a freshly created value of {@code typeId} starts with — {@code ""} for an id nothing
     * registered, which is the only honest seed for a type nobody can describe.
     */
    public String defaultItem(String typeId) {
        return codec(typeId).map(ValueCodec::defaultWire).orElse("");
    }

    /**
     * One stored item, read and written back canonically — {@code store(parse(wire))}. Total, and a fixed
     * point: normalising twice changes nothing, which is what lets the editor show the value the bot will
     * actually get rather than the text somebody happened to type.
     *
     * <p><b>An id nothing registered is returned untouched.</b> That is the {@linkplain ValueType#unknown
     * unknown-type} rule at the one place it costs something: the host cannot canonicalise what it cannot
     * read, and rewriting it to {@code ""} would destroy a value whose plugin is merely not installed today.
     */
    public String normalize(String typeId, String wire) {
        String safe = wire == null ? "" : wire;
        return codec(typeId).map(codec -> canonical(codec, safe)).orElse(safe);
    }

    /**
     * One stored item as Java source, together with the class the file must import to write it — empty when
     * no import is needed, and {@link Optional#empty()} for an id nothing registered.
     *
     * <p>Unlike {@link #initializer}, which composes the shape and writes everything fully qualified for a
     * generated file, this is the single-item form the <em>editor</em> needs when it drops a value into the
     * user's own source, where an import is arranged rather than avoided.
     */
    public Optional<Literal> literal(String typeId, String wire) {
        Optional<ValueCodec<?>> found = codec(typeId);
        if (found.isEmpty()) return Optional.empty();
        return Optional.of(new Literal(render(found.get(), wire == null ? "" : wire),
                type(typeId).importName()));
    }

    /** One item's Java source and the class it needs imported ({@code ""} when it needs none). */
    public record Literal(String source, String importName) {}

    public List<String> imports(ValueChoice choice) {
        if (choice == null) return List.of();
        String fqn = choice.type().importName();
        return fqn.isEmpty() ? List.of() : List.of(fqn);
    }

    // ---- the form-directed grammar -------------------------------------------------------------------------
    //
    // One writer and one reader over the whole type tree, both total. What crosses here is a *live value*,
    // never a wire encoding: a composite's canonical form is its Java initialiser and nothing else, so there
    // is no second spelling to keep in step. See docs/refactor/32-generic-values.md.
    //
    // Three rules carry over from the flat pair these generalise, and matter more with nesting rather than
    // less:
    //
    //   * Empty means decline, and declining is not an error. A guess compiles into a user's bot.
    //   * A partial reading is not a reading. One part a codec refuses empties the *whole* answer, so a map
    //     with one unreadable value is shown whole and untouched rather than silently losing an entry.
    //   * The split is not a parser. It reads back what this wrote; anything else answers empty.

    /**
     * {@code value} written as the Java initialiser a field of type {@code form} would take, or empty when
     * any part of it cannot be written.
     *
     * <p>Everything is written fully qualified, so the result compiles wherever it is placed and
     * {@link #imports(ValueForm)} is advice rather than a requirement.
     */
    public Optional<String> initializer(ValueForm form, Object value) {
        if (form == null) return Optional.empty();
        return switch (form) {
            case ValueForm.Leaf leaf -> codec(leaf.type().id()).map(codec -> literalOf(codec, value));
            case ValueForm.Of of -> {
                Optional<ValueContainer<?>> registered = container(of.container().id());
                if (registered.isEmpty()) yield Optional.empty();
                ValueContainer<?> container = registered.get();
                List<Object> parts = value == null ? List.of() : container.partsOf(value);
                List<ValueForm> forms = container.partForms(of.arguments(), parts.size());
                if (forms.size() != parts.size()) yield Optional.empty();

                StringBuilder out = new StringBuilder(container.factorySource()).append('(');
                for (int i = 0; i < parts.size(); i++) {
                    Optional<String> part = initializer(forms.get(i), parts.get(i));
                    if (part.isEmpty()) yield Optional.empty();
                    if (i > 0) out.append(", ");
                    out.append(part.get());
                }
                yield Optional.of(out.append(')').toString());
            }
            // A class the bot declares is Studio's to write, because only the host can see the bot's own
            // source. The catalog describes what it registered and declines the rest.
            case ValueForm.Declared ignored -> Optional.empty();
        };
    }

    /**
     * {@link #initializer} read backwards: the value {@code initializer} was written from, or empty.
     *
     * <p>Empty for an unknown type, a container nothing registered, a source this grammar did not write, and
     * anything a codec declines. The host then shows the initialiser as it stands, read-only — which is what
     * makes an unbounded type tree safe rather than dangerous.
     */
    public Optional<Object> valueOf(ValueForm form, String initializer) {
        if (form == null || initializer == null) return Optional.empty();
        String source = initializer.strip();
        return switch (form) {
            case ValueForm.Leaf leaf -> codec(leaf.type().id()).flatMap(codec -> valueOfLiteral(codec, source));
            case ValueForm.Of of -> {
                Optional<ValueContainer<?>> registered = container(of.container().id());
                if (registered.isEmpty()) yield Optional.empty();
                ValueContainer<?> container = registered.get();

                Optional<List<String>> split = SourceSplit.arguments(source, container.factorySource());
                if (split.isEmpty()) yield Optional.empty();
                List<String> written = split.get();
                List<ValueForm> forms = container.partForms(of.arguments(), written.size());
                if (forms.size() != written.size()) yield Optional.empty();

                List<Object> parts = new ArrayList<>(written.size());
                for (int i = 0; i < written.size(); i++) {
                    Optional<Object> part = valueOf(forms.get(i), written.get(i));
                    if (part.isEmpty()) yield Optional.empty();
                    parts.add(part.get());
                }
                yield Optional.ofNullable(container.build(parts));
            }
            case ValueForm.Declared ignored -> Optional.empty();
        };
    }

    /**
     * The classes a file writing a value of this form would import, in the order they are first reached.
     *
     * <p>Advice rather than a requirement: {@link #initializer} writes everything fully qualified, so a file
     * that imports none of these still compiles. It is here for a host placing a literal into a user's own
     * source, where an import is arranged rather than avoided.
     */
    public List<String> imports(ValueForm form) {
        Set<String> out = new LinkedHashSet<>();
        collectImports(form, out);
        out.remove("");
        return List.copyOf(out);
    }

    private void collectImports(ValueForm form, Set<String> out) {
        switch (form) {
            case null -> {
            }
            case ValueForm.Leaf leaf -> out.add(leaf.type().importName());
            case ValueForm.Of of -> {
                out.add(of.container().importName());
                of.arguments().forEach(argument -> collectImports(argument, out));
            }
            case ValueForm.Declared declared -> {
                out.add(declared.qualifiedName());
                declared.arguments().forEach(argument -> collectImports(argument, out));
            }
        }
    }

    /**
     * This catalog's registrations, plus {@code other}'s for every id this one does not already claim.
     *
     * <p>Containers merge on the same left-biased rule and for the same reason. The three seeded ones are in
     * both sides of every merge and collide harmlessly, which is what {@code putIfAbsent} is for.
     */
    public ValueCatalog merge(ValueCatalog other) {
        if (other == null || (other.byId.isEmpty() && other.containers.size() <= containers.size())) return this;
        Map<String, Entry> mergedTypes = new LinkedHashMap<>(byId);
        other.byId.forEach(mergedTypes::putIfAbsent);
        Map<String, ValueContainer<?>> mergedContainers = new LinkedHashMap<>(containers);
        other.containers.forEach(mergedContainers::putIfAbsent);
        return new ValueCatalog(mergedTypes, mergedContainers);
    }

    /** The ids {@code other} declares that this catalog already claims — what a merge would drop. */
    public List<String> clashesWith(ValueCatalog other) {
        if (other == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String id : other.byId.keySet()) {
            if (byId.containsKey(id)) out.add(id);
        }
        return List.copyOf(out);
    }

    /**
     * The Java type names {@code other} declares that this catalog already claims — what a merge would leave
     * unreachable by {@link #forJava(Class)}, under a <em>different</em> id from the one that wins.
     *
     * <p>Separate from {@link #clashesWith} because the two are different mistakes. One id claimed twice is
     * two plugins disagreeing about a name they both persist. One Java type claimed twice is two plugins each
     * owning their own id and both wanting to be what {@code Duration.class} means — nothing about either
     * project file is wrong, and only the class lookup is ambiguous.
     */
    public List<String> javaClashesWith(ValueCatalog other) {
        if (other == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, ValueType> e : other.byJava.entrySet()) {
            ValueType mine = byJava.get(e.getKey());
            if (mine != null && !mine.equals(e.getValue())) out.add(e.getKey());
        }
        return List.copyOf(out);
    }

    /** The capture that lets the host call a codec it cannot name the type parameter of. */
    private static <T> String render(ValueCodec<T> codec, String wire) {
        return codec.literal(codec.parse(wire));
    }

    private static <T> String canonical(ValueCodec<T> codec, String wire) {
        return codec.store(codec.parse(wire));
    }

    /**
     * One live value as Java source.
     *
     * <p>The unchecked cast is the "wildcard capture" the module's {@code CLAUDE.md} already describes for
     * {@code literal(parse(wire))}: {@code T} stays inside the plugin and only source text comes back. It is
     * sound because the value reached here came from a form whose leaf names this very codec's type.
     */
    @SuppressWarnings("unchecked")
    private static <T> String literalOf(ValueCodec<T> codec, Object value) {
        return codec.literal((T) value);
    }

    /** One stored item as the live value it names — the wire half, which only the bridges below still use. */
    private static <T> Object parseOf(ValueCodec<T> codec, String wire) {
        return codec.parse(wire);
    }

    /** A live value back to its stored text. Same, in the other direction. */
    @SuppressWarnings("unchecked")
    private static <T> String storeOf(ValueCodec<T> codec, Object value) {
        return codec.store((T) value);
    }

    /**
     * One Java literal back to the live value it was written from.
     *
     * <p><b>Two hops today, one tomorrow.</b> {@code wireOfLiteral} answers the stored text and
     * {@code parse} turns that into the value, so the wire is a way-station inside this method and nowhere
     * else. Phase C of {@code docs/refactor/32-generic-values.md} replaces the pair with a single
     * {@code valueOfLiteral} on the codec and this body becomes one call — which is also the point at which
     * eight SDK types stop having no reader at all.
     */
    private static <T> Optional<Object> valueOfLiteral(ValueCodec<T> codec, String source) {
        return codec.wireOfLiteral(source).map(wire -> (Object) codec.parse(wire));
    }

    /**
     * One registration. A class rather than a record, and reachable only through {@link Builder}: a public
     * record's canonical constructor is part of its binary signature, so gaining a component would throw
     * {@code NoSuchMethodError} in every plugin already compiled against it.
     */
    public static final class Entry {

        private final ValueType type;
        private final ValueCodec<?> codec;

        private Entry(ValueType type, ValueCodec<?> codec) {
            this.type = type;
            this.codec = codec;
        }

        public ValueType type() {
            return type;
        }

        public ValueCodec<?> codec() {
            return codec;
        }
    }

    /** Collects the registrations of one plugin. Registration order is the order a menu offers them in. */
    public static final class Builder {

        private final Map<String, Entry> byId = new LinkedHashMap<>();
        private final Map<String, ValueContainer<?>> containers = new LinkedHashMap<>();

        private Builder() {
            // The contract's own three, seeded into every catalog so a project with no plugin still has a
            // list and a map. They are registered through the same method a plugin uses, which is what keeps
            // the built-ins honest about the interface they define.
            add(ValueContainer.LIST);
            add(ValueContainer.MAP);
            add(ValueContainer.ENTRY);
        }

        /**
         * Registers a composite a value may be built out of.
         *
         * <p>Re-registering an id within one builder is a programming error and throws, exactly as it is for
         * a type. Across plugins it is a merge, and the merge is left-biased and silent.
         */
        public Builder add(ValueContainer<?> container) {
            Objects.requireNonNull(container, "container");
            if (container.arity() < 1) {
                throw new IllegalArgumentException(
                        "container " + container.id() + " must take at least one type argument");
            }
            if (containers.containsKey(container.id())) {
                throw new IllegalArgumentException("container " + container.id() + " is registered twice");
            }
            containers.put(container.id(), container);
            return this;
        }

        /**
         * Registers {@code type} with the codec that reads and writes it.
         *
         * <p>Re-registering an id within one builder is a programming error and throws — unlike a merge
         * across plugins, this is one author contradicting themselves, and there is a compiler-adjacent
         * moment to notice it in.
         *
         * <p><b>Two ids claiming one {@linkplain ValueType#javaName() Java type} throw for the same reason.</b>
         * {@link ValueCatalog#forJava(Class)} is a function only if that cannot happen — it was one in
         * practice across the SDK's seventeen types and nothing made it one by construction, which is exactly
         * the kind of accident that survives until a second registration lands and then answers whichever
         * type happened to be registered first.
         */
        public <T> Builder add(ValueType type, ValueCodec<T> codec) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(codec, "codec");
            if (byId.containsKey(type.id())) {
                throw new IllegalArgumentException("value type " + type.id() + " is registered twice");
            }
            for (Entry existing : byId.values()) {
                if (existing.type().javaName().equals(type.javaName())) {
                    throw new IllegalArgumentException("value types " + existing.type().id() + " and "
                            + type.id() + " both claim the Java type " + type.javaName());
                }
            }
            byId.put(type.id(), new Entry(type, codec));
            return this;
        }

        public ValueCatalog build() {
            return new ValueCatalog(byId, containers);
        }
    }
}

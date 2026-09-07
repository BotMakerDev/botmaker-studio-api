package com.botmaker.plugin.api.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    private ValueCatalog(Map<String, Entry> byId) {
        this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A catalog that knows nothing. Every lookup answers unknown; useful as a merge seed and in tests. */
    public static ValueCatalog empty() {
        return new ValueCatalog(Map.of());
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
        if (!choice.isList()) {
            return Optional.of(render(codec, value == null || value.isEmpty() ? "" : value.getFirst()));
        }
        if (value == null || value.isEmpty()) return Optional.of("java.util.List.of()");
        StringBuilder out = new StringBuilder("java.util.List.of(");
        for (int i = 0; i < value.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(render(codec, value.get(i)));
        }
        return Optional.of(out.append(')').toString());
    }

    /**
     * The classes a field of this type has to import — empty for a primitive, a JDK type written fully
     * qualified, or an unknown type.
     */
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

    /** This catalog's registrations, plus {@code other}'s for every id this one does not already claim. */
    public ValueCatalog merge(ValueCatalog other) {
        if (other == null || other.byId.isEmpty()) return this;
        if (byId.isEmpty()) return other;
        Map<String, Entry> merged = new LinkedHashMap<>(byId);
        other.byId.forEach(merged::putIfAbsent);
        return new ValueCatalog(merged);
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

    /** The capture that lets the host call a codec it cannot name the type parameter of. */
    private static <T> String render(ValueCodec<T> codec, String wire) {
        return codec.literal(codec.parse(wire));
    }

    private static <T> String canonical(ValueCodec<T> codec, String wire) {
        return codec.store(codec.parse(wire));
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

        private Builder() {
        }

        /**
         * Registers {@code type} with the codec that reads and writes it.
         *
         * <p>Re-registering an id within one builder is a programming error and throws — unlike a merge
         * across plugins, this is one author contradicting themselves, and there is a compiler-adjacent
         * moment to notice it in.
         */
        public <T> Builder add(ValueType type, ValueCodec<T> codec) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(codec, "codec");
            if (byId.putIfAbsent(type.id(), new Entry(type, codec)) != null) {
                throw new IllegalArgumentException("value type " + type.id() + " is registered twice");
            }
            return this;
        }

        public ValueCatalog build() {
            return new ValueCatalog(byId);
        }
    }
}

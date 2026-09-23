package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.palette.Hidden;
import com.botmaker.plugin.api.palette.Palette;
import com.botmaker.plugin.api.palette.PaletteDefault;
import com.botmaker.plugin.api.palette.PaletteLabel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a plugin offers the block palette: which types, in which groups, in which order, and which of their
 * members are worth proposing.
 *
 * <h2>How one is built</h2>
 *
 * <p>{@link #scan(Class)} — every class carrying {@link Palette} in the jar the plugin was loaded from:
 *
 * <pre>{@code
 * PaletteCatalog.scan(MyPlugin.class);
 * }</pre>
 *
 * <p>Everything else is read off those classes. Every public method a class declares is offered; the
 * exceptions carry {@link Hidden}, the lead overload of a name carries {@link PaletteDefault} when parameter
 * count does not decide it, and a menu name that should not be the member's own carries
 * {@link PaletteLabel}. <b>Classes and members are discovered, never named</b>, so nothing in a catalog can
 * go stale against a rename, and an annotated class cannot be left out of it.
 *
 * <p>{@link #of(Class[])} catalogues a given list instead, for a host or a test that builds one by hand.
 *
 * <h2>What a catalog does not answer</h2>
 *
 * <p><b>Presence.</b> A catalog describes the build it was created in, not the jar a given bot actually pins.
 * Whether a member exists in that jar is answered by scanning it, and the catalog answers only curation,
 * order and labels.
 *
 * <p>The two compose in the safe direction: the palette is the <em>intersection</em>, so a bot pinned to an
 * old version may occasionally be offered slightly less than that version truly had, and never more. Being
 * offered a member that does not exist is a bot that will not compile; being offered one member fewer is a
 * menu entry somebody types by hand.
 *
 * <h2>A malformed catalog degrades; it never throws</h2>
 *
 * <p>{@link #scan(Class)} and {@link #of(Class[])} collect what they cannot make sense of into
 * {@link #problems()} and build
 * everything else. That is the same rule {@code ValueCatalog.merge} follows and for the same reason: this
 * runs while a project is opening, and a menu missing an entry is recoverable where a project that will not
 * open is not. A host should log {@code problems()} once; a test should assert it is empty.
 */
public record PaletteCatalog(List<FacadeEntry> facades, List<String> problems) {

    private static final PaletteCatalog EMPTY = new PaletteCatalog(List.of(), List.of());

    public PaletteCatalog {
        facades = List.copyOf(facades);
        problems = List.copyOf(problems);
    }

    public PaletteCatalog(List<FacadeEntry> facades) {
        this(facades, List.of());
    }

    /**
     * A catalog that offers nothing — the answer a plugin gives for a version it does not recognise, and the
     * default a plugin that contributes no palette at all inherits.
     *
     * <p>Read it as "this plugin declined to curate", never as "this plugin offers no members": a consumer
     * that sees an empty catalog should fall back to offering everything the jar contains, which is what
     * Studio has done since curation-by-annotation was removed.
     */
    public static PaletteCatalog empty() {
        return EMPTY;
    }

    // ---------------------------------------------------------------- construction

    /**
     * Catalogues every {@link Palette} class in the jar or class directory {@code anchor} was loaded from —
     * normally the plugin's own class, so the palette is exactly what the plugin's sources annotate.
     *
     * <p>Only that one location is read, never the rest of the class path: a class another jar annotates is
     * that jar's plugin's to offer. Class files are searched for the annotation before anything is loaded,
     * and nothing is initialised, so a scan never links a dependency the plugin keeps {@code optional}.
     *
     * <p>A location that cannot be read, and a class that cannot be loaded, are reported in
     * {@link #problems()}; what could be read is still catalogued.
     */
    public static PaletteCatalog scan(Class<?> anchor) {
        List<String> problems = new ArrayList<>();
        PaletteCatalog found = of(PaletteScan.classes(anchor, problems).toArray(Class<?>[]::new));
        problems.addAll(found.problems());
        return new PaletteCatalog(found.facades(), problems);
    }

    /**
     * Catalogues these classes, reading {@link Palette} and the member annotations off each.
     *
     * <p>Facade order is {@link Palette#order()} then simple name, so a group can be laid out without every
     * member of it being renumbered when one is inserted. Member order is the order the class file declares
     * its methods in — source order, as javac writes it — with alphabetical order as the fallback when the
     * class file cannot be read.
     *
     * <p>A class with no {@code @Palette} is reported in {@link #problems()} and skipped, rather than
     * silently ignored: passing one is always a mistake, and it is exactly the mistake — a facade that
     * quietly stops appearing — worth being told about.
     */
    public static PaletteCatalog of(Class<?>... facades) {
        List<String> problems = new ArrayList<>();
        List<FacadeEntry> entries = new ArrayList<>(facades.length);
        Map<String, String> categoryLabels = new HashMap<>();
        Map<Class<?>, Integer> order = new HashMap<>();

        for (Class<?> type : facades) {
            Palette palette = type == null ? null : type.getAnnotation(Palette.class);
            if (palette == null) {
                problems.add((type == null ? "null" : type.getName())
                        + " was catalogued but carries no @Palette");
                continue;
            }
            Category category = category(type, palette, categoryLabels, problems);
            order.put(type, palette.order());
            entries.add(new FacadeEntry(type, category, !type.isAnnotationPresent(Hidden.class),
                    blankToNull(palette.icon()), blankToNull(palette.label()), members(type, problems)));
        }

        entries.sort(Comparator.<FacadeEntry>comparingInt(e -> order.get(e.type()))
                .thenComparing(FacadeEntry::simpleName));
        return new PaletteCatalog(entries, problems);
    }

    /**
     * The category, and the one check that spans two facades: they may disagree about a category's
     * <em>label</em> only by leaving it blank, since a menu group cannot have two names. The first non-blank
     * label wins and the disagreement is reported.
     */
    private static Category category(Class<?> type, Palette palette, Map<String, String> labels,
                                     List<String> problems) {
        String id = palette.category();
        if (id.isBlank()) {
            problems.add(type.getName() + " gives a blank @Palette category");
            id = "other";
        }
        String label = palette.categoryLabel();
        if (!label.isBlank()) {
            String seen = labels.putIfAbsent(id, label);
            if (seen != null && !seen.equals(label)) {
                problems.add(type.getName() + " labels category '" + id + "' as '" + label
                        + "'; another facade already labelled it '" + seen + "'");
                label = seen;
            }
        } else {
            label = labels.getOrDefault(id, "");
        }
        return Category.of(id, label);
    }

    /**
     * Every public method and constructor the class <em>declares</em> and does not hide, grouped by name.
     *
     * <p><b>Overloads are grouped, never dropped.</b> The unit of curation is the member <em>name</em>: every
     * overload of an offered name is offered, adjacent, with the lead shape first and the rest in
     * parameter-count order behind it. {@link FacadeEntry#member(String)} is therefore the shape the menu
     * inserts and {@link FacadeEntry#overloads(String)} the submenu, and neither needs a record component to
     * say so — the lead is simply the first entry for its name. The lead is the narrowest shape unless a
     * {@link PaletteDefault} names another, and a {@link Hidden} on any one overload drops the whole name.
     *
     * <p><b>Declared, not inherited.</b> A public method a class merely inherits belongs to the supertype
     * that declared it, and that type catalogues itself or is not catalogued at all — otherwise one member
     * appears under every subtype's menu with a different owner each time.
     */
    private static List<MemberEntry> members(Class<?> type, List<String> problems) {
        Map<String, List<Member>> byName = new LinkedHashMap<>();
        try {
            for (Method method : type.getDeclaredMethods()) {
                if (eligible(type, method)) {
                    byName.computeIfAbsent(method.getName(), n -> new ArrayList<>()).add(Member.of(method));
                }
            }
            // Constructors are deliberately NOT catalogued, and the reason was measured rather than assumed.
            // Reflecting them added an <init> entry to seven offered facades — Mouse, Keyboard, Wait,
            // ImageFinder, ImageClicker, ImageWaiter, Pixel — every one of them a static facade whose
            // implicit public constructor exists only because nobody wrote a private one. A palette entry
            // inserts a CALL, so an <init> under a static facade is a menu row that cannot be rendered.
            // MemberId keeps its constructor support: a plugin that wants a constructor offered can build
            // the entry itself, and Studio already reads the id. This is the default, not the ceiling.
        } catch (LinkageError e) {
            // A member's signature names a class the plugin's classloader cannot see — an optional
            // dependency the host did not resolve, most often. getDeclaredMethods() is all-or-nothing, so
            // there is no partial answer to salvage: report the facade and offer it with no members rather
            // than taking the whole catalog down with it. Degradation, never a throw: the precedent is
            // ValueCatalog.merge, and the rule behind it is that no malformed catalog may be the reason a
            // project will not open.
            problems.add(type.getName() + ": cannot read its members (" + e + ")");
            return List.of();
        }

        List<MemberEntry> entries = new ArrayList<>();
        for (String name : SourceOrder.arrange(new LinkedHashSet<>(byName.keySet()),
                SourceOrder.methodNames(type))) {
            List<Member> overloads = byName.get(name);
            if (overloads.stream().anyMatch(m -> m.hidden)) {
                if (overloads.stream().anyMatch(m -> m.label != null)) {
                    problems.add(type.getName() + "#" + name + " is @Hidden and also @PaletteLabel'd");
                }
                continue;
            }
            // The lead first, then the rest narrowest-first. An author's @PaletteDefault is the only thing
            // that can beat parameter count, and it settles a whole family in one line.
            long leads = overloads.stream().filter(m -> m.lead).count();
            if (leads > 1) {
                problems.add(type.getName() + "#" + name + " has " + leads + " @PaletteDefault overloads");
            }
            if (overloads.stream().map(m -> m.label).filter(l -> l != null).distinct().count() > 1) {
                problems.add(type.getName() + "#" + name
                        + " labels its overloads differently; a label names the whole family");
            }
            overloads.sort(Comparator.comparingInt((Member m) -> m.lead ? 0 : 1)
                    .thenComparingInt(m -> m.id.parameterTypeNames().size())
                    .thenComparing(m -> m.id.descriptor()));
            String label = overloads.stream().map(m -> m.label).filter(l -> l != null).findFirst().orElse(null);
            for (Member overload : overloads) {
                entries.add(new MemberEntry(overload.id, label));
            }
        }
        return entries;
    }

    /** One reflected member, reduced to the four things curation asks about. */
    private record Member(MemberId id, boolean hidden, boolean lead, String label) {

        static Member of(Method method) {
            return new Member(MemberId.of(method), method.isAnnotationPresent(Hidden.class),
                    method.isAnnotationPresent(PaletteDefault.class), label(method.getAnnotation(PaletteLabel.class)));
        }

        private static String label(PaletteLabel annotation) {
            return annotation == null || annotation.value().isBlank() ? null : annotation.value();
        }
    }

    /**
     * Whether {@link #members} considers this method at all — structural, never editorial. It drops what no
     * author should have to annotate: {@code toString()}, {@code equals(Object)} and {@code hashCode()}; an
     * enum's synthetic {@code values()} and {@code valueOf(String)}; and synthetic or bridge methods.
     */
    private static boolean eligible(Class<?> facade, Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || method.isBridge()) {
            return false;
        }
        if (facade.isEnum() && (method.getName().equals("values") && method.getParameterCount() == 0
                || method.getName().equals("valueOf") && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String.class)) {
            return false;
        }
        return switch (method.getName()) {
            case "toString", "hashCode" -> method.getParameterCount() != 0;
            case "equals" -> !(method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class);
            default -> true;
        };
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    // ---------------------------------------------------------------- queries

    public boolean isEmpty() {
        return facades.isEmpty();
    }

    /** The facade with this fully-qualified name, if it is catalogued. */
    public Optional<FacadeEntry> facade(String qualifiedName) {
        return facades.stream().filter(f -> f.qualifiedName().equals(qualifiedName)).findFirst();
    }

    /**
     * The facade with this simple name, if exactly one is catalogued under it.
     *
     * <p>Empty when two facades share a simple name — the case an editor must not guess at, since the two
     * need different imports.
     */
    public Optional<FacadeEntry> facadeBySimpleName(String simpleName) {
        List<FacadeEntry> matches = facades.stream().filter(f -> f.simpleName().equals(simpleName)).toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public Optional<FacadeEntry> facade(Class<?> type) {
        return facades.stream().filter(f -> f.type().equals(type)).findFirst();
    }

    /** Every catalogued facade in this category, in menu order. */
    public List<FacadeEntry> facadesIn(Category category) {
        return facades.stream().filter(f -> f.category().equals(category)).toList();
    }

    /** The facades the insert menus list, in menu order — everything not {@link Hidden} on its type. */
    public List<FacadeEntry> offeredFacades() {
        return facades.stream().filter(FacadeEntry::offered).toList();
    }

    /** The categories that have at least one facade, in the order their first facade appears. */
    public List<Category> categories() {
        Set<Category> seen = new LinkedHashSet<>();
        facades.forEach(f -> seen.add(f.category()));
        return List.copyOf(seen);
    }

    public boolean offers(Class<?> type) {
        return facade(type).isPresent();
    }

    public boolean offers(Class<?> type, String memberName) {
        return facade(type).filter(f -> f.offers(memberName)).isPresent();
    }

    /**
     * Merges another catalog over this one — later wins on a facade both declare, and its members are
     * appended to the ones already there rather than replacing them.
     *
     * <p>This is how a host composes several plugins into one palette. It is deliberately additive: a plugin
     * curates its own surface and has no business removing another's.
     */
    public PaletteCatalog mergedWith(PaletteCatalog other) {
        if (other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        Map<Class<?>, FacadeEntry> merged = new LinkedHashMap<>();
        facades.forEach(f -> merged.put(f.type(), f));
        for (FacadeEntry incoming : other.facades()) {
            merged.merge(incoming.type(), incoming, PaletteCatalog::mergeFacades);
        }
        List<String> allProblems = new ArrayList<>(problems);
        allProblems.addAll(other.problems());
        return new PaletteCatalog(List.copyOf(merged.values()), allProblems);
    }

    /** Later wins on everything the entry says about itself; members are appended, never replaced. */
    private static FacadeEntry mergeFacades(FacadeEntry existing, FacadeEntry incoming) {
        List<MemberEntry> members = new ArrayList<>(existing.members());
        Set<MemberId> seen = new LinkedHashSet<>();
        existing.members().forEach(m -> seen.add(m.id()));
        for (MemberEntry member : incoming.members()) {
            if (seen.add(member.id())) {
                members.add(member);
            }
        }
        return new FacadeEntry(incoming.type(), incoming.category(), incoming.offered(),
                incoming.icon() != null ? incoming.icon() : existing.icon(),
                incoming.label() != null ? incoming.label() : existing.label(), members);
    }
}

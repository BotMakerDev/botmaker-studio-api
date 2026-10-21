package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.palette.Hidden;
import com.botmaker.plugin.api.palette.Palette;
import com.botmaker.plugin.api.palette.PaletteDefault;
import com.botmaker.plugin.api.palette.PaletteLabel;

import java.util.ArrayList;
import java.util.Comparator;
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
 * <p>{@link #of(Class[])} catalogues classes annotated {@link Palette}. The host calls it with every such
 * class it finds in a plugin's jar, so a plugin names none of them:
 *
 * <pre>{@code
 * PaletteCatalog.of(Mouse.class, Keyboard.class, ImageFinder.class);
 * }</pre>
 *
 * <p>Everything else is read off those classes. What their offered methods take and return from the same
 * jar — {@code Point}, {@code MatchResult} — is catalogued with them, not offered, and never listed. Every
 * public method a class declares is offered; the
 * exceptions carry {@link Hidden}, the lead overload of a name carries {@link PaletteDefault} when parameter
 * count does not decide it, and a menu name that should not be the member's own carries
 * {@link PaletteLabel}. <b>Classes and members are discovered, never named</b>, so nothing in a catalog can
 * go stale against a rename.
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
 * <p>{@link #of(Class[])} collects what it cannot make sense of into {@link #problems()} and builds
 * everything else, because this runs while a project is opening, and a menu missing an entry is recoverable
 * where a project that will not open is not. A host should log {@code problems()} once; a test should assert it is empty.
 */
public record PaletteCatalog(List<FacadeEntry> facades, List<String> problems) {

    private static final PaletteCatalog EMPTY = new PaletteCatalog(List.of(), List.of());

    /** Alphabetical by what the user reads, the qualified name settling a tie. */
    static final Comparator<FacadeEntry> BY_LABEL =
            Comparator.comparing(FacadeEntry::displayLabel, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(FacadeEntry::qualifiedName);

    public PaletteCatalog {
        facades = List.copyOf(facades);
        problems = List.copyOf(problems);
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
     * Offers these classes, reading {@link Palette} and the member annotations off each, and catalogues
     * every type their offered members reach.
     *
     * <p><b>Reach</b> is what an offered member takes or returns — parameter, return and public field types,
     * type arguments and array elements included — plus a reached type's own supertypes, followed through each
     * reached type's members in turn. It stops at the jar: only a public type loaded from the same jar or class
     * directory as the class it was reached from is catalogued, so {@code Duration} and {@code List} never
     * are, and another plugin's type is that plugin's. A {@link Hidden} member reaches nothing. A reached type
     * is filed under the category of the class that reached it first and is not offered.
     *
     * <p>Facades are listed alphabetically by {@link FacadeEntry#displayLabel()}; member order is the order
     * the class file declares its methods in — source order, as javac writes it — with alphabetical order as
     * the fallback when the class file cannot be read.
     *
     * <p>A class with no {@code @Palette} is reported in {@link #problems()} and skipped, rather than
     * silently ignored: passing one is always a mistake, and it is exactly the mistake — a facade that
     * quietly stops appearing — worth being told about.
     */
    public static PaletteCatalog of(Class<?>... facades) {
        return CatalogReader.read(facades);
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

    /** The facades the insert menus list, alphabetically — the {@link Palette} classes, not what they reach. */
    public List<FacadeEntry> offeredFacades() {
        return facades.stream().filter(FacadeEntry::offered).toList();
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
        List<FacadeEntry> sorted = new ArrayList<>(merged.values());
        sorted.sort(BY_LABEL);
        return new PaletteCatalog(sorted, allProblems);
    }

    /**
     * Later wins on everything the entry says about itself, unless only the earlier one offers the type —
     * a class one catalog offers and another merely reaches stays offered as declared. Members are appended,
     * never replaced.
     */
    private static FacadeEntry mergeFacades(FacadeEntry existing, FacadeEntry incoming) {
        List<MemberEntry> members = new ArrayList<>(existing.members());
        Set<MemberId> seen = new LinkedHashSet<>();
        existing.members().forEach(m -> seen.add(m.id()));
        for (MemberEntry member : incoming.members()) {
            if (seen.add(member.id())) {
                members.add(member);
            }
        }
        FacadeEntry winner = existing.offered() && !incoming.offered() ? existing : incoming;
        FacadeEntry other = winner == existing ? incoming : existing;
        return new FacadeEntry(winner.type(), winner.category(), winner.offered(),
                winner.icon() != null ? winner.icon() : other.icon(),
                winner.label() != null ? winner.label() : other.label(), members);
    }
}

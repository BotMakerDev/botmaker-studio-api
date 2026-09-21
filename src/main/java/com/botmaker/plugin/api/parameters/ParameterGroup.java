package com.botmaker.plugin.api.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * One section of the Parameters window, and the generated class its values become fields of.
 *
 * <p>A bot's parameters are the values its user configures — a delay, a colour, a list of hotkeys — and
 * they are <b>project data</b>: the user declares them, not the plugin. What a plugin declares here is the
 * <em>section</em> they are filed under: a heading in the window, an owner for the file they are generated
 * into, and a namespace for their names.
 *
 * <p><b>One window, one section per group — not a window per plugin.</b> A user configuring a bot is
 * configuring one thing; making them hunt through three windows because three plugins are installed would
 * be the plugin architecture leaking into the product. The host renders the sections in the order the
 * plugins are loaded, which puts the default plugin's first.
 *
 * <p><b>The class is whole-file owned.</b> Each group generates exactly one file, written by exactly one
 * plugin; two groups claiming one {@link #className()} is a composition error the host refuses, in the same
 * way it refuses two plugins claiming one value-type id. That ownership is what dissolves the flat
 * namespace parameters used to live in: names are unique within a group, because they are fields of that
 * group's own class, and two plugins may both offer a {@code timeout}.
 *
 * @param id        the group's stable key, held in the project file against every variable filed under it.
 *                  Never shown to a user and never changed once projects exist — it is what tells the host
 *                  which variables are still yours after a rename. The default plugin's is {@code ""}, so
 *                  that every project written before groups existed reads back as its own.
 * @param title     the section heading, in the user's words — {@code "Parameters"}, {@code "Discord"}.
 *                  Blank falls back to the id.
 * @param className the simple name of the generated class, a valid Java identifier — {@code "Parameters"},
 *                  {@code "DiscordParameters"}. It is what a bot writes down to read a value, so it is as
 *                  much API as anything in the palette.
 * @param categories the headings a user may file this group's parameters under, in the order they are shown.
 *                  A <b>second layer inside a section</b>, not a second section: the window still shows one
 *                  section per group, and this is the rail down its left. Blank and duplicate entries are
 *                  dropped and the list is unmodifiable. Empty is the ordinary case — a group with nothing to
 *                  subdivide gets <i>General</i> and nothing else.
 *                  <p>Declared by the plugin rather than typed by the user, for the same reason a tag is:
 *                  a category that comes into existence by being typed produces "Minning" beside "Mining"
 *                  and disappears when its last parameter is refiled. Declaring makes the set finite, so
 *                  every filing UI is a picklist and a category cannot be invented by a typo.
 *                  <p><b>This component was added on 2026-09-02, which is a thing the compatibility rules
 *                  otherwise forbid</b> — growing a record a plugin constructs changes the canonical
 *                  constructor's descriptor. It is allowed because the contract is still in development and
 *                  there is no third-party plugin to break; see the umbrella {@code CLAUDE.md}. The
 *                  {@link #of(String, String)} factory means the SDK's own construction sites did not have
 *                  to move, and it is the one a plugin should use.
 */
public record ParameterGroup(String id, String title, String className, List<String> categories) {

    /** The default plugin's group id. Empty, so a project file written before groups existed reads as it. */
    public static final String DEFAULT_ID = "";

    public ParameterGroup {
        id = id == null ? DEFAULT_ID : id.trim();
        className = className == null ? "" : className.trim();
        if (className.isEmpty()) {
            throw new IllegalArgumentException("a parameter group must name the class it generates");
        }
        if (title == null || title.isBlank()) title = id.isEmpty() ? className : id;
        categories = clean(categories);
    }

    /** A group with no categories of its own — the ordinary case; its heading is its class name. */
    public static ParameterGroup of(String id, String className) {
        return new ParameterGroup(id, className, className, List.of());
    }

    /** The same, with the categories its parameters may be filed under. */
    public static ParameterGroup of(String id, String className, List<String> categories) {
        return new ParameterGroup(id, className, className, categories);
    }

    /**
     * Whether {@code category} is one this group declares, compared the way a user reads it — case-insensitively.
     *
     * <p>Never an identity or an {@code equals} test: the name is round-tripped through the project file, and
     * a user who retypes {@code mining} means the {@code Mining} that is already there.
     */
    public boolean declares(String category) {
        if (category == null || category.isBlank()) return false;
        for (String declared : categories) {
            if (declared.equalsIgnoreCase(category.trim())) return true;
        }
        return false;
    }

    /** Trimmed, blank-free, first-spelling-wins over case, and unmodifiable. */
    private static List<String> clean(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> out = new ArrayList<>(raw.size());
        for (String name : raw) {
            if (name == null) continue;
            String trimmed = name.trim();
            if (trimmed.isEmpty() || !seen.add(trimmed)) continue;
            out.add(trimmed);
        }
        return List.copyOf(out);
    }
}

package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.palette.Hidden;
import com.botmaker.plugin.api.palette.Palette;
import com.botmaker.plugin.api.palette.PaletteDefault;
import com.botmaker.plugin.api.palette.PaletteLabel;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The reflective walk behind {@link PaletteCatalog#of(Class[])}: the offered classes read, then every type their
 * members reach in the same jar, each member grouped by name in source order ({@link SourceOrder}). What it cannot
 * read goes into the catalog's problems; nothing here throws.
 */
final class CatalogReader {

    private CatalogReader() {
    }

    /** The catalog of {@code facades}, as {@link PaletteCatalog#of(Class[])} documents it. */
    static PaletteCatalog read(Class<?>... facades) {
        List<String> problems = new ArrayList<>();
        Map<Class<?>, FacadeEntry> entries = new LinkedHashMap<>();
        Map<String, String> categoryLabels = new HashMap<>();

        for (Class<?> type : facades) {
            Palette palette = type == null ? null : type.getAnnotation(Palette.class);
            if (palette == null) {
                problems.add((type == null ? "null" : type.getName())
                        + " was catalogued but carries no @Palette");
                continue;
            }
            if (entries.containsKey(type)) continue;
            Category category = category(type, palette, categoryLabels, problems);
            entries.put(type, new FacadeEntry(type, category, true,
                    blankToNull(palette.icon()), blankToNull(palette.label()), members(type, problems)));
        }

        Deque<Class<?>> pending = new ArrayDeque<>(entries.keySet());
        while (!pending.isEmpty()) {
            Class<?> from = pending.removeFirst();
            Category category = entries.get(from).category();
            for (Class<?> reached : reach(from)) {
                if (entries.containsKey(reached) || !sameJar(from, reached)) continue;
                entries.put(reached, new FacadeEntry(reached, category, false, null, null,
                        members(reached, problems)));
                pending.addLast(reached);
            }
        }

        List<FacadeEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(PaletteCatalog.BY_LABEL);
        return new PaletteCatalog(sorted, problems);
    }

    /**
     * The public, non-array classes {@code type}'s offered surface names: its supertypes, and the parameter,
     * return and type-argument types of every eligible method whose name is not {@link Hidden}, and the
     * types of its public fields. The jar test is the caller's.
     */
    private static Set<Class<?>> reach(Class<?> type) {
        Set<Class<?>> found = new LinkedHashSet<>();
        try {
            if (type.getSuperclass() != null) collect(type.getGenericSuperclass(), found);
            for (Type face : type.getGenericInterfaces()) collect(face, found);
            Set<String> hidden = new HashSet<>();
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Hidden.class)) hidden.add(method.getName());
            }
            for (Method method : type.getDeclaredMethods()) {
                if (!eligible(type, method) || hidden.contains(method.getName())) continue;
                collect(method.getGenericReturnType(), found);
                for (Type parameter : method.getGenericParameterTypes()) collect(parameter, found);
            }
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && !field.isSynthetic()) {
                    collect(field.getGenericType(), found);
                }
            }
        } catch (LinkageError | MalformedParameterizedTypeException | TypeNotPresentException e) {
            // members() reports the same unreadable class once; reach adds nothing and says nothing more.
            return Set.of();
        }
        found.remove(type);
        found.removeIf(c -> c.isPrimitive() || !Modifier.isPublic(c.getModifiers()) || c.isSynthetic()
                || c.isAnonymousClass() || c.isLocalClass());
        return found;
    }

    /** Every class a generic type names: the raw class, its arguments, an array's element, a wildcard's bounds. */
    private static void collect(Type type, Set<Class<?>> into) {
        switch (type) {
            case Class<?> c -> {
                Class<?> leaf = c;
                while (leaf.isArray()) leaf = leaf.getComponentType();
                into.add(leaf);
            }
            case ParameterizedType p -> {
                collect(p.getRawType(), into);
                for (Type argument : p.getActualTypeArguments()) collect(argument, into);
            }
            case GenericArrayType a -> collect(a.getGenericComponentType(), into);
            case WildcardType w -> {
                for (Type bound : w.getUpperBounds()) collect(bound, into);
                for (Type bound : w.getLowerBounds()) collect(bound, into);
            }
            default -> {
                // A type variable says nothing about which class a call hands over.
            }
        }
    }

    /**
     * Whether both classes were loaded from one jar or class directory. A class with no location — the
     * JDK's, loaded by the platform — is never in a plugin's jar.
     */
    private static boolean sameJar(Class<?> a, Class<?> b) {
        URL left = location(a);
        return left != null && left.equals(location(b));
    }

    private static URL location(Class<?> type) {
        try {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            return source == null ? null : source.getLocation();
        } catch (SecurityException e) {
            return null;
        }
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
            // than taking the whole catalog down with it. Degradation, never a throw: no malformed catalog may
            // be the reason a project will not open.
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
}

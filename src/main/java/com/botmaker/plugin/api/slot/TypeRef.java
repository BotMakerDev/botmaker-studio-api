package com.botmaker.plugin.api.slot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The declared type of a value slot, as the host resolved it.
 *
 * <p>An interface rather than a {@link Class}, because the host resolves a type out of the <em>bot's</em>
 * classpath, not its own: the class an editor would be handed may be a different version of itself, or may
 * not be loadable in the host's JVM at all. So the host answers questions about the type, and every question
 * takes the editor's own {@code Class} and compares by <b>binary name</b> — the comparison that is actually
 * true across two classloaders.
 *
 * <p><b>No name crosses as text</b> (since 0.3.0). {@code simpleName()}, {@code qualifiedName()} and
 * {@code isNamed(String)} were deleted: every editor that used them compared a spelling — {@code "Duration"}
 * or {@code "java.time.Duration"}, either accepted — and a bot declaring a {@code Duration} of its own was
 * claimed by the SDK's wait editor. A question that takes a class cannot be asked about the wrong one.
 * {@link #displayName()} is for a label and nothing else.
 *
 * <p>An <b>unresolved</b> type — an unresolved import, a pom mid-edit — answers {@code false} to both
 * questions. The host no longer guesses a type from how its name is written; an editor keyed on a type is
 * absent there, and the value is still shown as written.
 */
public interface TypeRef {

    /** Whether the slot's type is exactly {@code type}, compared by binary name. */
    boolean is(Class<?> type);

    /**
     * Whether the slot's type is {@code type} or extends or implements it, compared by binary name — for an
     * editor that serves every implementation of an interface it owns.
     */
    boolean isSubtypeOf(Class<?> type);

    /** Whether the host resolved the type at all. */
    boolean isResolved();

    /** {@code Rect}, for a label. Never {@code null}, never compared: ask {@link #is} instead. */
    String displayName();

    /**
     * The type of a value whose class is known — what a host answers for a value it read and what a test
     * hands an editor. Its supertypes are read off {@code type} itself, so {@link #isSubtypeOf} agrees with
     * {@link Class#isAssignableFrom} for every class on one loader.
     */
    static TypeRef of(Class<?> type) {
        if (type == null) return unresolved("");
        Set<String> supertypes = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            Class<?> next = pending.removeFirst();
            if (!supertypes.add(next.getName())) continue;
            if (next.getSuperclass() != null) pending.add(next.getSuperclass());
            pending.addAll(List.of(next.getInterfaces()));
        }
        if (!type.isPrimitive()) supertypes.add(Object.class.getName());
        String name = type.getName();
        String display = type.getSimpleName();
        return new TypeRef() {
            @Override public boolean is(Class<?> other) {
                return other != null && name.equals(other.getName());
            }

            @Override public boolean isSubtypeOf(Class<?> other) {
                return other != null && supertypes.contains(other.getName());
            }

            @Override public boolean isResolved() {
                return true;
            }

            @Override public String displayName() {
                return display;
            }
        };
    }

    /** A type the host could not resolve, shown as {@code displayName}; it is no class at all. */
    static TypeRef unresolved(String displayName) {
        String display = displayName == null ? "" : displayName;
        return new TypeRef() {
            @Override public boolean is(Class<?> other) {
                return false;
            }

            @Override public boolean isSubtypeOf(Class<?> other) {
                return false;
            }

            @Override public boolean isResolved() {
                return false;
            }

            @Override public String displayName() {
                return display;
            }
        };
    }
}

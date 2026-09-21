package com.botmaker.plugin.api.slot;

/**
 * The declared type of a value slot, as the host resolved it.
 *
 * <p>An interface rather than a {@link Class}, because the host resolves a type out of the <em>bot's</em>
 * classpath, not its own: the class an editor would be handed may be a different version of itself, or may
 * not be loadable in the host's JVM at all. So the type crosses as names, and {@link #is(Class)} compares
 * by fully-qualified name — which is the comparison that is actually true across two classloaders.
 *
 * <p>{@link #qualifiedName()} may be empty when the host could not resolve the type (an unresolved import,
 * a project that does not compile). {@link #simpleName()} is still populated in that case, and an editor
 * that keys off a simple name alone still fires — deliberately, since refusing to edit a value because the
 * rest of the file is broken is the worse failure.
 */
public interface TypeRef {

    /** {@code Rect}. Never {@code null}; may be empty for a slot whose type the host could not name at all. */
    String simpleName();

    /** {@code com.botmaker.sdk.api.geometry.Rect}, or empty when the type could not be resolved. */
    String qualifiedName();

    /** Whether the slot's type is this class, compared by fully-qualified name. */
    default boolean is(Class<?> type) {
        return type.getName().equals(qualifiedName());
    }

    /**
     * Whether the slot's type is named this, accepting either spelling — a fully-qualified name is matched
     * against {@link #qualifiedName()}, a bare one against {@link #simpleName()}.
     */
    default boolean isNamed(String name) {
        return name.indexOf('.') >= 0 ? name.equals(qualifiedName()) : name.equals(simpleName());
    }

    /** Whether the type resolved at all. */
    default boolean isResolved() {
        return !qualifiedName().isEmpty();
    }
}

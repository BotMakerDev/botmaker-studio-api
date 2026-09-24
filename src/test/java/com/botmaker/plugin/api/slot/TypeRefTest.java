package com.botmaker.plugin.api.slot;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.TemporalAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two questions a {@link TypeRef} answers, and the one type that answers neither.
 *
 * <p>Every question takes a class, so the case worth pinning is the one a name got wrong: a class of the same
 * simple name in another package is a different type.
 */
class TypeRefTest {

    /** A bot's own {@code Duration}: what {@code isNamed("Duration")} used to claim for the SDK's wait editor. */
    static final class Duration {}

    @Test
    void aTypeIsItsOwnClassAndNoNamesake() {
        TypeRef wait = TypeRef.of(java.time.Duration.class);

        assertTrue(wait.is(java.time.Duration.class));
        assertFalse(wait.is(Duration.class), "same simple name, another class");
        assertEquals("Duration", wait.displayName());
    }

    @Test
    void aSubtypeAnswersForEverySupertypeAndInterface() {
        TypeRef wait = TypeRef.of(java.time.Duration.class);

        assertTrue(wait.isSubtypeOf(java.time.Duration.class), "a type is a subtype of itself");
        assertTrue(wait.isSubtypeOf(TemporalAmount.class));
        assertTrue(wait.isSubtypeOf(Serializable.class));
        assertTrue(wait.isSubtypeOf(Object.class));
        assertFalse(wait.isSubtypeOf(CharSequence.class));
        assertFalse(wait.is(TemporalAmount.class), "is() is exact");
    }

    @Test
    void aPrimitiveIsItselfAndNothingElse() {
        TypeRef count = TypeRef.of(int.class);

        assertTrue(count.is(int.class));
        assertFalse(count.is(Integer.class));
        assertFalse(count.isSubtypeOf(Object.class));
    }

    @Test
    void anUnresolvedTypeIsNoClassAtAll() {
        TypeRef unknown = TypeRef.unresolved("Mystery");

        assertFalse(unknown.isResolved());
        assertFalse(unknown.is(Object.class));
        assertFalse(unknown.isSubtypeOf(Object.class));
        assertEquals("Mystery", unknown.displayName());
    }
}

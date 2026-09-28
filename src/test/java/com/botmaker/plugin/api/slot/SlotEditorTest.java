package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Executable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which slot an editor claims.
 *
 * <p>The property worth asserting is the one a plugin author gets wrong: an editor chosen by the parameter is
 * <b>absent</b> from the Parameters window and from a {@code @Managed} value, because neither has a call
 * site by construction. Declining there is the honest answer, and it is why a value that must be editable
 * in both places needs a type of its own.
 */
class SlotEditorTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface AppId {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Flag {}

    /** Dropped by javac from reflection: no parameter is ever seen to carry it. */
    @Retention(RetentionPolicy.CLASS)
    public @interface Invisible {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethods {}

    public static class Game {
        public static void launchSteam(@AppId String id) {}
        public static void launchSteamIfNotRunning(@AppId String id, Object source) {}
        public static void launchWindow(String title) {}
        public static void launch(String path, @Flag String... flags) {}
    }

    private static final TypeRef STRING = TypeRef.of(String.class);

    /** A Parameters row, or a {@code @Managed} value: a value with no call site anywhere. */
    private static ValueContext row() {
        return new ValueContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"570\""; }
            @Override public StudioServices services() { return null; }
        };
    }

    /** Argument {@code index} of {@code owner.method(…)}, resolved; {@code null} for a call that did not. */
    private static SlotContext call(Class<?> owner, String method, int index) {
        Executable resolved = owner == null ? null : methodOf(owner, method);
        return new SlotContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"570\""; }
            @Override public StudioServices services() { return null; }
            @Override public Optional<Executable> enclosingExecutable() { return Optional.ofNullable(resolved); }
            @Override public int argIndex() { return index; }
        };
    }

    private static Executable methodOf(Class<?> owner, String name) {
        for (var method : owner.getDeclaredMethods()) {
            if (method.getName().equals(name)) return method;
        }
        throw new AssertionError(owner + " has no " + name);
    }

    private static final SlotEditor STEAM_APP_ID = SlotEditor.onParameter(AppId.class).draw(() -> ctx -> null);

    @Test
    void itClaimsTheArgumentsPassedToAnAnnotatedParameter() {
        assertTrue(STEAM_APP_ID.matches(call(Game.class, "launchSteam", 0)));
        assertTrue(STEAM_APP_ID.matches(call(Game.class, "launchSteamIfNotRunning", 0)));
    }

    /** The whole point: same type, different parameter, different editor. */
    @Test
    void itDeclinesAnotherParameterAndAnotherMethod() {
        assertFalse(STEAM_APP_ID.matches(call(Game.class, "launchSteamIfNotRunning", 1)));
        assertFalse(STEAM_APP_ID.matches(call(Game.class, "launchWindow", 0)));
    }

    /** Every argument of a varargs tail is passed to the last parameter, however many there are. */
    @Test
    void everyArgumentOfAVarargsTailIsClaimed() {
        SlotEditor flag = SlotEditor.onParameter(Flag.class).draw(() -> ctx -> null);

        assertFalse(flag.matches(call(Game.class, "launch", 0)));
        assertTrue(flag.matches(call(Game.class, "launch", 1)));
        assertTrue(flag.matches(call(Game.class, "launch", 3)));
        assertTrue(call(Game.class, "launch", 3).parameter().orElseThrow().isVarArgs());
    }

    /** A call the host could not resolve is no call: the editor is absent rather than guessing. */
    @Test
    void anUnresolvedCallIsNotClaimed() {
        assertFalse(STEAM_APP_ID.matches(call(null, null, 0)));
        assertFalse(call(null, null, 0).parameter().isPresent());
    }

    /** An argument past the end of a fixed-arity call has no parameter. */
    @Test
    void anIndexPastAFixedCallHasNoParameter() {
        assertFalse(call(Game.class, "launchSteam", 1).parameter().isPresent());
        assertFalse(call(Game.class, "launchSteam", -1).parameter().isPresent());
    }

    /** An annotation no parameter can be seen to carry fails when the editor is built, not by never matching. */
    @Test
    void anAnnotationReflectionCannotSeeFailsAtOnce() {
        assertThrows(IllegalArgumentException.class, () -> SlotEditor.onParameter(Invisible.class));
        assertThrows(IllegalArgumentException.class, () -> SlotEditor.onParameter(OnMethods.class));
    }

    /** No call site, no claim — the case an editor written against a slot never sees. */
    @Test
    void itIsAbsentWhereThereIsNoCall() {
        assertFalse(STEAM_APP_ID.matches(row()));
    }

    /** A type-matched editor is the other way round: it serves every place the host edits a value. */
    @Test
    void aTypeMatchedEditorServesTheRowAndTheSlotAlike() {
        SlotEditor text = SlotEditor.forType(String.class).draw(() -> ctx -> null);

        assertTrue(text.matches(row()));
        assertTrue(text.matches(call(Game.class, "launchSteam", 0)));
        assertFalse(SlotEditor.forType(Integer.class).draw(() -> ctx -> null).matches(row()));
    }

    /** {@code when} is the predicate as given; the preview is drawn only when one was declared. */
    @Test
    void aPredicateEditorDrawsItsPreviewOnlyWhenDeclared() {
        SlotEditor plain = SlotEditor.when(ctx -> ctx.slot().isEmpty()).draw(() -> ctx -> null);
        SlotEditor previewed = SlotEditor.when(ctx -> true).draw(() -> ctx -> null, () -> ctx -> null);

        assertTrue(plain.matches(row()));
        assertFalse(plain.matches(call(Game.class, "launchWindow", 0)));
        assertEquals(null, plain.preview(row()));
        assertEquals(null, previewed.preview(row()));
        assertThrows(NullPointerException.class, () -> SlotEditor.when(ctx -> true).draw(() -> ctx -> null, null));
    }
}

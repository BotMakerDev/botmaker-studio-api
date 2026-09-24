package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which slot an editor claims — the half of the toolkit's deleted {@code CallSites} that was contract
 * vocabulary all along.
 *
 * <p>The property worth asserting is the one a plugin author gets wrong: an editor chosen by the call is
 * <b>absent</b> from the Parameters window and from a {@code @Managed} value, because neither has a call
 * site by construction. Declining there is the honest answer, and it is why a value that must be editable
 * in both places needs a type-matched editor too.
 */
class SlotEditorTest {

    public static class Game {
        public static void launchSteam(String id) {}
        public static void launchSteamIfNotRunning(String id, Object source) {}
        public static void launchWindow(String title) {}
    }

    /** Another library's class with the same simple name and method: the case the names used to claim. */
    public static class OtherGame {
        public static void launchSteam(String id) {}
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
    private static ValueContext call(Class<?> owner, String method, int index) {
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

    private static final SlotEditor STEAM_APP_ID = SlotEditor.forCall(
            SlotEditor.calls(Game.class, "launchSteam", "launchSteamIfNotRunning"), 0, ctx -> null);

    @Test
    void itClaimsTheArgumentItNames() {
        assertTrue(STEAM_APP_ID.matches(call(Game.class, "launchSteam", 0)));
        assertTrue(STEAM_APP_ID.matches(call(Game.class, "launchSteamIfNotRunning", 0)));
    }

    /** The whole point: same type, different call, different editor. */
    @Test
    void itDeclinesAnotherArgumentAndAnotherMethod() {
        assertFalse(STEAM_APP_ID.matches(call(Game.class, "launchSteam", 1)));
        assertFalse(STEAM_APP_ID.matches(call(Game.class, "launchWindow", 0)));
    }

    /** The call is the resolved declaration, so a namesake elsewhere is not claimed. */
    @Test
    void aMethodOfTheSameNameOnAnotherClassIsNotClaimed() {
        assertFalse(STEAM_APP_ID.matches(call(OtherGame.class, "launchSteam", 0)));
    }

    /** A call the host could not resolve is no call: the editor is absent rather than guessing. */
    @Test
    void anUnresolvedCallIsNotClaimed() {
        assertFalse(STEAM_APP_ID.matches(call(null, null, 0)));
    }

    /** A name the class does not declare fails when the predicate is built, not by never matching. */
    @Test
    void aMisspelledMethodFailsAtOnce() {
        assertThrows(IllegalArgumentException.class, () -> SlotEditor.calls(Game.class, "launchStem"));
    }

    /** No call site, no claim — the case an editor written against a slot never sees. */
    @Test
    void itIsAbsentWhereThereIsNoCall() {
        assertFalse(STEAM_APP_ID.matches(row()));
    }

    /** A type-matched editor is the other way round: it serves every place the host edits a value. */
    @Test
    void aTypeMatchedEditorServesTheRowAndTheSlotAlike() {
        SlotEditor text = SlotEditor.forType(String.class, ctx -> null);

        assertTrue(text.matches(row()));
        assertTrue(text.matches(call(Game.class, "launchSteam", 0)));
        assertFalse(SlotEditor.forType(Integer.class, ctx -> null).matches(row()));
    }
}

package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    static class Game {}

    private static final TypeRef STRING = new TypeRef() {
        @Override public String simpleName() { return "String"; }
        @Override public String qualifiedName() { return "java.lang.String"; }
    };

    /** A Parameters row, or a {@code @Managed} value: a value with no call site anywhere. */
    private static ValueContext row() {
        return new ValueContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"570\""; }
            @Override public void set(String javaExpression, Class<?>... imports) {}
            @Override public StudioServices services() { return null; }
        };
    }

    /** Argument {@code index} of {@code enclosingClass.method(…)}. */
    private static ValueContext call(String enclosingClass, String method, int index) {
        return new SlotContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"570\""; }
            @Override public void set(String javaExpression, Class<?>... imports) {}
            @Override public StudioServices services() { return null; }
            @Override public Optional<String> enclosingClassName() { return Optional.of(enclosingClass); }
            @Override public Optional<String> enclosingMethodName() { return Optional.of(method); }
            @Override public int argIndex() { return index; }
            @Override public Optional<String> enclosingCall() { return Optional.empty(); }
        };
    }

    private static final SlotEditor STEAM_APP_ID =
            SlotEditor.forCall(Game.class, 0, ctx -> null, "launchSteam", "launchSteamIfNotRunning");

    @Test
    void itClaimsTheArgumentItNames() {
        assertTrue(STEAM_APP_ID.matches(call("Game", "launchSteam", 0)));
        assertTrue(STEAM_APP_ID.matches(call("Game", "launchSteamIfNotRunning", 0)));
    }

    /** The whole point: same type, different call, different editor. */
    @Test
    void itDeclinesAnotherArgumentAndAnotherMethod() {
        assertFalse(STEAM_APP_ID.matches(call("Game", "launchSteam", 1)));
        assertFalse(STEAM_APP_ID.matches(call("Game", "launchWindow", 0)));
    }

    /**
     * The class matches by simple name or by a qualified name ending in it: the host may hand back either
     * spelling, and an older version of the plugin's library may have had the class in another package.
     */
    @Test
    void theClassMatchesBySimpleNameOrByATailOfAQualifiedOne() {
        assertTrue(STEAM_APP_ID.matches(call("com.example.older.Game", "launchSteam", 0)));
        assertFalse(STEAM_APP_ID.matches(call("MyGame", "launchSteam", 0)));
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
        assertTrue(text.matches(call("Game", "launchSteam", 0)));
        assertFalse(SlotEditor.forType(Integer.class, ctx -> null).matches(row()));
    }
}

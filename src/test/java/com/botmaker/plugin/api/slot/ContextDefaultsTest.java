package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a context answers when the host implements nothing beyond what it must.
 *
 * <p>Every {@code default} here is a question a plugin asks without checking first, so the answer has to be
 * the harmless one rather than an exception — <em>no call site</em>, <em>no sibling run</em>, <em>anything
 * goes</em>. Each is one line, and each is wrong silently if it is wrong at all: an editor that never sees a
 * value the host did supply, or one that throws inside somebody else's window.
 */
class ContextDefaultsTest {

    private static final TypeRef STRING = new TypeRef() {
        @Override public String simpleName() { return "String"; }
        @Override public String qualifiedName() { return "java.lang.String"; }
    };

    /** A Parameters row, or a {@code @Managed} value: a value with no call site anywhere. */
    private static ValueContext row() {
        return new ValueContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) {
                return type == String.class ? Optional.of(type.cast("x")) : Optional.empty();
            }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"x\""; }
            @Override public void setSource(String javaExpression, Class<?>... imports) {}
            @Override public StudioServices services() { return null; }
        };
    }

    /** The least a host can implement and still be a slot. */
    private static SlotContext slot() {
        return new SlotContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) { return Optional.empty(); }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"570\""; }
            @Override public void setSource(String javaExpression, Class<?>... imports) {}
            @Override public StudioServices services() { return null; }
            @Override public Optional<String> enclosingClassName() { return Optional.of("Game"); }
            @Override public Optional<String> enclosingMethodName() { return Optional.of("launchSteam"); }
            @Override public int argIndex() { return 0; }
            @Override public Optional<String> enclosingCall() { return Optional.empty(); }
        };
    }

    @Test
    void aValueWithNoCallSiteHasNoSlot() {
        assertTrue(row().slot().isEmpty());
    }

    @Test
    void aSlotIsItsOwnCallSite() {
        SlotContext slot = slot();

        assertSame(slot, slot.slot().orElseThrow());
    }

    @Test
    void standingAloneIsTheOrdinaryState() {
        assertTrue(slot().siblingRun().isEmpty());
    }

    @Test
    void replacingAnEnclosingCallThereIsNoneOfDoesNothing() {
        slot().replaceEnclosingCall("Wait.between(a, b)", "com.botmaker.sdk.api.interaction.Wait");
    }

    @Test
    void anUnrestrictedRunAnswersEmptyRatherThanEveryElement() {
        SlotRun bare = new SlotRun() {
            @Override public List<String> elements() { return List.of("Pictures.ORE"); }
            @Override public void replace(List<String> javaExpressions, String... importsNeeded) {}
        };

        assertTrue(bare.allowedSources().isEmpty(), "empty is anything goes, not nothing allowed");
    }

    @Test
    void aValueCarriesItsTypedValueAndTheSourceItWasWrittenAs() {
        ValueContext value = row();

        assertEquals("x", value.value(String.class).orElseThrow());
        assertEquals("\"x\"", value.source(), "the escape hatch, for an expression nothing can decode");
        assertTrue(value.value(Integer.class).isEmpty(), "asking for a type nothing owns is empty, not a throw");
    }
}

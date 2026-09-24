package com.botmaker.plugin.api.slot;

import com.botmaker.plugin.api.StudioServices;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
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

    private static final TypeRef STRING = TypeRef.of(String.class);

    /** A Parameters row, or a {@code @Managed} value: a value with no call site anywhere. */
    private static ValueContext row() {
        return new ValueContext() {
            @Override public TypeRef type() { return STRING; }
            @Override public <T> Optional<T> value(Class<T> type) {
                return type == String.class ? Optional.of(type.cast("x")) : Optional.empty();
            }
            @Override public void set(Object value) {}
            @Override public String source() { return "\"x\""; }
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
            @Override public StudioServices services() { return null; }
            @Override public Optional<Executable> enclosingExecutable() { return Optional.empty(); }
            @Override public int argIndex() { return 0; }
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
    void anUnrestrictedRunAnswersEmptyRatherThanEveryElement() {
        SlotRun bare = new SlotRun() {
            @Override public List<Element> elements() { return List.of(new Element(null, "Pictures.ORE")); }
            @Override public void replace(List<?> elements) {}
        };

        assertTrue(bare.allowed().isEmpty(), "empty is anything goes, not nothing allowed");
    }

    @Test
    void anElementAnswersItsValueOnlyAsItsOwnType() {
        SlotRun.Element read = new SlotRun.Element("x", "\"x\"");
        SlotRun.Element unread = new SlotRun.Element(null, null);

        assertEquals("x", read.value(String.class).orElseThrow());
        assertTrue(read.value(Integer.class).isEmpty());
        assertTrue(unread.value(String.class).isEmpty(), "an element the host cannot read has no value");
        assertEquals("", unread.source(), "never null");
    }

    @Test
    void aValueCarriesItsTypedValueAndTheSourceItWasWrittenAs() {
        ValueContext value = row();

        assertEquals("x", value.value(String.class).orElseThrow());
        assertEquals("\"x\"", value.source(), "the escape hatch, for an expression nothing can decode");
        assertTrue(value.value(Integer.class).isEmpty(), "asking for a type nothing owns is empty, not a throw");
    }
}

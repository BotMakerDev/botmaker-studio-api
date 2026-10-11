package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.managed.ManagedMarker;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A plugin's declared conversion of its managed values (2026-10-11): what the steps refuse, and what
 * {@link Conversion#convert} lets through to the host.
 */
class ConversionTest {

    @ManagedMarker
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TestValue {

        Id value();

        enum Id { OLD_GREETING, OLD_NAMES, GREETING, NAMES }
    }

    static final ManagedValue<String> OLD_GREETING = ManagedValue.method(TestValue.Id.OLD_GREETING).in("Values")
            .holds(String.class, "").because("Old.").retired();
    static final ManagedValue<String> OLD_NAMES = ManagedValue.openSet(TestValue.Id.OLD_NAMES).of(String.class)
            .in("OldNames").because("Old.").retired();
    static final ManagedValue<String> GREETING = ManagedValue.method(TestValue.Id.GREETING).in("Values")
            .holds(String.class, "").because("New.");
    static final ManagedValue<String> NAMES = ManagedValue.openSet(TestValue.Id.NAMES).of(String.class)
            .in("Names").because("New.");

    /** Upper-cases the greeting and prefixes each name: one conversion, two values each way. */
    public static Converted shout(Converting old) {
        Map<String, String> names = new LinkedHashMap<>();
        old.members(OLD_NAMES).forEach((name, value) -> names.put("NEW_" + name, value));
        String greeting = old.value(OLD_GREETING);
        return Converted.empty().value(GREETING, greeting == null ? "" : greeting.toUpperCase()).members(NAMES, names);
    }

    /** Writes a value its {@code to(…)} does not name. */
    public static Converted stray(Converting old) {
        return Converted.empty().value(OLD_GREETING, "x");
    }

    public static Converted fails(Converting old) {
        throw new IllegalStateException("cannot");
    }

    private static Conversion conversion(com.botmaker.plugin.api.value.Ref.Of1<Converting, Converted> by) {
        return Conversion.from(OLD_GREETING, OLD_NAMES).to(GREETING, NAMES).by(by).because("Renamed.");
    }

    @Test
    void retired_is_read_only_and_says_so() {
        assertTrue(OLD_GREETING.isRetired());
        assertFalse(GREETING.isRetired());
        assertEquals(GREETING.id().replace("GREETING", "OLD_GREETING"), OLD_GREETING.id());
    }

    @Test
    void many_to_many_converts_what_the_bot_holds() {
        Converting old = Converting.of(Map.of(OLD_GREETING.id(), "hello",
                OLD_NAMES.id(), Map.of("ANNA", "Anna")));
        Converted written = conversion(ConversionTest::shout).convert(old);
        assertEquals("HELLO", written.written().get(GREETING.id()));
        assertEquals(Map.of("NEW_ANNA", "Anna"), written.written().get(NAMES.id()));
    }

    @Test
    void a_value_the_bot_lacks_reads_as_nothing() {
        Converting old = Converting.of(Map.of());
        assertFalse(old.holds(OLD_GREETING));
        assertNull(old.value(OLD_GREETING));
        assertTrue(old.members(OLD_NAMES).isEmpty());
        Converted written = conversion(ConversionTest::shout).convert(old);
        assertEquals("", written.written().get(GREETING.id()));
    }

    @Test
    void the_steps_refuse_a_wrong_direction() {
        assertThrows(IllegalArgumentException.class, () -> Conversion.from(GREETING));
        assertThrows(IllegalArgumentException.class, () -> Conversion.from(OLD_GREETING).to(OLD_NAMES));
        assertThrows(IllegalArgumentException.class, () -> Conversion.from());
        assertThrows(IllegalArgumentException.class, () -> Conversion.from(OLD_GREETING, OLD_GREETING));
        assertThrows(IllegalArgumentException.class,
                () -> Conversion.from(OLD_GREETING).to(GREETING).by(old -> Converted.empty()));
    }

    @Test
    void a_conversion_that_fails_or_strays_is_refused() {
        Converting old = Converting.of(Map.of());
        assertThrows(IllegalStateException.class, () -> conversion(ConversionTest::fails).convert(old));
        assertThrows(IllegalStateException.class, () -> conversion(ConversionTest::stray).convert(old));
    }

    /** A retired shape and the current one are never one id, or the host could not tell the two holders apart. */
    @Test
    void a_value_read_and_written_never_share_an_id() {
        ManagedValue<String> same = ManagedValue.method(TestValue.Id.GREETING).in("Old")
                .holds(String.class, "").because("Old.").retired();
        assertThrows(IllegalArgumentException.class, () -> Conversion.from(same).to(GREETING));
    }

    /** Reading a set as a value is a mistake, not a bot that holds nothing. */
    @Test
    void converting_refuses_the_wrong_shape_and_hands_out_a_copy() {
        Map<String, String> held = new LinkedHashMap<>(Map.of("ANNA", "Anna"));
        Converting old = Converting.of(Map.of(OLD_NAMES.id(), held));
        held.put("BOB", "Bob");
        assertEquals(Map.of("ANNA", "Anna"), old.members(OLD_NAMES));
        assertThrows(IllegalArgumentException.class, () -> old.value(OLD_NAMES));
        assertThrows(IllegalArgumentException.class, () -> old.members(OLD_GREETING));
    }

    @Test
    void converted_refuses_a_value_of_the_wrong_shape() {
        assertThrows(IllegalArgumentException.class, () -> Converted.empty().value(NAMES, "x"));
        assertThrows(IllegalArgumentException.class, () -> Converted.empty().members(GREETING, Map.of("A", "a")));
        assertEquals(List.of(OLD_GREETING, OLD_NAMES), conversion(ConversionTest::shout).sources());
    }
}
